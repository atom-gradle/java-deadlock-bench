# -*- coding: utf-8 -*-

import argparse, glob, hashlib, json, os, re, warnings, joblib, time, sys, random
from collections import Counter

import numpy as np
import pandas as pd
import torch
import torch.nn.functional as F
from scipy.stats import mannwhitneyu, t, ttest_rel, wilcoxon
from xgboost import XGBClassifier
from sklearn.svm import SVC
from sklearn.linear_model import LogisticRegression
from sklearn.preprocessing import StandardScaler
from sklearn.pipeline import Pipeline
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import (
    roc_auc_score, f1_score, precision_score, recall_score,
    accuracy_score, )
from sklearn.model_selection import GroupKFold, GridSearchCV
from torch_geometric.data import HeteroData, Batch
from torch_geometric.loader import DataLoader
from torch_geometric.nn import SAGEConv, HeteroConv, Linear, global_mean_pool, global_max_pool

warnings.filterwarnings("ignore")

RANDOM_SEED = 42
np.random.seed(RANDOM_SEED)
torch.manual_seed(RANDOM_SEED)
random.seed(RANDOM_SEED)

LOCK_FEAT_DIM = 5
ACQ_FEAT_DIM = 6
LOCK_EDGE_FEAT_DIM = 3
CALL_EDGE_FEAT_DIM = 3
GLOBAL_FEAT_DIM = 18

GRAPH_FEATS = [
    "n_lock", "n_acq", "n_lock_edge", "n_call_edge",
    "n_cross", "n_cross_atl", "n_cross_lta",
    "static_ratio", "nonnull_ratio",
    "avg_lock_dist", "alias_ratio", "same_obj_ratio",
    "sync_ratio", "acq_static_ratio", "avg_nesting", "avg_pts",
    "avg_call_dist", "nested_ratio",
]

CYCLE_FEATS = [
    "thread_count", "max_thread_occurrence", "edge_count",
    "all_edges_same_thread", "all_edges_reachable", "has_non_adjacent_edge",
]

FEAT_STATS = {
    "lock": {"mean": None, "std": None},
    "acq": {"mean": None, "std": None},
    "global": {"mean": None, "std": None},
}

FOLD_METRICS = {}
SEED_METRICS = {}


def _normalize(x: torch.Tensor, key: str):
    st = FEAT_STATS.get(key)
    if st is None or st["mean"] is None:
        return x
    mean = torch.as_tensor(st["mean"], dtype=x.dtype, device=x.device)
    std = torch.as_tensor(st["std"], dtype=x.dtype, device=x.device)
    return (x - mean) / std.clamp_min(1e-6)


def reset_feat_stats():
    for k in FEAT_STATS:
        FEAT_STATS[k]["mean"] = None
        FEAT_STATS[k]["std"] = None


def compute_feat_stats(data_list):
    for key, dim in [("lock", LOCK_FEAT_DIM), ("acq", ACQ_FEAT_DIM)]:
        vecs = []
        for d in data_list:
            if d[key].x.numel() > 0:
                vecs.append(d[key].x)
        if vecs:
            allx = torch.cat(vecs, dim=0)
            FEAT_STATS[key]["mean"] = allx.mean(dim=0).tolist()
            FEAT_STATS[key]["std"] = allx.std(dim=0).clamp_min(1e-6).tolist()

    vecs = [d.global_feat for d in data_list]
    if vecs:
        allx = torch.cat(vecs, dim=0)
        FEAT_STATS["global"]["mean"] = allx.mean(dim=0).tolist()
        FEAT_STATS["global"]["std"] = allx.std(dim=0).clamp_min(1e-6).tolist()


class FocalLoss(torch.nn.Module):
    def __init__(self, gamma=2.0, alpha=None, reduction="mean"):
        super().__init__()
        self.gamma = gamma
        self.alpha = alpha
        self.reduction = reduction

    def forward(self, logits, target):
        logp = F.log_softmax(logits, dim=-1)
        p = logp.exp()
        target = target.view(-1, 1)
        logpt = logp.gather(1, target).squeeze(1)
        pt = p.gather(1, target).squeeze(1)
        loss = -((1 - pt) ** self.gamma) * logpt
        if self.alpha is not None:
            at = self.alpha.to(logits.device).gather(0, target.squeeze(1))
            loss = at * loss
        return loss.mean() if self.reduction == "mean" else loss


def parse_file(filepath):
    cycles = []
    cur = None

    def new_cycle(cid):
        return {
            "file": os.path.basename(filepath), "cycle_id": cid,
            "lock_nodes": [], "lock_edges": [], "acq_nodes": [], "call_edges": [],
            "cross": [], "features": {}, "labels": {},
        }

    for raw in open(filepath, encoding="utf-8"):
        line = raw.strip()
        if not line or line.startswith("#"): continue
        if line.startswith("CYCLE "):
            if cur is not None: cycles.append(cur)
            cur = new_cycle(int(line.split()[1]))
            continue
        if cur is None: continue

        if line.startswith("LOCK_NODE"):
            _, rest = line.split(None, 1)
            key_part, obj_type, is_static, obj_nonnull = rest.rsplit(",", 3)
            nid, obj_key = key_part.split(",", 1)
            cur["lock_nodes"].append({"id": int(nid), "obj_key": obj_key, "obj_type": obj_type,
                                      "is_static": is_static.strip().lower() == "true",
                                      "obj_nonnull": int(obj_nonnull.strip())})
        elif line.startswith("LOCK_EDGE"):
            _, rest = line.split(None, 1)
            p = rest.split(",")
            cur["lock_edges"].append(
                {"from": int(p[0]), "to": int(p[1]), "from_method": p[2], "from_line": int(p[3]), "to_method": p[4],
                 "to_line": int(p[5]), "dist": int(p[6]), "may_alias": p[7].strip().lower() == "true",
                 "same_lock_obj": p[8].strip().lower() == "true"})
        elif line.startswith("ACQ_NODE"):
            _, rest = line.split(None, 1)
            p = rest.split(",", 7)
            cur["acq_nodes"].append({"id": int(p[0]), "method": p[1], "line": int(p[2]), "var": p[3],
                                     "is_static": p[4].strip().lower() == "true",
                                     "is_sync": p[5].strip().lower() == "true", "nesting": int(p[6]),
                                     "pts_size": int(p[7])})
        elif line.startswith("CALL_EDGE"):
            _, rest = line.split(None, 1)
            p = rest.split(",")
            cur["call_edges"].append(
                {"src": int(p[0]), "dst": int(p[1]), "dist": int(p[2]), "same_method": p[3].strip().lower() == "true",
                 "nested": p[4].strip().lower() == "true"})
        elif line.startswith("ACQ_TO_LOCK"):
            _, rest = line.split(None, 1)
            p = rest.split(",")
            cur["cross"].append(("ACQ_TO_LOCK", int(p[0]), int(p[1])))
        elif line.startswith("LOCK_TO_ACQ"):
            _, rest = line.split(None, 1)
            p = rest.split(",")
            cur["cross"].append(("LOCK_TO_ACQ", int(p[0]), int(p[1])))
        elif line.startswith("CYCLE_FEATURES"):
            cur["features"] = dict(re.findall(r"(\w+)=([\w\.\-]+)", line))
        elif line.startswith("LABEL_CYCLE"):
            cur["labels"]["cycle"] = int(line.split()[1])
        elif line.startswith("LABEL_REAL"):
            cur["labels"]["real"] = int(line.split()[1])
    if cur is not None: cycles.append(cur)
    return cycles


def load_all(data_dir, pattern="graph-*.txt"):
    files = sorted(glob.glob(os.path.join(data_dir, pattern)))
    if not files:
        files = sorted(glob.glob(os.path.join(data_dir, "graph-*.txt")))
        files = [f for f in files if "-copy" not in f]
    cycles = []
    for f in files:
        try:
            cycles.extend(parse_file(f))
        except Exception as e:
            pass
    return cycles


def build_feature_table(cycles):
    rows = []
    for c in cycles:
        f = c["features"]
        n_lock = len(c["lock_nodes"]);
        n_acq = len(c["acq_nodes"])
        n_le = len(c["lock_edges"]);
        n_ce = len(c["call_edges"])
        n_cross = len(c["cross"])
        n_atl = sum(1 for x in c["cross"] if x[0] == "ACQ_TO_LOCK")
        n_lta = sum(1 for x in c["cross"] if x[0] == "LOCK_TO_ACQ")
        static_ratio = (sum(1 for n in c["lock_nodes"] if n["is_static"]) / n_lock if n_lock else 0.0)
        nonnull_ratio = (sum(1 for n in c["lock_nodes"] if n["obj_nonnull"]) / n_lock if n_lock else 0.0)
        dists = [e["dist"] for e in c["lock_edges"] if e["dist"] >= 0]
        avg_lock_dist = float(np.mean(dists)) if dists else -1.0
        alias_ratio = (sum(1 for e in c["lock_edges"] if e["may_alias"]) / n_le if n_le else 0.0)
        same_obj_ratio = (sum(1 for e in c["lock_edges"] if e["same_lock_obj"]) / n_le if n_le else 0.0)
        sync_ratio = (sum(1 for a in c["acq_nodes"] if a["is_sync"]) / n_acq if n_acq else 0.0)
        acq_static_ratio = (sum(1 for a in c["acq_nodes"] if a["is_static"]) / n_acq if n_acq else 0.0)
        avg_nesting = float(np.mean([a["nesting"] for a in c["acq_nodes"]])) if n_acq else 0.0
        avg_pts = float(np.mean([a["pts_size"] for a in c["acq_nodes"]])) if n_acq else 0.0
        call_dists = [e["dist"] for e in c["call_edges"]]
        avg_call_dist = float(np.mean(call_dists)) if call_dists else -1.0
        nested_ratio = (sum(1 for e in c["call_edges"] if e["nested"]) / n_ce if n_ce else 0.0)

        struct_key = (n_lock, n_acq, n_le, n_ce, n_cross, n_atl, n_lta, round(static_ratio, 2), round(nonnull_ratio, 2),
                      round(avg_lock_dist, 2), round(alias_ratio, 2), round(same_obj_ratio, 2), round(sync_ratio, 2),
                      round(acq_static_ratio, 2), round(avg_nesting, 2), round(avg_pts, 2), round(avg_call_dist, 2),
                      round(nested_ratio, 2))
        struct_hash = hashlib.md5(str(struct_key).encode()).hexdigest()

        rows.append({
            "file": c["file"], "cycle_id": c["cycle_id"], "label_cycle": c["labels"].get("cycle", -1),
            "label_real": c["labels"].get("real", -1), "struct_hash": struct_hash,
            "n_lock": n_lock, "n_acq": n_acq, "n_lock_edge": n_le, "n_call_edge": n_ce, "n_cross": n_cross,
            "n_cross_atl": n_atl, "n_cross_lta": n_lta,
            "static_ratio": static_ratio, "nonnull_ratio": nonnull_ratio, "avg_lock_dist": avg_lock_dist,
            "alias_ratio": alias_ratio, "same_obj_ratio": same_obj_ratio,
            "sync_ratio": sync_ratio, "acq_static_ratio": acq_static_ratio, "avg_nesting": avg_nesting,
            "avg_pts": avg_pts, "avg_call_dist": avg_call_dist, "nested_ratio": nested_ratio,
            "thread_count": int(f.get("thread_count", 0)),
            "max_thread_occurrence": int(f.get("max_thread_occurrence", 0)), "edge_count": int(f.get("edge_count", 0)),
            "all_edges_same_thread": int(f.get("all_edges_same_thread", "false") == "true"),
            "all_edges_reachable": int(f.get("all_edges_reachable", "false") == "true"),
            "has_non_adjacent_edge": int(f.get("has_non_adjacent_edge", "false") == "true"),
        })
    return pd.DataFrame(rows)


def apply_label_mode(df, label_mode):
    df = df[df["label_real"].isin([0, 1, 2])].copy()
    if label_mode == "binary_strong":
        df["y"] = (df["label_real"] == 2).astype(int)
    elif label_mode == "binary_loose":
        df["y"] = (df["label_real"] >= 1).astype(int)
    elif label_mode == "three_way":
        df["y"] = df["label_real"].astype(int)
    else:
        raise ValueError(f"unknown label_mode: {label_mode}")
    return df


def make_folds(df, n_splits=5):
    groups = df["struct_hash"].values
    y = df["y"].values
    n_groups = len(np.unique(groups))
    n_splits = min(n_splits, n_groups)
    if n_splits < 2:
        raise ValueError(f"Too few unique structure ({n_groups})")
    gkf = GroupKFold(n_splits=n_splits)
    return list(gkf.split(np.zeros(len(df)), y, groups))


def mean_ci95(values):
    arr = np.asarray(values, dtype=float)
    arr = arr[~np.isnan(arr)]
    if len(arr) < 2: return float(arr.mean()) if len(arr) else float("nan"), 0.0
    m = float(arr.mean())
    hw = float(t.interval(0.95, len(arr) - 1, loc=m, scale=arr.std(ddof=1) / np.sqrt(len(arr)))[1] - m)
    return m, hw


def paired_ttest(a, b):
    a, b = np.asarray(a, dtype=float), np.asarray(b, dtype=float)
    mask = ~(np.isnan(a) | np.isnan(b))
    a, b = a[mask], b[mask]
    if len(a) < 2: return float("nan"), float("nan"), float("nan")
    t_stat, p_val = ttest_rel(a, b)
    return float(t_stat), float(p_val), float(np.mean(a - b))


def corrected_resampled_ttest(a, b, n_train, n_test):
    a, b = np.asarray(a, dtype=float), np.asarray(b, dtype=float)
    d = a - b
    n = len(d)
    if n < 2: return float("nan"), float("nan")
    mean_d = d.mean()
    var_d = d.var(ddof=1)
    correction = 1.0 / n + n_test / n_train
    t_stat = mean_d / np.sqrt(var_d * correction)
    from scipy.stats import t as t_dist
    return float(t_stat), float(2 * (1 - t_dist.cdf(abs(t_stat), df=n - 1)))


def eval_sklearn_on_folds(df, feat_cols, folds, tag="lr", out_dir=None):
    X = df[feat_cols].fillna(0).values.astype(np.float32)
    y = df["y"].values.astype(int)
    is_binary = df["y"].nunique() == 2
    oof_prob = np.zeros(len(y))
    oof_pred = np.zeros(len(y), dtype=int)
    fold_raw = {"auc": [], "f1": [], "p": [], "r": [], "acc": [], "f1_macro": []}

    for k, (tr, te) in enumerate(folds):
        if len(np.unique(y[te])) < 2:
            for key in fold_raw: fold_raw[key].append(float("nan"))
            continue
        if tag == "rule_lr":
            model = Pipeline([("scaler", StandardScaler()), ("clf",
                                                             LogisticRegression(max_iter=2000, class_weight="balanced",
                                                                                random_state=RANDOM_SEED))])
        elif tag == "lr":
            model = Pipeline([("scaler", StandardScaler()), ("clf",
                                                             LogisticRegression(max_iter=2000, class_weight="balanced",
                                                                                random_state=RANDOM_SEED))])
        elif tag == "xgb":
            model = XGBClassifier(n_estimators=400, max_depth=4, learning_rate=0.05, subsample=0.9,
                                  colsample_bytree=0.9, reg_lambda=1.0, min_child_weight=1,
                                  objective="binary:logistic" if is_binary else "multi:softprob",
                                  num_class=None if is_binary else 3,
                                  eval_metric="logloss" if is_binary else "mlogloss", tree_method="hist",
                                  random_state=RANDOM_SEED, n_jobs=-1)
        elif tag == "svm":
            model = Pipeline([("scaler", StandardScaler()), ("clf",
                                                             SVC(kernel="rbf", C=1.0, gamma="scale", probability=True,
                                                                 class_weight="balanced", random_state=RANDOM_SEED))])

        model.fit(X[tr], y[tr])
        if is_binary:
            prob = model.predict_proba(X[te])[:, 1]
            pred = (prob >= 0.5).astype(int)
            oof_prob[te] = prob;
            oof_pred[te] = pred
            try:
                fold_raw["auc"].append(roc_auc_score(y[te], prob))
            except:
                fold_raw["auc"].append(float("nan"))
            fold_raw["f1"].append(f1_score(y[te], pred, zero_division=0))
            fold_raw["p"].append(precision_score(y[te], pred, zero_division=0))
            fold_raw["r"].append(recall_score(y[te], pred, zero_division=0))
            fold_raw["acc"].append(accuracy_score(y[te], pred))
        else:
            prob = model.predict_proba(X[te]);
            pred = model.predict(X[te])
            oof_pred[te] = pred
            fold_raw["f1_macro"].append(f1_score(y[te], pred, average="macro", zero_division=0))
            fold_raw["acc"].append(accuracy_score(y[te], pred))

    mean_metrics = {}
    if is_binary:
        for key in ["auc", "f1", "p", "r", "acc"]:
            m, hw = mean_ci95(fold_raw[key]);
            mean_metrics[key] = m;
            mean_metrics[f"{key}_ci95"] = hw
    else:
        for key in ["f1_macro", "acc"]:
            m, hw = mean_ci95(fold_raw[key]);
            mean_metrics[key] = m;
            mean_metrics[f"{key}_ci95"] = hw

    if out_dir:
        oof_df = df[["file", "cycle_id", "struct_hash", "label_real", "y"]].copy()
        oof_df["oof_prob"] = oof_prob if is_binary else -1
        oof_df["oof_pred"] = oof_pred
        oof_df.to_csv(os.path.join(out_dir, f"{tag}_oof.csv"), index=False)

    fold_metrics_list = []
    for i in range(len(fold_raw["acc"])):
        d = {"acc": fold_raw["acc"][i]}
        if is_binary:
            d["auc"] = fold_raw["auc"][i];
            d["f1"] = fold_raw["f1"][i]
        else:
            d["f1_macro"] = fold_raw["f1_macro"][i]
        fold_metrics_list.append(d)
    FOLD_METRICS[tag] = fold_metrics_list
    return mean_metrics, oof_prob, oof_pred


def cycle_to_hetero(c, drop_lock_edge=False, drop_call_edge=False, drop_cross=False,
                    drop_line=False, abl_node="full", abl_edge="full", abl_global="full"):

    data = HeteroData()
    n_lock = len(c["lock_nodes"])
    n_acq = len(c["acq_nodes"])

    f = c["features"]
    n_le, n_ce, n_cross = len(c["lock_edges"]), len(c["call_edges"]), len(c["cross"])
    static_ratio = (sum(1 for n in c["lock_nodes"] if n["is_static"]) / n_lock if n_lock else 0.0)
    nonnull_ratio = (sum(1 for n in c["lock_nodes"] if n["obj_nonnull"]) / n_lock if n_lock else 0.0)
    dists = [e["dist"] for e in c["lock_edges"] if e["dist"] >= 0]
    avg_lock_dist = float(np.mean(dists)) if dists else -1.0
    alias_ratio = (sum(1 for e in c["lock_edges"] if e["may_alias"]) / n_le if n_le else 0.0)
    same_obj_ratio = (sum(1 for e in c["lock_edges"] if e["same_lock_obj"]) / n_le if n_le else 0.0)
    sync_ratio = (sum(1 for a in c["acq_nodes"] if a["is_sync"]) / n_acq if n_acq else 0.0)
    acq_static_ratio = (sum(1 for a in c["acq_nodes"] if a["is_static"]) / n_acq if n_acq else 0.0)
    avg_nesting = float(np.mean([a["nesting"] for a in c["acq_nodes"]])) if n_acq else 0.0
    avg_pts = float(np.mean([a["pts_size"] for a in c["acq_nodes"]])) if n_acq else 0.0
    call_dists = [e["dist"] for e in c["call_edges"]]
    avg_call_dist = float(np.mean(call_dists)) if call_dists else -1.0
    nested_ratio = (sum(1 for e in c["call_edges"] if e["nested"]) / n_ce if n_ce else 0.0)

    global_feat = [
        n_lock, n_acq, n_le, n_ce, n_cross,
        sum(1 for x in c["cross"] if x[0] == "ACQ_TO_LOCK"),
        sum(1 for x in c["cross"] if x[0] == "LOCK_TO_ACQ"),
        static_ratio, nonnull_ratio, avg_lock_dist, alias_ratio, same_obj_ratio,
        sync_ratio, acq_static_ratio, avg_nesting, avg_pts, avg_call_dist, nested_ratio,
    ]
    if abl_global == "zero":
        global_feat = [0.0] * GLOBAL_FEAT_DIM
    data.global_feat = torch.tensor([global_feat], dtype=torch.float)

    lock_in, lock_out = Counter(), Counter()
    for e in c["lock_edges"]:
        lock_out[e["from"]] += 1
        lock_in[e["to"]] += 1
    for (t, a, b) in c["cross"]:
        if t == "ACQ_TO_LOCK":
            lock_in[b] += 1
        else:
            lock_out[a] += 1

    if n_lock > 0:
        lf = []
        for n in c["lock_nodes"]:
            nid = n["id"]
            feat = [
                float(n["is_static"]),
                float(n["obj_nonnull"]),
                float(lock_in.get(nid, 0)),
                float(lock_out.get(nid, 0)),
                float(lock_in.get(nid, 0) + lock_out.get(nid, 0)),
            ]
            if abl_node == "no_static":
                feat[0] = 0.0
                feat[1] = 0.0
            elif abl_node == "no_degree":
                feat[2] = 0.0
                feat[3] = 0.0
                feat[4] = 0.0
            elif abl_node == "zero":
                feat = [0.0] * LOCK_FEAT_DIM
            lf.append(feat)
        data["lock"].x = torch.tensor(lf, dtype=torch.float)
    else:
        data["lock"].x = torch.zeros((0, LOCK_FEAT_DIM), dtype=torch.float)

    acq_in, acq_out = Counter(), Counter()
    for e in c["call_edges"]:
        acq_out[e["src"]] += 1
        acq_in[e["dst"]] += 1
    for (t, a, b) in c["cross"]:
        if t == "ACQ_TO_LOCK":
            acq_out[a] += 1
        else:
            acq_in[b] += 1

    if n_acq > 0:
        af = []
        for a in c["acq_nodes"]:
            aid = a["id"]
            feat = [
                float(a["line"] if a["line"] >= 0 else 0),
                float(a["is_static"]),
                float(a["is_sync"]),
                float(a["nesting"]),
                float(a["pts_size"]),
                float(acq_in.get(aid, 0) + acq_out.get(aid, 0)),
            ]
            if abl_node == "no_static":
                feat[1] = 0.0
                feat[2] = 0.0
            elif abl_node == "no_degree":
                feat[5] = 0.0
            elif abl_node == "zero":
                feat = [0.0] * ACQ_FEAT_DIM
            if drop_line:
                feat[0] = 0.0
            af.append(feat)
        data["acq"].x = torch.tensor(af, dtype=torch.float)
    else:
        data["acq"].x = torch.zeros((0, ACQ_FEAT_DIM), dtype=torch.float)

    if abl_edge == "zero":
        eff_drop_lock_edge = True
        eff_drop_call_edge = True
        eff_drop_cross = True
    else:
        eff_drop_lock_edge = drop_lock_edge
        eff_drop_call_edge = drop_call_edge
        eff_drop_cross = drop_cross

    if c["lock_edges"] and not eff_drop_lock_edge:
        data["lock", "lock_edge", "lock"].edge_index = torch.tensor(
            [[e["from"] for e in c["lock_edges"]],
             [e["to"] for e in c["lock_edges"]]],
            dtype=torch.long,
        )
    else:
        data["lock", "lock_edge", "lock"].edge_index = torch.zeros((2, 0), dtype=torch.long)

    if c["call_edges"] and not eff_drop_call_edge:
        data["acq", "call_edge", "acq"].edge_index = torch.tensor(
            [[e["src"] for e in c["call_edges"]],
             [e["dst"] for e in c["call_edges"]]],
            dtype=torch.long,
        )
    else:
        data["acq", "call_edge", "acq"].edge_index = torch.zeros((2, 0), dtype=torch.long)

    atl = [(a, b) for (t, a, b) in c["cross"] if t == "ACQ_TO_LOCK"] if not eff_drop_cross else []
    lta = [(a, b) for (t, a, b) in c["cross"] if t == "LOCK_TO_ACQ"] if not eff_drop_cross else []

    data["acq", "acq_to_lock", "lock"].edge_index = (
        torch.tensor([[a for a, b in atl], [b for a, b in atl]], dtype=torch.long)
        if atl else torch.zeros((2, 0), dtype=torch.long)
    )
    data["lock", "lock_to_acq", "acq"].edge_index = (
        torch.tensor([[a for a, b in lta], [b for a, b in lta]], dtype=torch.long)
        if lta else torch.zeros((2, 0), dtype=torch.long)
    )

    data.y = torch.tensor([c["labels"].get("real", 0)], dtype=torch.long)
    return data

def quality_report(df, out_dir):
    print("\n\n")
    print("Data quality assessment")

    print("\n[label_real raw distribution]")
    print(df["label_real"].value_counts().sort_index())

    print("\n[y Distribution (after label_mode)]")
    print(df["y"].value_counts().sort_index())

    print(f"\n[Duplicate structures] unique struct_hash = {df['struct_hash'].nunique()} / total samples: {len(df)}")

    print("\n[Missing values]")
    miss = df.isnull().sum()
    miss = miss[miss > 0]
    print(miss if len(miss) else "  No missing values")

    if df["y"].nunique() == 2:
        print("\n[Single-feature discriminative power (Mann-Whitney U)]")
        print(f"  {'feature':28s} {'p-value':>12s} {'AUC':>8s}")
        pos = df[df["y"] == 1]; neg = df[df["y"] == 0]
        results = []
        for col in GRAPH_FEATS + CYCLE_FEATS:
            if col not in df.columns:
                continue
            a, b = pos[col].dropna(), neg[col].dropna()
            if len(a) < 2 or len(b) < 2:
                continue
            try:
                stat, p = mannwhitneyu(a, b, alternative="two-sided")
                auc = stat / (len(a) * len(b))
                auc = max(auc, 1 - auc)
                results.append((col, p, auc))
            except Exception:
                pass
        results.sort(key=lambda x: x[2], reverse=True)
        for col, p, auc in results:
            sig = "***" if p < 0.001 else "**" if p < 0.01 else "*" if p < 0.05 else ""
            print(f"  {col:28s} {p:12.4g} {auc:8.3f} {sig}")

        leaky = [(c, a) for c, _, a in results if c in CYCLE_FEATS and a > 0.9]
        print("\n[Leakage detection]")
        if leaky:
            for c, a in leaky:
                print(f"  Warning! {c}: AUC={a:.3f}")
        else:
            print("No obvious leakage found.")

    os.makedirs(out_dir, exist_ok=True)
    df.to_csv(os.path.join(out_dir, "features.csv"), index=False)
    print(f"\n[save] {out_dir}/features.csv")


def rule_baseline(df):
    pred = (
        (df["all_edges_same_thread"] == 1)
        & (df["all_edges_reachable"] == 1)
        & (df["thread_count"] >= 2)
        & (df["max_thread_occurrence"] <= 1)
    ).astype(int).values

    y = df["y"].values.astype(int)

    if len(np.unique(y)) < 2:
        print("  Only one class in the labels, skipping.")
        return None

    if df["y"].nunique() == 2:
        acc = (pred == y).mean()
        try:
            auc = roc_auc_score(y, pred)
        except Exception:
            auc = float("nan")
        f1 = f1_score(y, pred, zero_division=0)
        p = precision_score(y, pred, zero_division=0)
        r = recall_score(y, pred, zero_division=0)
        print(f"  Acc={acc:.4f}  AUC={auc:.4f}  F1={f1:.4f}  P={p:.4f}  R={r:.4f}")
        print(f"  Predicted positive class ratio={pred.mean():.4f}  True positive class ratio={y.mean():.4f}")
        print("  [NOTE] This is the analyzer's hard-coded isReal heuristic; it is reported as a reference, not as a competitive baseline.")
        return {"acc": float(acc), "auc": float(auc),
                "f1": float(f1), "precision": float(p), "recall": float(r)}
    else:
        pred3 = np.where(pred == 1, 2, 0)
        acc = (pred3 == y).mean()
        f1m = f1_score(y, pred3, average="macro", zero_division=0)
        print(f"  [Three-class classification] Acc={acc:.4f}  macro-F1={f1m:.4f}")
        print(f"  The rule can only output 0/2 and cannot distinguish label 1.")
        print("  [NOTE] This is the analyzer's hard-coded isReal heuristic; it is reported as a reference, not as a competitive baseline.")
        return {"acc": float(acc), "f1_macro": float(f1m)}


def eval_rf_on_folds(df, feat_cols, folds, tag="rf", do_grid=False, out_dir=None):
    X = df[feat_cols].fillna(0).values.astype(np.float32)
    y = df["y"].values.astype(int)
    is_binary = df["y"].nunique() == 2

    best_params = None
    if do_grid:
        print(f"\n  [GridSearch] ...")
        param_grid = {
            "n_estimators": [200, 300, 500],
            "max_depth": [3, 5, 7, None],
            "min_samples_leaf": [1, 2, 4],
            "class_weight": ["balanced", "balanced_subsample"],
        }
        tr0, _ = folds[0]
        rf = RandomForestClassifier(random_state=RANDOM_SEED, n_jobs=-1)
        scoring = "roc_auc" if is_binary else "f1_macro"
        gs = GridSearchCV(rf, param_grid, cv=3, scoring=scoring,
                          n_jobs=-1, verbose=0)
        gs.fit(X[tr0], y[tr0])
        best_params = gs.best_params_
        print(f"  [GridSearch] best={best_params}, score={gs.best_score_:.4f}")

    oof_prob = np.zeros(len(y))
    oof_pred = np.zeros(len(y), dtype=int)
    fold_metrics = []

    for k, (tr, te) in enumerate(folds):
        if len(np.unique(y[te])) < 2:
            print(f"  fold {k}: test set has only one class, recorded as NaN")
            if is_binary:
                fold_metrics.append({"auc": float("nan"), "f1": float("nan"),
                                     "p": float("nan"), "r": float("nan"),
                                     "acc": float("nan")})
            else:
                fold_metrics.append({"f1_macro": float("nan"),
                                     "acc": float("nan")})
            continue

        if best_params is not None:
            model = RandomForestClassifier(random_state=RANDOM_SEED, n_jobs=-1, **best_params)
        else:
            model = RandomForestClassifier(
                n_estimators=300, random_state=RANDOM_SEED, n_jobs=-1,
                class_weight="balanced")
        model.fit(X[tr], y[tr])

        if is_binary:
            prob = model.predict_proba(X[te])[:, 1]
            pred = (prob >= 0.5).astype(int)
            oof_prob[te] = prob
            oof_pred[te] = pred
            try:
                m = {
                    "auc": float(roc_auc_score(y[te], prob)),
                    "f1": float(f1_score(y[te], pred, zero_division=0)),
                    "p": float(precision_score(y[te], pred, zero_division=0)),
                    "r": float(recall_score(y[te], pred, zero_division=0)),
                    "acc": float(accuracy_score(y[te], pred)),
                }
            except Exception:
                m = None
        else:
            prob = model.predict_proba(X[te])
            pred = model.predict(X[te])
            oof_pred[te] = pred
            m = {
                "f1_macro": float(f1_score(y[te], pred, average="macro", zero_division=0)),
                "acc": float(accuracy_score(y[te], pred)),
            }
        if m:
            fold_metrics.append(m)
            print(f"  fold {k}: {m}")

    if is_binary:
        mean_metrics = {}
        for key, src in [("auc", "auc"), ("f1", "f1"),
                         ("precision", "p"), ("recall", "r"), ("acc", "acc")]:
            m, hw = mean_ci95([x[src] for x in fold_metrics])
            mean_metrics[key] = m
            mean_metrics[f"{key}_ci95"] = hw
        try:
            mean_metrics["oof_auc"] = float(roc_auc_score(y, oof_prob))
        except Exception:
            mean_metrics["oof_auc"] = float("nan")
    else:
        mean_metrics = {}
        for key in ["f1_macro", "acc"]:
            m, hw = mean_ci95([x[key] for x in fold_metrics])
            mean_metrics[key] = m
            mean_metrics[f"{key}_ci95"] = hw

    if out_dir:
        final_model = RandomForestClassifier(
            n_estimators=500, random_state=RANDOM_SEED, n_jobs=-1,
            class_weight="balanced")
        final_model.fit(X, y)
        imp = sorted(zip(feat_cols, final_model.feature_importances_),
                     key=lambda x: -x[1])
        print(f"\n  [Top15 importance @{tag}]")
        for name, v in imp[:15]:
            print(f"    {name:28s} {v:.4f}")
        joblib.dump(final_model, os.path.join(out_dir, f"{tag}_model.pkl"))
        with open(os.path.join(out_dir, f"{tag}_importance.json"), "w") as f:
            json.dump({k: float(v) for k, v in imp}, f, indent=2)
        oof_df = df[["file", "cycle_id", "struct_hash", "label_real", "y"]].copy()
        oof_df["oof_prob"] = oof_prob if is_binary else -1
        oof_df["oof_pred"] = oof_pred
        oof_df.to_csv(os.path.join(out_dir, f"{tag}_oof.csv"), index=False)

    FOLD_METRICS[tag] = fold_metrics
    return mean_metrics, oof_prob, oof_pred


def collate_hetero(batch):
    return Batch.from_data_list(batch)


class HeteroGNN(torch.nn.Module):
    def __init__(self, hidden=32, num_classes=2, num_layers=1, dropout=0.3):
        super().__init__()
        self.lin_lock = Linear(LOCK_FEAT_DIM, hidden)
        self.lin_acq = Linear(ACQ_FEAT_DIM, hidden)
        self.lin_global = Linear(GLOBAL_FEAT_DIM, hidden)
        self.hidden = hidden

        def make_nn():
            return torch.nn.Sequential(torch.nn.Linear(hidden, hidden), torch.nn.ReLU(),
                                       torch.nn.Linear(hidden, hidden))

        self.convs = torch.nn.ModuleList()
        for _ in range(num_layers):
            conv = HeteroConv({
                ("lock", "lock_edge", "lock"): SAGEConv((hidden, hidden), hidden),
                ("acq", "call_edge", "acq"): SAGEConv((hidden, hidden), hidden),
                ("acq", "acq_to_lock", "lock"): SAGEConv((hidden, hidden), hidden),
                ("lock", "lock_to_acq", "acq"): SAGEConv((hidden, hidden), hidden),
            }, aggr="sum")
            self.convs.append(conv)

        self.norm = torch.nn.LayerNorm(hidden * 3)
        self.dropout = dropout
        self.classifier = torch.nn.Sequential(
            torch.nn.Linear(hidden * 3, hidden),
            torch.nn.ReLU(),
            torch.nn.Dropout(dropout),
            torch.nn.Linear(hidden, num_classes),
        )

    def forward(self, data):
        lock_x = _normalize(data["lock"].x, "lock")
        acq_x = _normalize(data["acq"].x, "acq")
        glob_x = _normalize(data.global_feat, "global")

        x_dict = {
            "lock": F.relu(self.lin_lock(lock_x)),
            "acq": F.relu(self.lin_acq(acq_x)),
        }
        edge_index_dict = {
            ("lock", "lock_edge", "lock"): data["lock", "lock_edge", "lock"].edge_index,
            ("acq", "call_edge", "acq"): data["acq", "call_edge", "acq"].edge_index,
            ("acq", "acq_to_lock", "lock"): data["acq", "acq_to_lock", "lock"].edge_index,
            ("lock", "lock_to_acq", "acq"): data["lock", "lock_to_acq", "acq"].edge_index,
        }

        for conv in self.convs:
            x_dict = conv(x_dict, edge_index_dict)
            x_dict = {k: F.relu(v) for k, v in x_dict.items()}
            x_dict = {k: F.dropout(v, p=self.dropout, training=self.training) for k, v in x_dict.items()}

        num_graphs = data.y.size(0)
        pooled = []
        for k in ["lock", "acq"]:
            if x_dict[k].size(0) > 0 and hasattr(data[k], "batch"):
                pooled.append(global_mean_pool(x_dict[k], data[k].batch))
            else:
                pooled.append(torch.zeros(num_graphs, self.hidden, device=data.y.device))

        g_pool = torch.cat(pooled, dim=-1)
        g_glob = F.relu(self.lin_global(glob_x))

        g = torch.cat([g_pool, g_glob], dim=-1)
        g = self.norm(g)

        return self.classifier(g)


def _split_train_val(train_ds, val_ratio=0.15, seed=RANDOM_SEED):
    n = len(train_ds)
    if n < 5: return train_ds, train_ds[:0]
    rng = np.random.RandomState(seed)
    idx = np.arange(n);
    rng.shuffle(idx)
    n_val = max(1, int(n * val_ratio))
    return [train_ds[i] for i in idx[n_val:]], [train_ds[i] for i in idx[:n_val]]


def train_and_eval_gnn(train_ds, test_ds, num_classes=2, epochs=100, hidden=32, num_layers=1, dropout=0.3, lr=1e-3,
                       patience=15, model_cls=HeteroGNN, use_focal=False, fold_seed=None,
                       reset_stats=True, cross_project_mode=False, **model_kwargs):

    if fold_seed is None: fold_seed = RANDOM_SEED

    random.seed(fold_seed)
    torch.manual_seed(fold_seed)
    np.random.seed(fold_seed)
    if torch.cuda.is_available():
        torch.cuda.manual_seed_all(fold_seed)

    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    if reset_stats:
        reset_feat_stats()
    compute_feat_stats(train_ds)

    model = model_cls(hidden=hidden, num_classes=num_classes, num_layers=num_layers, dropout=dropout,
                      **model_kwargs).to(device)
    opt = torch.optim.Adam(model.parameters(), lr=lr, weight_decay=1e-3)
    criterion = FocalLoss(gamma=2.0) if use_focal else torch.nn.CrossEntropyLoss()

    tr_ds, val_ds = _split_train_val(train_ds, val_ratio=0.15, seed=fold_seed)

    g = torch.Generator()
    g.manual_seed(fold_seed)
    train_loader = DataLoader(tr_ds, batch_size=16, shuffle=True,
                              collate_fn=collate_hetero, num_workers=0,
                              generator=g)
    val_loader = DataLoader(val_ds, batch_size=16, shuffle=False,
                            collate_fn=collate_hetero) if val_ds else None

    effective_patience = epochs + 1 if cross_project_mode else patience

    best_score, best_state, no_improve = -np.inf, None, 0
    for epoch in range(1, epochs + 1):
        model.train()
        for batch in train_loader:
            batch = batch.to(device)
            opt.zero_grad()
            out = model(batch)
            loss = criterion(out, batch.y.view(-1))
            loss.backward()
            opt.step()

        if val_loader is not None and not cross_project_mode:
            model.eval()
            all_prob, all_y = [], []
            with torch.no_grad():
                for batch in val_loader:
                    batch = batch.to(device)
                    all_prob.append(F.softmax(model(batch), dim=-1).cpu().numpy())
                    all_y.append(batch.y.view(-1).cpu().numpy())
            if all_prob:
                all_prob = np.concatenate(all_prob)
                all_y = np.concatenate(all_y)
                if len(np.unique(all_y)) < 2:
                    score = 0.0
                else:
                    score = roc_auc_score(all_y, all_prob[:, 1]) if num_classes == 2 else f1_score(
                        all_y, all_prob.argmax(axis=1), average="macro", zero_division=0)
            else:
                score = 0.0
        else:
            score = float(epoch)

        if score > best_score + 1e-5:
            best_score = score
            best_state = {k: v.detach().cpu().clone() for k, v in model.state_dict().items()}
            no_improve = 0
        else:
            no_improve += 1
            if no_improve >= effective_patience:
                break

    if best_state is not None:
        model.load_state_dict({k: v.to(device) for k, v in best_state.items()})

    model.eval()
    test_loader = DataLoader(test_ds, batch_size=16, shuffle=False,
                             collate_fn=collate_hetero)
    all_prob, all_y = [], []
    with torch.no_grad():
        for batch in test_loader:
            batch = batch.to(device)
            all_prob.append(F.softmax(model(batch), dim=-1).cpu().numpy())
            all_y.append(batch.y.view(-1).cpu().numpy())
    return best_score, np.concatenate(all_prob) if all_prob else None, np.concatenate(all_y) if all_y else None


def eval_gnn_on_folds(cycles, df, folds, num_classes=2, epochs=100, hidden=32,
                      num_layers=1, dropout=0.3, lr=1e-3,
                      drop_lock_edge=False, drop_call_edge=False, drop_cross=False, drop_line=False,
                      abl_node="full", abl_edge="full", abl_global="full",
                      tag="gnn", model_cls=HeteroGNN, use_focal=False,
                      model_kwargs=None):

    valid_idx = df.index.values
    valid_cycles = [cycles[i] for i in valid_idx]

    data_list = [cycle_to_hetero(c,
                                 drop_lock_edge=drop_lock_edge,
                                 drop_call_edge=drop_call_edge,
                                 drop_cross=drop_cross,
                                 drop_line=drop_line,
                                 abl_node=abl_node,
                                 abl_edge=abl_edge,
                                 abl_global=abl_global)
                 for c in valid_cycles]

    y_eff = df["y"].values.astype(int)
    for i, d in enumerate(data_list):
        d.y = torch.tensor([int(y_eff[i])], dtype=torch.long)

    fold_metrics, oof_prob, oof_pred = [], np.zeros((len(data_list), num_classes)), np.zeros(len(data_list), dtype=int)
    for k, (tr, te) in enumerate(folds):
        tr_ds = [data_list[i] for i in tr];
        te_ds = [data_list[i] for i in te]
        y_te = np.array([d.y.item() for d in te_ds])
        if len(np.unique(y_te)) < 2 and num_classes == 2:
            fold_metrics.append({"auc": float("nan"), "f1": float("nan"), "acc": float("nan")});
            continue

        _, prob, _ = train_and_eval_gnn(tr_ds, te_ds, num_classes=num_classes, epochs=epochs, hidden=hidden,
                                        num_layers=num_layers, dropout=dropout, lr=lr, model_cls=model_cls,
                                        use_focal=use_focal, fold_seed=RANDOM_SEED + k,
                                        reset_stats=True, cross_project_mode=False,
                                        **(model_kwargs or {}))
        if prob is None:
            fold_metrics.append({"auc": float("nan"), "f1": float("nan"), "acc": float("nan")});
            continue

        oof_prob[te] = prob;
        pred = prob.argmax(axis=1);
        oof_pred[te] = pred

        if num_classes == 2:
            fold_metrics.append(
                {"auc": float(roc_auc_score(y_te, prob[:, 1])), "f1": float(f1_score(y_te, pred, zero_division=0)),
                 "acc": float(accuracy_score(y_te, pred))})
        else:
            fold_metrics.append({"f1_macro": float(f1_score(y_te, pred, average="macro", zero_division=0)),
                                 "acc": float(accuracy_score(y_te, pred))})
        print(f"  fold {k}: {fold_metrics[-1]}")

    if not fold_metrics: return None, oof_prob, oof_pred
    mean_metrics = {}
    for key in ["auc", "f1", "acc"] if num_classes == 2 else ["f1_macro", "acc"]:
        m, hw = mean_ci95([x[key] for x in fold_metrics]);
        mean_metrics[key] = m;
        mean_metrics[f"{key}_ci95"] = hw
    FOLD_METRICS[tag] = fold_metrics
    return mean_metrics, oof_prob, oof_pred


def triage_at_recall(df, prob_col="oof_prob", y_col="y", target_recall=0.90):
    if len(df) == 0:
        return None
    d = df.sort_values(prob_col, ascending=False).reset_index(drop=True)
    y = d[y_col].values.astype(int)
    n_pos = int(y.sum())
    if n_pos == 0:
        return {"n_total": len(d), "n_pos": 0, "target_recall": target_recall,
                "need_pos": 0, "inspections": 0, "review_reduction": 0.0,
                "random_expected": 0, "reduction_vs_random": 0.0}
    need = int(np.ceil(target_recall * n_pos))
    cum_pos = np.cumsum(y)
    k = int(np.argmax(cum_pos >= need)) + 1
    return {
        "n_total": len(d),
        "n_pos": n_pos,
        "target_recall": target_recall,
        "need_pos": need,
        "inspections": k,
        "review_reduction": float(1 - k / len(d)),
        "random_expected": int(np.ceil(target_recall * len(d))),
        "reduction_vs_random": float(1 - k / (target_recall * len(d))),
    }


def triage_at_precision(df, prob_col="oof_prob", y_col="y", target_precision=0.80):
    if len(df) == 0:
        return None
    d = df.sort_values(prob_col, ascending=False).reset_index(drop=True)
    y = d[y_col].values.astype(int)
    n_pos = int(y.sum())
    if n_pos == 0:
        return {"target_precision": target_precision, "recall": 0.0, "k": 0}
    cum_tp = np.cumsum(y)
    cum_fp = np.cumsum(1 - y)
    precision = cum_tp / (cum_tp + cum_fp + 1e-12)
    valid = np.where(precision >= target_precision)[0]
    if len(valid) == 0:
        return {"target_precision": target_precision, "recall": 0.0, "k": 0}
    k = int(valid[-1]) + 1
    recall = float(cum_tp[k - 1] / n_pos)
    return {"target_precision": target_precision, "recall": recall, "k": k}


def eval_rule_lr_on_folds(df, folds, tag="lr", out_dir=None):
    return eval_sklearn_on_folds(df, CYCLE_FEATS, folds, tag=tag, out_dir=out_dir)


def wilcoxon_test(a, b):
    a = np.asarray(a, dtype=float)
    b = np.asarray(b, dtype=float)
    mask = ~(np.isnan(a) | np.isnan(b))
    a, b = a[mask], b[mask]
    if len(a) < 2:
        return float("nan"), float("nan"), float("nan")
    diff = a - b
    if np.allclose(diff, 0):
        return float("nan"), 1.0, 0.0
    try:
        stat, p_val = wilcoxon(a, b)
        return float(stat), float(p_val), float(np.mean(diff))
    except Exception:
        return float("nan"), float("nan"), float(np.mean(diff))


def triage_report(oof_path, tag, label_mode):
    if not label_mode.startswith("binary"):
        return None
    if not os.path.exists(oof_path):
        return None
    df = pd.read_csv(oof_path)
    if "oof_prob" not in df.columns or "y" not in df.columns:
        return None
    if (df["oof_prob"] == -1).all():
        return None

    res = {
        "tag": tag,
        "recall@0.90": triage_at_recall(df, target_recall=0.90),
        "recall@0.95": triage_at_recall(df, target_recall=0.95),
        "precision@0.80": triage_at_precision(df, target_precision=0.80),
        "precision@0.90": triage_at_precision(df, target_precision=0.90),
    }
    return res


def eval_cross_project(df_train, df_test, cycles_train, cycles_test,
                       label_mode="binary_strong", out_dir=None,
                       gnn_epochs=100, gnn_hidden=32, gnn_layers=1,
                       gnn_dropout=0.3, gnn_lr=1e-3):
    df_tr = apply_label_mode(df_train, label_mode)
    df_te = apply_label_mode(df_test, label_mode)

    print(f"\n[cross-project] train samples = {len(df_tr)}, test samples = {len(df_te)}")
    print(f"[cross-project] train y = {df_tr['y'].value_counts().to_dict()}")
    print(f"[cross-project] test  y = {df_te['y'].value_counts().to_dict()}")

    is_binary = df_tr["y"].nunique() == 2
    y_te = df_te["y"].values.astype(int)
    results = {}

    pred_rule = (
        (df_te["all_edges_same_thread"] == 1)
        & (df_te["all_edges_reachable"] == 1)
        & (df_te["thread_count"] >= 2)
        & (df_te["max_thread_occurrence"] <= 1)
    ).astype(int).values
    if is_binary:
        results["rule"] = {
            "auc": float(roc_auc_score(y_te, pred_rule)) if len(np.unique(y_te)) > 1 else float("nan"),
            "f1": float(f1_score(y_te, pred_rule, zero_division=0)),
            "acc": float(accuracy_score(y_te, pred_rule)),
        }
    else:
        pred3 = np.where(pred_rule == 1, 2, 0)
        results["rule"] = {
            "f1_macro": float(f1_score(y_te, pred3, average="macro", zero_division=0)),
            "acc": float(accuracy_score(y_te, pred3)),
        }
    print(f"  [rule] {results['rule']}")

    X_tr = df_tr[GRAPH_FEATS].fillna(0).values.astype(np.float32)
    X_te = df_te[GRAPH_FEATS].fillna(0).values.astype(np.float32)
    y_tr = df_tr["y"].values.astype(int)

    for tag in ["rf", "lr", "xgb", "svm"]:
        if tag == "rf":
            model = RandomForestClassifier(
                n_estimators=300, random_state=RANDOM_SEED, n_jobs=-1,
                class_weight="balanced")
        elif tag == "lr":
            model = Pipeline([
                ("scaler", StandardScaler()),
                ("clf", LogisticRegression(
                    max_iter=2000, class_weight="balanced",
                    random_state=RANDOM_SEED)),
            ])
        elif tag == "svm":
            model = Pipeline([
                ("scaler", StandardScaler()),
                ("clf", SVC(kernel="rbf", C=1.0, gamma="scale",
                            probability=True, class_weight="balanced",
                            random_state=RANDOM_SEED)),
            ])
        else:
            model = XGBClassifier(
                n_estimators=400, max_depth=4, learning_rate=0.05,
                subsample=0.9, colsample_bytree=0.9,
                reg_lambda=1.0, min_child_weight=1,
                objective="binary:logistic" if is_binary else "multi:softprob",
                num_class=None if is_binary else 3,
                eval_metric="logloss" if is_binary else "mlogloss",
                tree_method="hist", random_state=RANDOM_SEED, n_jobs=-1,
            )
        model.fit(X_tr, y_tr)
        if is_binary:
            prob = model.predict_proba(X_te)[:, 1]
            pred = (prob >= 0.5).astype(int)
            res = {
                "auc": float(roc_auc_score(y_te, prob)) if len(np.unique(y_te)) > 1 else float("nan"),
                "f1": float(f1_score(y_te, pred, zero_division=0)),
                "acc": float(accuracy_score(y_te, pred)),
            }
        else:
            prob = model.predict_proba(X_te)
            pred = model.predict(X_te)
            res = {
                "f1_macro": float(f1_score(y_te, pred, average="macro", zero_division=0)),
                "acc": float(accuracy_score(y_te, pred)),
            }
        results[tag] = res
        print(f"  [{tag}] {res}")

    cycles_tr = [cycles_train[i] for i in df_tr.index.values]
    cycles_te = [cycles_test[i] for i in df_te.index.values]

    num_classes = 2 if is_binary else 3
    data_tr = [cycle_to_hetero(c) for c in cycles_tr]
    data_te = [cycle_to_hetero(c) for c in cycles_te]

    y_tr_eff = df_tr["y"].values.astype(int)
    y_te_eff = df_te["y"].values.astype(int)
    for i, d in enumerate(data_tr):
        d.y = torch.tensor([int(y_tr_eff[i])], dtype=torch.long)
    for i, d in enumerate(data_te):
        d.y = torch.tensor([int(y_te_eff[i])], dtype=torch.long)

    _, prob_gnn, _ = train_and_eval_gnn(
        data_tr, data_te, num_classes=num_classes,
        epochs=gnn_epochs, hidden=gnn_hidden, num_layers=gnn_layers,
        dropout=gnn_dropout, lr=gnn_lr, model_cls=HeteroGNN,
        reset_stats=True, cross_project_mode=True)

    if prob_gnn is not None:
        if num_classes == 2:
            pred_gnn = prob_gnn[:, 1] >= 0.5
            results["gnn"] = {
                "auc": float(roc_auc_score(y_te_eff, prob_gnn[:, 1])) if len(np.unique(y_te_eff)) > 1 else float("nan"),
                "f1": float(f1_score(y_te_eff, pred_gnn, zero_division=0)),
                "acc": float(accuracy_score(y_te_eff, pred_gnn)),
            }
            if out_dir:
                oof = df_te.copy()
                oof["oof_prob"] = prob_gnn[:, 1]
                oof["oof_pred"] = pred_gnn.astype(int)
                oof.to_csv(os.path.join(out_dir, "cross_gnn_oof.csv"), index=False)
        else:
            pred_gnn = prob_gnn.argmax(axis=1)
            results["gnn"] = {
                "f1_macro": float(f1_score(y_te_eff, pred_gnn, average="macro", zero_division=0)),
                "acc": float(accuracy_score(y_te_eff, pred_gnn)),
            }
        print(f"  [gnn] {results['gnn']}")

    if out_dir and is_binary:
        for tag in ["rf", "lr", "xgb", "svm"]:
            if tag == "rf":
                model = RandomForestClassifier(
                    n_estimators=300, random_state=RANDOM_SEED, n_jobs=-1,
                    class_weight="balanced")
            elif tag == "lr":
                model = Pipeline([
                    ("scaler", StandardScaler()),
                    ("clf", LogisticRegression(
                        max_iter=2000, class_weight="balanced",
                        random_state=RANDOM_SEED)),
                ])
            else:
                model = XGBClassifier(
                    n_estimators=400, max_depth=4, learning_rate=0.05,
                    subsample=0.9, colsample_bytree=0.9,
                    reg_lambda=1.0, min_child_weight=1,
                    objective="binary:logistic",
                    eval_metric="logloss",
                    tree_method="hist", random_state=RANDOM_SEED, n_jobs=-1,
                )
            model.fit(X_tr, y_tr)
            prob = model.predict_proba(X_te)[:, 1]
            oof = df_te.copy()
            oof["oof_prob"] = prob
            oof["oof_pred"] = (prob >= 0.5).astype(int)
            oof.to_csv(os.path.join(out_dir, f"cross_{tag}_oof.csv"), index=False)

    return results


GNN_ABLATIONS = {
    "node_full": dict(abl_node="full", model_cls=HeteroGNN),
    "node_no_static": dict(abl_node="no_static", model_cls=HeteroGNN),
    "node_no_degree": dict(abl_node="no_degree", model_cls=HeteroGNN),
    "node_no_line": dict(abl_node="full", drop_line=True, model_cls=HeteroGNN),
    "node_zero": dict(abl_node="zero", model_cls=HeteroGNN),

    "edge_full": dict(abl_edge="full", model_cls=HeteroGNN),
    "edge_zero": dict(abl_edge="zero", model_cls=HeteroGNN),

    "arch_full": dict(model_cls=HeteroGNN),
    "arch_no_cross": dict(model_cls=HeteroGNN, drop_cross=True),
    "arch_no_lock": dict(model_cls=HeteroGNN, drop_lock_edge=True),
    "arch_no_call": dict(model_cls=HeteroGNN, drop_call_edge=True),

    "global_zero": dict(abl_global="zero", model_cls=HeteroGNN),
}

ABL_KIND_MAP = {
    "node": ["node_full", "node_no_static", "node_no_degree", "node_no_line", "node_zero"],
    "edge": ["edge_full", "edge_zero"],
    "arch": ["arch_full", "arch_no_cross", "arch_no_lock", "arch_no_call"],
    "global": ["global_zero"],
}


def run_gnn_ablation(cycles, df, folds, num_classes, args, kinds=None):
    if kinds is None:
        kinds = ["node", "edge", "arch"]

    selected = []
    for k in kinds:
        for name in ABL_KIND_MAP.get(k, []):
            selected.append(name)

    results = {}
    print("\n\n")
    print("4. GNN ablation study")
    print(f"   kinds = {kinds}")
    print(f"   runs  = {len(selected)}")

    for name in selected:
        cfg = dict(GNN_ABLATIONS[name])
        model_cls = cfg.pop("model_cls", HeteroGNN)
        model_kwargs = cfg.pop("model_kwargs", None)
        print(f"\n  [{name}]  cls={model_cls.__name__}  cfg={cfg}  kwargs={model_kwargs}")

        res, oof_prob, oof_pred = eval_gnn_on_folds(
            cycles, df, folds, num_classes=num_classes,
            epochs=args.gnn_epochs, hidden=args.gnn_hidden,
            num_layers=args.gnn_layers, dropout=args.gnn_dropout,
            lr=args.gnn_lr, tag=f"gnn_{name}",
            model_cls=model_cls, use_focal=args.use_focal,
            model_kwargs=model_kwargs,
            **cfg)
        results[name] = res
        print(f"    -> {res}")

        oof_df = df[df["label_real"].isin([0, 1, 2])][
            ["file", "cycle_id", "struct_hash", "label_real", "y"]].copy()
        if num_classes == 2 and oof_prob is not None:
            oof_df["oof_prob"] = oof_prob[:, 1]
        else:
            oof_df["oof_prob"] = -1
        oof_df["oof_pred"] = oof_pred
        oof_df.to_csv(os.path.join(args.out_dir, f"gnn_{name}_oof.csv"), index=False)

    rows = []
    for name, m in results.items():
        if m is None:
            continue
        if num_classes == 2:
            rows.append({
                "ablation": name,
                "auc": m.get("auc"),
                "auc_ci95": m.get("auc_ci95"),
                "f1": m.get("f1"),
                "f1_ci95": m.get("f1_ci95"),
                "acc": m.get("acc"),
                "acc_ci95": m.get("acc_ci95"),
            })
        else:
            rows.append({
                "ablation": name,
                "f1_macro": m.get("f1_macro"),
                "f1_macro_ci95": m.get("f1_macro_ci95"),
                "acc": m.get("acc"),
                "acc_ci95": m.get("acc_ci95"),
            })
    if rows:
        pd.DataFrame(rows).to_csv(
            os.path.join(args.out_dir, "gnn_ablation.csv"), index=False)
        print(f"\n[save] {args.out_dir}/gnn_ablation.csv")

    return results


def record_seed_metrics(tag, fold_metrics, metric_keys):
    if not fold_metrics:
        return
    SEED_METRICS[tag] = {}
    for k in metric_keys:
        vals = [m.get(k) for m in fold_metrics if m.get(k) is not None]
        vals = [v for v in vals if not (isinstance(v, float) and np.isnan(v))]
        if vals:
            SEED_METRICS[tag][k] = float(np.mean(vals))


def paired_ttest_across_seeds(a, b):
    a = np.asarray(a, dtype=float)
    b = np.asarray(b, dtype=float)
    mask = ~(np.isnan(a) | np.isnan(b))
    a, b = a[mask], b[mask]
    if len(a) < 2:
        return float("nan"), float("nan"), float("nan")
    t_stat, p_val = ttest_rel(a, b)
    return float(t_stat), float(p_val), float(np.mean(a - b))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--data_dir", default="dataset/train")
    ap.add_argument("--test_dir", default="dataset/test")
    ap.add_argument("--out_dir", default="./output")
    ap.add_argument("--seed", type=int, default=42)
    ap.add_argument("--label_mode", default="binary_strong",
                    choices=["binary_strong", "binary_loose", "three_way"],
                    help="Label modes: strong = train only on L2; loose = both L2+L1 count as positive; three_way = L0/L1/L2.")
    ap.add_argument("--n_splits", type=int, default=5)
    ap.add_argument("--gnn_epochs", type=int, default=100)
    ap.add_argument("--gnn_hidden", type=int, default=32)
    ap.add_argument("--gnn_layers", type=int, default=1)
    ap.add_argument("--gnn_lr", type=float, default=1e-3)
    ap.add_argument("--gnn_dropout", type=float, default=0.3)
    ap.add_argument("--use_focal", action="store_true", default=False)
    ap.add_argument("--do_rf", action="store_true", default=True)
    ap.add_argument("--do_gnn", action="store_true", default=True)
    ap.add_argument("--do_grid", action="store_true")
    ap.add_argument("--do_gnn_ablation", action="store_true",
                    help="Run GNN node/edge/arch ablation study")
    ap.add_argument("--abl_kinds", nargs="+", default=["node", "edge", "arch"],
                    choices=["node", "edge", "arch", "global"],
                    help="Which ablation groups to run")
    ap.add_argument("--do_lr", action="store_true", default=True)
    ap.add_argument("--do_xgb", action="store_true", default=True)
    ap.add_argument("--do_svm", action="store_true", default=True)
    args = ap.parse_args()

    global RANDOM_SEED
    RANDOM_SEED = args.seed
    np.random.seed(RANDOM_SEED)
    torch.manual_seed(RANDOM_SEED)
    random.seed(RANDOM_SEED)

    timestamp = time.strftime("%Y%m%d-%H%M%S")
    run_tag = f"output-seed{RANDOM_SEED}-{timestamp}"
    args.out_dir = os.path.join(args.out_dir, run_tag)
    os.makedirs(args.out_dir, exist_ok=True)

    console_path = os.path.join(args.out_dir, f"console-output-{args.label_mode}-seed{args.seed}.txt")
    console_file = open(console_path, "w", encoding="utf-8")

    class Tee:
        def __init__(self, *streams):
            self.streams = streams

        def write(self, data):
            for s in self.streams:
                s.write(data)
                s.flush()

        def flush(self):
            for s in self.streams:
                s.flush()

    sys.stdout = Tee(sys.__stdout__, console_file)
    sys.stderr = Tee(sys.__stderr__, console_file)

    print("Unified comparison of rule-based / random forest / heterogeneous GNN")
    print(f"  data_dir   = {args.data_dir}")
    print(f"  test_dir   = {args.test_dir}")
    print(f"  out_dir    = {args.out_dir}")
    print(f"  label_mode = {args.label_mode}")
    print(f"  n_splits   = {args.n_splits}")
    print(f"  gnn        = hidden={args.gnn_hidden}, layers={args.gnn_layers}, "
          f"lr={args.gnn_lr}, dropout={args.gnn_dropout}, epochs={args.gnn_epochs}")
    print(f"  loss       = {'focal' if args.use_focal else 'cross-entropy (no class weights)'}")
    print(f"  gnn_ablation = {args.do_gnn_ablation}  kinds={args.abl_kinds}")

    cycles_train = load_all(args.data_dir)
    cycles_test = load_all(args.test_dir)
    cycles = cycles_train + cycles_test
    if not cycles:
        print("[ERROR] no data found")
        return

    df = build_feature_table(cycles)
    df = apply_label_mode(df, args.label_mode)
    print(f"\n[data] Total samples={len(df)}  Unique structure={df['struct_hash'].nunique()}")
    print(f"[data] y Distribution = {df['y'].value_counts().to_dict()}")

    quality_report(df, args.out_dir)

    folds = make_folds(df, n_splits=args.n_splits)
    print(f"\n[fold] GroupKFold n_splits={len(folds)}  (After merging train+test, group by `struct_hash`)")

    print("\n\n")
    print("1. Rule Baseline (reference: hard-coded isReal)")
    rule_res = rule_baseline(df)

    print("\n\n")
    print("1b. Rule-LR (learned on CYCLE_FEATS only)")
    rule_lr_res, rule_lr_oof_prob, rule_lr_oof_pred = eval_rule_lr_on_folds(
        df, folds, tag="lr", out_dir=args.out_dir)
    print(f"  [Rule-LR GroupCV] {rule_lr_res}")
    record_seed_metrics("rule_lr", FOLD_METRICS.get("rule_lr"), ["auc", "f1", "acc", "f1_macro"])

    print("\n\n")
    print("2. Statistical Models")
    print("2a. Logistic Regression")
    lr_res = None
    if args.do_lr:
        lr_res, lr_oof_prob, lr_oof_pred = eval_sklearn_on_folds(
            df, GRAPH_FEATS, folds, tag="lr", out_dir=args.out_dir)
        print(f"  [LR GroupCV] {lr_res}")
        record_seed_metrics("lr", FOLD_METRICS.get("lr"), ["auc", "f1", "acc", "f1_macro"])

    print("\n\n")
    print("2b. SVM (RBF)")
    svm_res = None
    if args.do_svm:
        svm_res, svm_oof_prob, svm_oof_pred = eval_sklearn_on_folds(
            df, GRAPH_FEATS, folds, tag="svm", out_dir=args.out_dir)
        print(f"  [SVM GroupCV] {svm_res}")
        record_seed_metrics("svm", FOLD_METRICS.get("svm"), ["auc", "f1", "acc", "f1_macro"])

    print("\n\n")
    print("2c. Random Forest")
    rf_res = None
    if args.do_rf:
        rf_res, rf_oof_prob, rf_oof_pred = eval_rf_on_folds(
            df, GRAPH_FEATS, folds, tag="rf",
            do_grid=args.do_grid, out_dir=args.out_dir)
        print(f"  [RF GroupCV average] {rf_res}")
        record_seed_metrics("rf", FOLD_METRICS.get("rf"), ["auc", "f1", "acc", "f1_macro"])

    print("\n\n")
    print("2d. XGBoost")
    xgb_res = None
    if args.do_xgb:
        xgb_res, xgb_oof_prob, xgb_oof_pred = eval_sklearn_on_folds(
            df, GRAPH_FEATS, folds, tag="xgb", out_dir=args.out_dir)
        print(f"  [XGB GroupCV] {xgb_res}")
        record_seed_metrics("xgb", FOLD_METRICS.get("xgb"), ["auc", "f1", "acc", "f1_macro"])

    print("\n\n")
    print("3. Heterogeneous GNN")
    gnn_res = None
    if args.do_gnn:
        num_classes = 2 if args.label_mode.startswith("binary") else 3
        gnn_res, gnn_oof_prob, gnn_oof_pred = eval_gnn_on_folds(
            cycles, df, folds, num_classes=num_classes,
            epochs=args.gnn_epochs, hidden=args.gnn_hidden,
            num_layers=args.gnn_layers, dropout=args.gnn_dropout,
            lr=args.gnn_lr, tag="gnn_full",
            model_cls=HeteroGNN, use_focal=args.use_focal)
        if gnn_res:
            print(f"  [GNN GroupCV average] {gnn_res}")
            oof_df = df[df["label_real"].isin([0, 1, 2])][["file", "cycle_id", "struct_hash", "label_real", "y"]].copy()
            if num_classes == 2:
                oof_df["oof_prob"] = gnn_oof_prob[:, 1]
            else:
                oof_df["oof_prob"] = -1
            oof_df["oof_pred"] = gnn_oof_pred
            oof_df.to_csv(os.path.join(args.out_dir, "gnn_oof.csv"), index=False)
            record_seed_metrics("gnn_full", FOLD_METRICS.get("gnn_full"), ["auc", "f1", "acc", "f1_macro"])

    gnn_ablation_res = {}
    if args.do_gnn_ablation:
        num_classes = 2 if args.label_mode.startswith("binary") else 3
        gnn_ablation_res = run_gnn_ablation(
            cycles, df, folds, num_classes, args,
            kinds=args.abl_kinds)

    print("\n\n")
    print("5. Cross-project generalization (train on train_dir, test on test_dir)")
    df_train_only = build_feature_table(cycles_train)
    df_test_only = build_feature_table(cycles_test)

    cross_res = eval_cross_project(
        df_train_only, df_test_only,
        cycles_train, cycles_test,
        label_mode=args.label_mode,
        out_dir=args.out_dir,
        gnn_epochs=args.gnn_epochs,
        gnn_hidden=args.gnn_hidden,
        gnn_layers=args.gnn_layers,
        gnn_dropout=args.gnn_dropout,
        gnn_lr=args.gnn_lr)

    print("\n[cross-project summary]")
    for k, v in cross_res.items():
        print(f"  {k:8s} {v}")

    cross_triage = {}
    if args.label_mode.startswith("binary"):
        for tag in ["lr", "svm", "rf", "xgb", "gnn"]:
            path = os.path.join(args.out_dir, f"cross_{tag}_oof.csv")
            r = triage_report(path, tag=tag, label_mode=args.label_mode)
            if r is not None:
                cross_triage[tag] = r
                print(f"  [cross-{tag:6s}] "
                      f"inspect {r['recall@0.90']['inspections']:>4d}/{r['recall@0.90']['n_total']:<4d} "
                      f"for R=0.90 (reduce {r['recall@0.90']['review_reduction']:5.1%}) | "
                      f"R@P=0.80 = {r['precision@0.80']['recall']:.3f}")

    print("\n\n")
    print("Final comparison (GroupKFold by struct_hash)")
    is_binary = args.label_mode.startswith("binary")

    rows = []
    if rule_res:
        rows.append(("Rule baseline", rule_res))
    if rule_lr_res:
        rows.append(("Rule-LR", rule_lr_res))
    if lr_res:
        rows.append(("LogisticReg", lr_res))
    if svm_res:
        rows.append(("SVM-RBF", svm_res))
    if rf_res:
        rows.append(("Random Forest", rf_res))
    if xgb_res:
        rows.append(("XGBoost", xgb_res))
    if gnn_res:
        rows.append(("HeteroGNN", gnn_res))

    if is_binary:
        print(f"  {'Model':16s} {'AUC (95% CI)':>22s} {'F1':>8s} {'Acc':>8s}")
        for name, m in rows:
            auc = m.get("auc", float("nan"))
            hw = m.get("auc_ci95", 0.0)
            print(f"  {name:16s} {auc:.4f} ± {hw:.4f}       "
                  f"{m.get('f1', float('nan')):8.4f} "
                  f"{m.get('acc', float('nan')):8.4f}")
    else:
        print(f"  {'Model':16s} {'macroF1 (95% CI)':>24s} {'Acc':>8s}")
        for name, m in rows:
            f1 = m.get("f1_macro", float("nan"))
            hw = m.get("f1_macro_ci95", 0.0)
            print(f"  {name:16s} {f1:.4f} ± {hw:.4f}        "
                  f"{m.get('acc', float('nan')):8.4f}")

    print("\n\n")
    print("Triage analysis (binary only; needs oof_prob)")
    triage_res = {}
    if is_binary:
        triage_targets = [
            ("Rule baseline", None),
            ("Rule-LR", os.path.join(args.out_dir, "rule_lr_oof.csv")),
            ("LogisticReg", os.path.join(args.out_dir, "lr_oof.csv")),
            ("SVM-RBF", os.path.join(args.out_dir, "svm_oof.csv")),
            ("Random Forest", os.path.join(args.out_dir, "rf_oof.csv")),
            ("XGBoost", os.path.join(args.out_dir, "xgb_oof.csv")),
            ("HeteroGNN", os.path.join(args.out_dir, "gnn_oof.csv")),
        ]
        for name, path in triage_targets:
            if path is None:
                continue
            r = triage_report(path, tag=name, label_mode=args.label_mode)
            if r is None:
                print(f"  [{name}] no oof_prob, skipped")
                continue
            triage_res[name] = r
            rr90 = r["recall@0.90"]["review_reduction"]
            rr95 = r["recall@0.95"]["review_reduction"]
            pr80 = r["precision@0.80"]["recall"]
            print(f"  [{name:14s}] "
                  f"inspect {r['recall@0.90']['inspections']:>4d}/{r['recall@0.90']['n_total']:<4d} "
                  f"for R=0.90 (reduce {rr90:5.1%}) | "
                  f"R=0.95 reduce {rr95:5.1%} | "
                  f"R@P=0.80 = {pr80:.3f}")

        if triage_res:
            rows_tri = []
            for name, r in triage_res.items():
                rows_tri.append({
                    "model": name,
                    "n_total": r["recall@0.90"]["n_total"],
                    "n_pos": r["recall@0.90"]["n_pos"],
                    "inspections@R90": r["recall@0.90"]["inspections"],
                    "review_reduction@R90": r["recall@0.90"]["review_reduction"],
                    "reduction_vs_random@R90": r["recall@0.90"]["reduction_vs_random"],
                    "inspections@R95": r["recall@0.95"]["inspections"],
                    "review_reduction@R95": r["recall@0.95"]["review_reduction"],
                    "recall@P80": r["precision@0.80"]["recall"],
                    "k@P80": r["precision@0.80"]["k"],
                    "recall@P90": r["precision@0.90"]["recall"],
                    "k@P90": r["precision@0.90"]["k"],
                })
            pd.DataFrame(rows_tri).to_csv(
                os.path.join(args.out_dir, f"triage_summary-{args.label_mode}-seed{args.seed}.csv"), index=False)
            print(f"\n[save] {args.out_dir}/triage_summary-{args.label_mode}-seed{args.seed}.csv")

    print("\n\n")
    print("GNN vs baselines: paired significance tests")
    metric = "auc" if is_binary else "f1_macro"
    gnn_tag = "gnn_full"
    baseline_tags = [
        ("Rule-LR", "rule_lr"),
        ("LogisticReg", "lr"),
        ("SVM-RBF", "svm"),
        ("XGBoost", "xgb"),
        ("Random Forest", "rf"),
    ]

    sig_res = {}
    if gnn_tag in FOLD_METRICS:
        gnn_vals = [m.get(metric) for m in FOLD_METRICS[gnn_tag]
                    if m.get(metric) is not None]
        print(f"  metric = {metric}   (paired by fold, n={len(gnn_vals)})")
        print(f"  {'Baseline':16s} {'mean(GNN)':>10s} {'mean(B)':>10s} "
              f"{'diff':>8s} {'t-p':>10s} {'w-p':>10s} {'c-p':>10s} {'sig':>5s}")
        for name, tag in baseline_tags:
            if tag not in FOLD_METRICS:
                continue
            b_vals = [m.get(metric) for m in FOLD_METRICS[tag]
                      if m.get(metric) is not None]
            n = min(len(gnn_vals), len(b_vals))
            if n < 2:
                continue
            gv, bv = gnn_vals[:n], b_vals[:n]
            _, p_t, diff = paired_ttest(gv, bv)
            _, p_w, _ = wilcoxon_test(gv, bv)
            n_train_fold = int(np.mean([len(tr) for tr, _ in folds]))
            n_test_fold = int(np.mean([len(te) for _, te in folds]))
            _, p_c = corrected_resampled_ttest(gv, bv, n_train_fold, n_test_fold)

            sig = "***" if p_t < 0.001 else "**" if p_t < 0.01 else "*" if p_t < 0.05 else ""
            print(f"  {name:16s} {np.mean(gv):>10.4f} {np.mean(bv):>10.4f} "
                  f"{diff:>8.4f} {p_t:>10.4g} {p_w:>10.4g} {p_c:>10.4g} {sig:>5s}")

            seed_level = {}
            if "gnn_full" in SEED_METRICS and tag in SEED_METRICS:
                g_seed = [SEED_METRICS["gnn_full"].get(metric)]
                b_seed = [SEED_METRICS[tag].get(metric)]
                if g_seed[0] is not None and b_seed[0] is not None:
                    seed_level = {
                        "n_seeds": 1,
                        "mean_gnn": float(g_seed[0]),
                        "mean_baseline": float(b_seed[0]),
                        "mean_diff": float(g_seed[0] - b_seed[0]),
                    }

            sig_res[name] = {
                "metric": metric,
                "mean_gnn": float(np.mean(gv)),
                "mean_baseline": float(np.mean(bv)),
                "mean_diff": float(diff),
                "paired_ttest_p": p_t,
                "wilcoxon_p": p_w,
                "corrected_resampled_ttest_p": p_c,
                "n_folds": n,
                "seed_level": seed_level,
            }
    else:
        print("  [skip] GNN fold metrics not found")

    summary = {
        "config": vars(args),
        "n_samples": len(df),
        "n_structures": int(df["struct_hash"].nunique()),
        "y_distribution": df["y"].value_counts().to_dict(),
        "rule_baseline": rule_res,
        "rule_lr": rule_lr_res,
        "random_forest": rf_res,
        "logistic_regression": lr_res,
        "xgboost": xgb_res,
        "svm": svm_res,
        "hetero_gnn": gnn_res,
        "gnn_ablation": gnn_ablation_res,
        "triage": triage_res,
        "cross_project": cross_res,
        "cross_triage": cross_triage,
        "gnn_vs_baselines": sig_res,
        "seed_metrics": {k: dict(v) for k, v in SEED_METRICS.items()},
    }

    file_name = f"summary-{args.label_mode}-seed{args.seed}.json"
    with open(os.path.join(args.out_dir, file_name), "w") as f:
        json.dump(summary, f, indent=2, default=str)
    print(f"\n[save] {args.out_dir}/{file_name}")

    sys.stdout = sys.__stdout__
    sys.stderr = sys.__stderr__
    console_file.close()
    print(f"[save] {console_path}")


if __name__ == "__main__":
    main()