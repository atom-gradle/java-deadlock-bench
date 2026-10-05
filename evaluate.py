# -*- coding: utf-8 -*-
"""
Uniformly evaluate the performance of three models on the "mutation ground truth (LABEL_REAL)":

  1. Rule-based baseline (replicating the logic of `classifyCycle.isReal`)
  2. Random forest (graph-level statistical features)
  3. Heterogeneous GNN (heterogeneous graph structure + edge-weight features)

Unified evaluation protocol:
  - GroupKFold by `struct_hash` (the same structure does not span train/test)
  - The three models share the same fold split to ensure a fair comparison
  - Support binary classification (`label==2` vs `label==0`) and three-class classification (2 / 1 / 0)
  - Support ablation (removing cross-layer edges / `lock_edge` / `call_edge`)

How to use?
  python evaluate_all.py --data_dir train --label_mode binary_strong
  python evaluate_all.py --data_dir train --label_mode three_way --do_grid --do_ablation
"""

import argparse,glob,hashlib,json,os,re,warnings,joblib
from collections import Counter

import numpy as np
import pandas as pd
import torch
import torch.nn.functional as F
from scipy.stats import mannwhitneyu
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import (
    roc_auc_score, f1_score, precision_score, recall_score,
    accuracy_score, )
from sklearn.model_selection import GroupKFold, GridSearchCV
from torch_geometric.data import HeteroData, Batch
from torch_geometric.loader import DataLoader
from torch_geometric.nn import GINEConv, HeteroConv, Linear, global_mean_pool

warnings.filterwarnings("ignore")

# Settings
RANDOM_SEED = 42
np.random.seed(RANDOM_SEED)
torch.manual_seed(RANDOM_SEED)

LOCK_FEAT_DIM = 5
ACQ_FEAT_DIM  = 6
LOCK_EDGE_FEAT_DIM = 3   # dist, may_alias, same_lock_obj
CALL_EDGE_FEAT_DIM = 3   # dist, same_method, nested

# Graph structure features (excluding CYCLE_FEATURES to avoid label leakage).
GRAPH_FEATS = [
    "n_lock", "n_acq", "n_lock_edge", "n_call_edge",
    "n_cross", "n_cross_atl", "n_cross_lta",
    "static_ratio", "nonnull_ratio",
    "avg_lock_dist", "alias_ratio", "same_obj_ratio",
    "sync_ratio", "acq_static_ratio", "avg_nesting", "avg_pts",
    "avg_call_dist", "nested_ratio",
]

# CYCLE_FEATURES（仅用于泄漏检测，不进入模型）
CYCLE_FEATS = [
    "thread_count", "max_thread_occurrence", "edge_count",
    "all_edges_same_thread", "all_edges_reachable", "has_non_adjacent_edge",
]

# 1. 解析器
def parse_file(filepath):
    cycles = []
    cur = None

    def new_cycle(cid):
        return {
            "file": os.path.basename(filepath),
            "cycle_id": cid,
            "lock_nodes": [], "lock_edges": [],
            "acq_nodes": [],  "call_edges": [],
            "cross": [], "features": {}, "labels": {},
        }

    for raw in open(filepath, encoding="utf-8"):
        line = raw.strip()
        if not line or line.startswith("#"):
            continue

        if line.startswith("CYCLE "):
            if cur is not None:
                cycles.append(cur)
            cur = new_cycle(int(line.split()[1]))
            continue
        if cur is None:
            continue

        if line.startswith("LOCK_NODE"):
            _, rest = line.split(None, 1)
            # 从右往左切 3 刀：obj_type, is_static, obj_nonnull（这三个不含逗号）
            key_part, obj_type, is_static, obj_nonnull = rest.rsplit(",", 3)
            nid, obj_key = key_part.split(",", 1)
            cur["lock_nodes"].append({
                "id": int(nid),
                "obj_key": obj_key,
                "obj_type": obj_type,
                "is_static": is_static.strip().lower() == "true",
                "obj_nonnull": int(obj_nonnull.strip()),
            })

        elif line.startswith("LOCK_EDGE"):
            _, rest = line.split(None, 1)
            p = rest.split(",")
            cur["lock_edges"].append({
                "from": int(p[0]), "to": int(p[1]),
                "from_method": p[2], "from_line": int(p[3]),
                "to_method": p[4], "to_line": int(p[5]),
                "dist": int(p[6]),
                "may_alias": p[7].strip().lower() == "true",
                "same_lock_obj": p[8].strip().lower() == "true",
            })

        elif line.startswith("ACQ_NODE"):
            _, rest = line.split(None, 1)
            p = rest.split(",", 7)
            cur["acq_nodes"].append({
                "id": int(p[0]), "method": p[1], "line": int(p[2]),
                "var": p[3],
                "is_static": p[4].strip().lower() == "true",
                "is_sync": p[5].strip().lower() == "true",
                "nesting": int(p[6]), "pts_size": int(p[7]),
            })

        elif line.startswith("CALL_EDGE"):
            _, rest = line.split(None, 1)
            p = rest.split(",")
            cur["call_edges"].append({
                "src": int(p[0]), "dst": int(p[1]), "dist": int(p[2]),
                "same_method": p[3].strip().lower() == "true",
                "nested": p[4].strip().lower() == "true",
            })

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
            # 4 值 ground truth（2/1/0/-1）
            cur["labels"]["real"] = int(line.split()[1])

    if cur is not None:
        cycles.append(cur)
    return cycles


def load_all(data_dir, pattern="graph-*-copy.txt"):
    """
    默认只读 -copy.txt（细粒度 LABEL_REAL 标注的版本）。
    如果找不到 -copy，退回 graph-*.txt。
    """
    files = sorted(glob.glob(os.path.join(data_dir, pattern)))
    if not files:
        print(f"[load] 未找到 {pattern}，尝试 graph-*.txt")
        files = sorted(glob.glob(os.path.join(data_dir, "graph-*.txt")))
        # 排除 -copy.txt（避免重复）
        files = [f for f in files if "-copy" not in f]
    print(f"[load] {data_dir}: {len(files)} 个文件")
    cycles = []
    for f in files:
        try:
            cycles.extend(parse_file(f))
        except Exception as e:
            print(f"[warn] 解析 {f} 失败: {e}")
    print(f"[load] 共 {len(cycles)} 个 CYCLE")
    return cycles

# 2. Feature table + structure hash + label
def build_feature_table(cycles):
    rows = []
    for c in cycles:
        f = c["features"]
        n_lock = len(c["lock_nodes"]); n_acq = len(c["acq_nodes"])
        n_le = len(c["lock_edges"]);   n_ce = len(c["call_edges"])
        n_cross = len(c["cross"])
        n_atl = sum(1 for x in c["cross"] if x[0] == "ACQ_TO_LOCK")
        n_lta = sum(1 for x in c["cross"] if x[0] == "LOCK_TO_ACQ")

        static_ratio = (sum(1 for n in c["lock_nodes"] if n["is_static"]) / n_lock
                        if n_lock else 0.0)
        nonnull_ratio = (sum(1 for n in c["lock_nodes"] if n["obj_nonnull"]) / n_lock
                         if n_lock else 0.0)

        dists = [e["dist"] for e in c["lock_edges"] if e["dist"] >= 0]
        avg_lock_dist = float(np.mean(dists)) if dists else -1.0
        alias_ratio = (sum(1 for e in c["lock_edges"] if e["may_alias"]) / n_le
                       if n_le else 0.0)
        same_obj_ratio = (sum(1 for e in c["lock_edges"] if e["same_lock_obj"]) / n_le
                          if n_le else 0.0)

        sync_ratio = (sum(1 for a in c["acq_nodes"] if a["is_sync"]) / n_acq
                      if n_acq else 0.0)
        acq_static_ratio = (sum(1 for a in c["acq_nodes"] if a["is_static"]) / n_acq
                            if n_acq else 0.0)
        avg_nesting = float(np.mean([a["nesting"] for a in c["acq_nodes"]])) if n_acq else 0.0
        avg_pts = float(np.mean([a["pts_size"] for a in c["acq_nodes"]])) if n_acq else 0.0

        call_dists = [e["dist"] for e in c["call_edges"]]
        avg_call_dist = float(np.mean(call_dists)) if call_dists else -1.0
        nested_ratio = (sum(1 for e in c["call_edges"] if e["nested"]) / n_ce
                        if n_ce else 0.0)

        struct_key = (
            n_lock, n_acq, n_le, n_ce, n_cross, n_atl, n_lta,
            round(static_ratio, 2), round(nonnull_ratio, 2),
            round(avg_lock_dist, 2), round(alias_ratio, 2),
            round(same_obj_ratio, 2), round(sync_ratio, 2),
            round(acq_static_ratio, 2), round(avg_nesting, 2),
            round(avg_pts, 2), round(avg_call_dist, 2), round(nested_ratio, 2),
        )
        struct_hash = hashlib.md5(str(struct_key).encode()).hexdigest()

        rows.append({
            "file": c["file"], "cycle_id": c["cycle_id"],
            "label_cycle": c["labels"].get("cycle", -1),
            "label_real":  c["labels"].get("real", -1),  # 4 values ground truth
            "struct_hash": struct_hash,
            "n_lock": n_lock, "n_acq": n_acq,
            "n_lock_edge": n_le, "n_call_edge": n_ce,
            "n_cross": n_cross, "n_cross_atl": n_atl, "n_cross_lta": n_lta,
            "static_ratio": static_ratio, "nonnull_ratio": nonnull_ratio,
            "avg_lock_dist": avg_lock_dist,
            "alias_ratio": alias_ratio, "same_obj_ratio": same_obj_ratio,
            "sync_ratio": sync_ratio, "acq_static_ratio": acq_static_ratio,
            "avg_nesting": avg_nesting, "avg_pts": avg_pts,
            "avg_call_dist": avg_call_dist, "nested_ratio": nested_ratio,
            "thread_count": int(f.get("thread_count", 0)),
            "max_thread_occurrence": int(f.get("max_thread_occurrence", 0)),
            "edge_count": int(f.get("edge_count", 0)),
            "all_edges_same_thread": int(f.get("all_edges_same_thread", "false") == "true"),
            "all_edges_reachable": int(f.get("all_edges_reachable", "false") == "true"),
            "has_non_adjacent_edge": int(f.get("has_non_adjacent_edge", "false") == "true"),
        })
    return pd.DataFrame(rows)


def apply_label_mode(df, label_mode):
    """
    Determine the binary/three-class `y` based on `--label_mode`.

    `label_real` semantics (4-value ground truth):
      2 = strong positive (≥2 real cycle entries)
      1 = weak positive (1 entry / ring helper / mock)
      0 = negative (fake cycle / native)
      -1 = unknown (discard)

    label_mode:
      binary_strong : y = (label_real == 2)  →  strict positive samples
      binary_loose  : y = (label_real >= 1)  →  loose positive samples
      three_way     : y = label_real (keep 0/1/2)
    """
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

# 3. Data quality report
def quality_report(df, out_dir):
    print("\n" + "=" * 30)
    print("数据质量评估")
    print("=" * 30)

    print("\n[label_real 原始分布]")
    print(df["label_real"].value_counts().sort_index())

    print("\n[y 分布 (after label_mode)]")
    print(df["y"].value_counts().sort_index())

    print(f"\n[结构重复] 唯一 struct_hash = {df['struct_hash'].nunique()} / 总样本 {len(df)}")

    print("\n[缺失值]")
    miss = df.isnull().sum()
    miss = miss[miss > 0]
    print(miss if len(miss) else "  无缺失")

    # Single-feature Mann-Whitney (for binary classification)
    if df["y"].nunique() == 2:
        print("\n[单特征判别性 (Mann-Whitney U)]")
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
                print(f"  ⚠ {c}: AUC={a:.3f}")
        else:
            print("No obvious leakage found.")

    os.makedirs(out_dir, exist_ok=True)
    df.to_csv(os.path.join(out_dir, "features.csv"), index=False)
    print(f"\n[save] {out_dir}/features.csv")

# 4. rule baseline
def rule_baseline(df):
    """
    Replicate `classifyCycle.isReal`:
      all_edges_same_thread && all_edges_reachable
      && thread_count >= 2 && max_thread_occurrence <= 1

    Note: this is the parser's rule and differs from the source of `LABEL_REAL`.
    """
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
        p  = precision_score(y, pred, zero_division=0)
        r  = recall_score(y, pred, zero_division=0)
        print(f"  Acc={acc:.4f}  AUC={auc:.4f}  F1={f1:.4f}  P={p:.4f}  R={r:.4f}")
        print(f"  预测正类比例={pred.mean():.4f}  真实正类比例={y.mean():.4f}")
        return {"acc": float(acc), "auc": float(auc),
                "f1": float(f1), "precision": float(p), "recall": float(r)}
    else:
        # Three-class classification: the rule outputs 0/1, and mapping to anything other than labels 0/2 counts as wrong.
        # If the rule judges it as true -> predict 2; otherwise predict 0.
        pred3 = np.where(pred == 1, 2, 0)
        acc = (pred3 == y).mean()
        f1m = f1_score(y, pred3, average="macro", zero_division=0)
        print(f"  [三分类] Acc={acc:.4f}  macro-F1={f1m:.4f}")
        print(f"  规则只能输出 0/2，无法区分 label 1")
        return {"acc": float(acc), "f1_macro": float(f1m)}

# 5. Unified K-Fold split (shared by the three models)
def make_folds(df, n_splits=5):
    """
    Group by {struct_hash} and perform GroupKFold, returning [(tr_idx, te_idx), ...].
    """
    groups = df["struct_hash"].values
    y = df["y"].values
    n_groups = len(np.unique(groups))
    n_splits = min(n_splits, n_groups)
    if n_splits < 2:
        raise ValueError(f"唯一结构数太少 ({n_groups})")

    gkf = GroupKFold(n_splits=n_splits)
    return list(gkf.split(np.zeros(len(df)), y, groups))

# 6. random forest
def eval_rf_on_folds(df, feat_cols, folds, tag="rf", do_grid=False, out_dir=None):
    """
    在给定的 fold 划分上评估 RF。返回 (mean_metrics, oof_prob, oof_pred, model)
    """
    X = df[feat_cols].fillna(0).values.astype(np.float32)
    y = df["y"].values.astype(int)
    is_binary = df["y"].nunique() == 2

    # ---- 可选：GridSearch 找最佳超参 ----
    best_params = None
    if do_grid:
        print(f"\n  [GridSearch] ...")
        param_grid = {
            "n_estimators": [200, 300, 500],
            "max_depth": [3, 5, 7, None],
            "min_samples_leaf": [1, 2, 4],
            "class_weight": ["balanced", "balanced_subsample"],
        }
        # Use the training set of the first fold for GridSearch.
        tr0, _ = folds[0]
        rf = RandomForestClassifier(random_state=RANDOM_SEED, n_jobs=-1)
        scoring = "roc_auc" if is_binary else "f1_macro"
        gs = GridSearchCV(rf, param_grid, cv=3, scoring=scoring,
                          n_jobs=-1, verbose=0)
        gs.fit(X[tr0], y[tr0])
        best_params = gs.best_params_
        print(f"  [GridSearch] best={best_params}, score={gs.best_score_:.4f}")

    # evaluate fold by fold
    oof_prob = np.zeros(len(y))
    oof_pred = np.zeros(len(y), dtype=int)
    fold_metrics = []

    for k, (tr, te) in enumerate(folds):
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

    # summarize
    if is_binary:
        mean_metrics = {
            "auc": float(np.mean([m["auc"] for m in fold_metrics])),
            "auc_std": float(np.std([m["auc"] for m in fold_metrics])),
            "f1": float(np.mean([m["f1"] for m in fold_metrics])),
            "precision": float(np.mean([m["p"] for m in fold_metrics])),
            "recall": float(np.mean([m["r"] for m in fold_metrics])),
            "acc": float(np.mean([m["acc"] for m in fold_metrics])),
        }
        try:
            mean_metrics["oof_auc"] = float(roc_auc_score(y, oof_prob))
        except Exception:
            mean_metrics["oof_auc"] = float("nan")
    else:
        mean_metrics = {
            "f1_macro": float(np.mean([m["f1_macro"] for m in fold_metrics])),
            "acc": float(np.mean([m["acc"] for m in fold_metrics])),
        }

    # Feature importance & saving
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

    return mean_metrics, oof_prob, oof_pred

# 7. Heterogeneous GNN
def cycle_to_hetero(c, drop_lock_edge=False, drop_call_edge=False, drop_cross=False):
    data = HeteroData()
    n_lock = len(c["lock_nodes"]); n_acq = len(c["acq_nodes"])

    # --- lock 节点特征 ---
    if n_lock > 0:
        lf = [[float(n["is_static"]), float(n["obj_nonnull"]),
               1.0 if "LockBox" in n["obj_key"] else 0.0,
               1.0 if "Object" in n["obj_type"] else 0.0,
               float(len(n["obj_key"]))] for n in c["lock_nodes"]]
        data["lock"].x = torch.tensor(lf, dtype=torch.float)
    else:
        data["lock"].x = torch.zeros((0, LOCK_FEAT_DIM), dtype=torch.float)

    # --- acq 节点特征 ---
    if n_acq > 0:
        af = [[float(a["line"] if a["line"] >= 0 else 0),
               float(a["is_static"]), float(a["is_sync"]),
               float(a["nesting"]), float(a["pts_size"]),
               float(len(a["var"]))] for a in c["acq_nodes"]]
        data["acq"].x = torch.tensor(af, dtype=torch.float)
    else:
        data["acq"].x = torch.zeros((0, ACQ_FEAT_DIM), dtype=torch.float)

    # --- lock_edge（带 edge_attr：dist / may_alias / same_lock_obj） ---
    if c["lock_edges"] and not drop_lock_edge:
        le = torch.tensor([[e["from"] for e in c["lock_edges"]],
                           [e["to"] for e in c["lock_edges"]]], dtype=torch.long)
        le_attr = torch.tensor(
            [[float(e["dist"]),
              float(e["may_alias"]),
              float(e["same_lock_obj"])] for e in c["lock_edges"]],
            dtype=torch.float)
    else:
        le = torch.zeros((2, 0), dtype=torch.long)
        le_attr = torch.zeros((0, LOCK_EDGE_FEAT_DIM), dtype=torch.float)
    data["lock", "lock_edge", "lock"].edge_index = le
    data["lock", "lock_edge", "lock"].edge_attr = le_attr

    # --- call_edge（带 edge_attr：dist / same_method / nested） ---
    if c["call_edges"] and not drop_call_edge:
        ce = torch.tensor([[e["src"] for e in c["call_edges"]],
                           [e["dst"] for e in c["call_edges"]]], dtype=torch.long)
        ce_attr = torch.tensor(
            [[float(e["dist"]),
              float(e["same_method"]),
              float(e["nested"])] for e in c["call_edges"]],
            dtype=torch.float)
    else:
        ce = torch.zeros((2, 0), dtype=torch.long)
        ce_attr = torch.zeros((0, CALL_EDGE_FEAT_DIM), dtype=torch.float)
    data["acq", "call_edge", "acq"].edge_index = ce
    data["acq", "call_edge", "acq"].edge_attr = ce_attr

    # --- 跨层边（无 edge_attr，用零维占位） ---
    if drop_cross:
        atl, lta = [], []
    else:
        atl = [(a, b) for (t, a, b) in c["cross"] if t == "ACQ_TO_LOCK"]
        lta = [(a, b) for (t, a, b) in c["cross"] if t == "LOCK_TO_ACQ"]

    data["acq", "acq_to_lock", "lock"].edge_index = (
        torch.tensor([[a for a, b in atl], [b for a, b in atl]], dtype=torch.long)
        if atl else torch.zeros((2, 0), dtype=torch.long))
    data["lock", "lock_to_acq", "acq"].edge_index = (
        torch.tensor([[a for a, b in lta], [b for a, b in lta]], dtype=torch.long)
        if lta else torch.zeros((2, 0), dtype=torch.long))

    data.y = torch.tensor([c["labels"].get("real", 0)], dtype=torch.long)
    return data


def collate_hetero(batch):
    return Batch.from_data_list(batch)


class HeteroGNN(torch.nn.Module):
    """
    Use `GINEConv` instead of `SAGEConv` to support `edge_attr`.
    `GINEConv` requires an `nn` module as the edge encoder to project `edge_attr` into the hidden dimension
    """
    def __init__(self, hidden=64, num_classes=2, num_layers=2, dropout=0.3):
        super().__init__()
        self.lin_lock = Linear(LOCK_FEAT_DIM, hidden)
        self.lin_acq  = Linear(ACQ_FEAT_DIM, hidden)
        self.hidden = hidden

        # Edge encoder: map edge features to hidden (required by GINEConv)
        self.edge_encoder_lock = torch.nn.Linear(LOCK_EDGE_FEAT_DIM, hidden)
        self.edge_encoder_call = torch.nn.Linear(CALL_EDGE_FEAT_DIM, hidden)

        # GINEConv's inner MLP (in the paper it is ReLU + Linear).
        def make_nn():
            return torch.nn.Sequential(
                torch.nn.Linear(hidden, hidden),
                torch.nn.ReLU(),
                torch.nn.Linear(hidden, hidden),
            )

        self.convs = torch.nn.ModuleList()
        for _ in range(num_layers):
            conv = HeteroConv({
                ("lock", "lock_edge", "lock"):  GINEConv(make_nn(), edge_dim=hidden),
                ("acq", "call_edge", "acq"):    GINEConv(make_nn(), edge_dim=hidden),
                ("acq", "acq_to_lock", "lock"): GINEConv(make_nn(), edge_dim=hidden),
                ("lock", "lock_to_acq", "acq"): GINEConv(make_nn(), edge_dim=hidden),
            }, aggr="sum")
            self.convs.append(conv)

        self.dropout = dropout
        self.classifier = torch.nn.Sequential(
            torch.nn.Linear(hidden * 2, hidden),
            torch.nn.ReLU(),
            torch.nn.Dropout(dropout),
            torch.nn.Linear(hidden, num_classes),
        )

    def _make_edge_attr(self, kind, data, device):
        """
        Return a dict of `edge_attr` with shapes aligned to `edge_index`.
        For cross-layer edges (which have no `edge_attr`), use a zero vector as a placeholder
        """
        if kind == "lock":
            n = data["lock", "lock_edge", "lock"].edge_index.size(1)
            if hasattr(data["lock", "lock_edge", "lock"], "edge_attr") \
               and data["lock", "lock_edge", "lock"].edge_attr.numel() > 0:
                return self.edge_encoder_lock(
                    data["lock", "lock_edge", "lock"].edge_attr)
            return torch.zeros(n, self.hidden, device=device)
        elif kind == "call":
            n = data["acq", "call_edge", "acq"].edge_index.size(1)
            if hasattr(data["acq", "call_edge", "acq"], "edge_attr") \
               and data["acq", "call_edge", "acq"].edge_attr.numel() > 0:
                return self.edge_encoder_call(
                    data["acq", "call_edge", "acq"].edge_attr)
            return torch.zeros(n, self.hidden, device=device)
        else:  # cross
            # Use the corresponding number of src nodes as a placeholder (cross-layer edges have no attr, so directly 0)
            if kind == "atl":
                n = data["acq", "acq_to_lock", "lock"].edge_index.size(1)
            else:
                n = data["lock", "lock_to_acq", "acq"].edge_index.size(1)
            return torch.zeros(n, self.hidden, device=device)

    def forward(self, data):
        x_dict = {
            "lock": F.relu(self.lin_lock(data["lock"].x)),
            "acq":  F.relu(self.lin_acq(data["acq"].x)),
        }
        edge_index_dict = {
            ("lock", "lock_edge", "lock"):  data["lock", "lock_edge", "lock"].edge_index,
            ("acq", "call_edge", "acq"):    data["acq", "call_edge", "acq"].edge_index,
            ("acq", "acq_to_lock", "lock"): data["acq", "acq_to_lock", "lock"].edge_index,
            ("lock", "lock_to_acq", "acq"): data["lock", "lock_to_acq", "acq"].edge_index,
        }
        device = x_dict["lock"].device if x_dict["lock"].size(0) > 0 else x_dict["acq"].device

        # GINEConv 要求 edge_attr 与 edge_index 维度一致
        edge_attr_dict = {
            ("lock", "lock_edge", "lock"):  self._make_edge_attr("lock", data, device),
            ("acq", "call_edge", "acq"):    self._make_edge_attr("call", data, device),
            ("acq", "acq_to_lock", "lock"): self._make_edge_attr("atl", data, device),
            ("lock", "lock_to_acq", "acq"): self._make_edge_attr("lta", data, device),
        }

        for conv in self.convs:
            x_dict = conv(x_dict, edge_index_dict, edge_attr_dict)
            x_dict = {k: F.relu(v) for k, v in x_dict.items()}
            x_dict = {k: F.dropout(v, p=self.dropout, training=self.training)
                      for k, v in x_dict.items()}

        num_graphs = data.y.size(0)
        pooled = []
        for k in ["lock", "acq"]:
            if x_dict[k].size(0) > 0 and hasattr(data[k], "batch"):
                pooled.append(global_mean_pool(x_dict[k], data[k].batch))
            else:
                pooled.append(torch.zeros(num_graphs, self.hidden, device=device))
        g = torch.cat(pooled, dim=-1)
        return self.classifier(g)


def train_one_fold_gnn(train_ds, test_ds, num_classes=2, epochs=100, hidden=64,
                       num_layers=2, dropout=0.3, lr=1e-3, patience=15):
    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    model = HeteroGNN(hidden=hidden, num_classes=num_classes,
                      num_layers=num_layers, dropout=dropout).to(device)
    opt = torch.optim.Adam(model.parameters(), lr=lr, weight_decay=1e-4)

    labels_tr = np.array([d.y.item() for d in train_ds])
    cnt = Counter(labels_tr)
    # sqrt 温和加权（避免过激）
    w = torch.tensor([1.0 / np.sqrt(max(cnt.get(i, 1), 1)) for i in range(num_classes)],
                     dtype=torch.float, device=device)
    w = w / w.sum() * num_classes
    criterion = torch.nn.CrossEntropyLoss(weight=w)

    train_loader = DataLoader(train_ds, batch_size=16, shuffle=True, collate_fn=collate_hetero)
    test_loader  = DataLoader(test_ds, batch_size=16, shuffle=False, collate_fn=collate_hetero)

    best_loss = float("inf")
    best_state = None
    no_improve = 0

    for epoch in range(1, epochs + 1):
        model.train()
        epoch_loss, n_seen = 0.0, 0
        for batch in train_loader:
            batch = batch.to(device)
            opt.zero_grad()
            out = model(batch)
            loss = criterion(out, batch.y.view(-1))
            loss.backward()
            opt.step()
            epoch_loss += loss.item() * batch.y.size(0)
            n_seen += batch.y.size(0)
        epoch_loss /= max(n_seen, 1)

        # 用训练 loss 选 best epoch（不碰 test）
        if epoch_loss < best_loss - 1e-5:
            best_loss = epoch_loss
            best_state = {k: v.detach().cpu().clone() for k, v in model.state_dict().items()}
            no_improve = 0
        else:
            no_improve += 1
            if no_improve >= patience:
                break

    # Restore the model using {best_state}, and run it once on test.
    if best_state is not None:
        model.load_state_dict({k: v.to(device) for k, v in best_state.items()})

    model.eval()
    all_prob, all_y = [], []
    with torch.no_grad():
        for batch in test_loader:
            batch = batch.to(device)
            out = model(batch)
            prob = F.softmax(out, dim=-1)
            all_prob.append(prob.cpu().numpy())
            all_y.append(batch.y.view(-1).cpu().numpy())
    all_prob = np.concatenate(all_prob, axis=0) if all_prob else None
    all_y = np.concatenate(all_y) if all_y else None
    return best_loss, all_prob, all_y


def eval_gnn_on_folds(cycles, df, folds, num_classes=2, epochs=100, hidden=64,
                      num_layers=2, dropout=0.3, lr=1e-3,
                      drop_lock_edge=False, drop_call_edge=False, drop_cross=False,
                      tag="gnn"):
    """
    Evaluate the GNN on the given fold split
    """
    valid_mask = df["label_real"].isin([0, 1, 2]).values
    valid_cycles = [c for c, m in zip(cycles, valid_mask) if m]
    data_list = [cycle_to_hetero(c, drop_lock_edge=drop_lock_edge,
                                 drop_call_edge=drop_call_edge,
                                 drop_cross=drop_cross) for c in valid_cycles]
    labels = np.array([d.y.item() for d in data_list])

    # In three-class classification, label 2 -> y=2; in binary classification, label 2 -> y=1.
    if num_classes == 2:
        df_eff = df[valid_mask].copy()
        y = df_eff["y"].values.astype(int)
        for i, d in enumerate(data_list):
            d.y = torch.tensor([int(y[i])], dtype=torch.long)
    else:
        for i, d in enumerate(data_list):
            d.y = torch.tensor([int(labels[i])], dtype=torch.long)

    fold_metrics = []
    oof_prob = np.zeros((len(data_list), num_classes)) if num_classes > 1 else np.zeros(len(data_list))
    oof_pred = np.zeros(len(data_list), dtype=int)

    for k, (tr, te) in enumerate(folds):
        tr_ds = [data_list[i] for i in tr]
        te_ds = [data_list[i] for i in te]
        y_te = np.array([d.y.item() for d in te_ds])
        if len(np.unique(y_te)) < 2 and num_classes == 2:
            print(f"  fold {k}: 测试集只有一类，跳过")
            continue

        _, prob, _ = train_one_fold_gnn(
            tr_ds, te_ds, num_classes=num_classes,
            epochs=epochs, hidden=hidden, num_layers=num_layers,
            dropout=dropout, lr=lr)
        if prob is None:
            print(f"  fold {k}: 训练失败")
            continue
        oof_prob[te] = prob
        pred = prob.argmax(axis=1)
        oof_pred[te] = pred

        if num_classes == 2:
            try:
                auc = roc_auc_score(y_te, prob[:, 1])
            except Exception:
                auc = 0.5
            m = {
                "auc": float(auc),
                "f1": float(f1_score(y_te, pred, zero_division=0)),
                "acc": float(accuracy_score(y_te, pred)),
            }
        else:
            m = {
                "f1_macro": float(f1_score(y_te, pred, average="macro", zero_division=0)),
                "acc": float(accuracy_score(y_te, pred)),
            }
        fold_metrics.append(m)
        print(f"  fold {k}: {m}")

    if not fold_metrics:
        return None, oof_prob, oof_pred

    if num_classes == 2:
        mean_metrics = {
            "auc": float(np.mean([m["auc"] for m in fold_metrics])),
            "auc_std": float(np.std([m["auc"] for m in fold_metrics])),
            "f1": float(np.mean([m["f1"] for m in fold_metrics])),
            "acc": float(np.mean([m["acc"] for m in fold_metrics])),
        }
    else:
        mean_metrics = {
            "f1_macro": float(np.mean([m["f1_macro"] for m in fold_metrics])),
            "acc": float(np.mean([m["acc"] for m in fold_metrics])),
        }
    return mean_metrics, oof_prob, oof_pred

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--data_dir", default="train")
    ap.add_argument("--test_dir", default="test")
    ap.add_argument("--out_dir", default="./output")
    ap.add_argument("--label_mode", default="binary_strong",
                    choices=["binary_strong", "binary_loose", "three_way"],
                    help="标签模式：strong=只在 L2 上训练；loose=L2+L1 都算正；three_way=L0/L1/L2")
    ap.add_argument("--n_splits", type=int, default=5)
    ap.add_argument("--gnn_epochs", type=int, default=100)
    ap.add_argument("--gnn_hidden", type=int, default=64)
    ap.add_argument("--gnn_layers", type=int, default=2)
    ap.add_argument("--gnn_lr", type=float, default=1e-3)
    ap.add_argument("--gnn_dropout", type=float, default=0.3)
    ap.add_argument("--do_rf", action="store_true", default=True)
    ap.add_argument("--do_gnn", action="store_true", default=True)
    ap.add_argument("--do_grid", action="store_true")
    ap.add_argument("--do_ablation", action="store_true")
    args = ap.parse_args()

    os.makedirs(args.out_dir, exist_ok=True)

    print("=" * 70)
    print("evaluate_all.py — Unified comparison of rule-based / random forest / heterogeneous GNN.")
    print(f"  data_dir   = {args.data_dir}")
    print(f"  test_dir   = {args.test_dir}")
    print(f"  out_dir    = {args.out_dir}")
    print(f"  label_mode = {args.label_mode}")
    print(f"  n_splits   = {args.n_splits}")
    print(f"  gnn        = hidden={args.gnn_hidden}, layers={args.gnn_layers}, "
          f"lr={args.gnn_lr}, dropout={args.gnn_dropout}, epochs={args.gnn_epochs}")
    print("=" * 70)

    # 1) Load train + test (merged, no split)
    cycles_train = load_all(args.data_dir)
    cycles_test  = load_all(args.test_dir)
    cycles = cycles_train + cycles_test
    if not cycles:
        print("[ERROR] 无数据")
        return

    df = build_feature_table(cycles)
    df = apply_label_mode(df, args.label_mode)
    print(f"\n[data] 总样本={len(df)}  唯一结构={df['struct_hash'].nunique()}")
    print(f"[data] y 分布 = {df['y'].value_counts().to_dict()}")

    quality_report(df, args.out_dir)

    # 2) Unified fold split (shared by the three models)
    folds = make_folds(df, n_splits=args.n_splits)
    print(f"\n[fold] GroupKFold n_splits={len(folds)}  (train+test 合并后按 struct_hash 分组)")

    # 3) rule baseline
    print("\n" + "=" * 70)
    print("① 规则 baseline")
    print("=" * 70)
    rule_res = rule_baseline(df)

    # 4) random forest
    print("\n" + "=" * 70)
    print("② Random Forest")
    print("=" * 70)
    rf_res = None
    if args.do_rf:
        rf_res, rf_oof_prob, rf_oof_pred = eval_rf_on_folds(
            df, GRAPH_FEATS, folds, tag="rf",
            do_grid=args.do_grid, out_dir=args.out_dir)
        print(f"\n  [RF GroupCV 平均] {rf_res}")

    # 5) Heterogeneous GNN
    print("\n" + "=" * 70)
    print("③ Heterogeneous GNN")
    print("=" * 70)
    gnn_res = None
    if args.do_gnn:
        num_classes = 2 if args.label_mode.startswith("binary") else 3
        gnn_res, _, _ = eval_gnn_on_folds(
            cycles, df, folds, num_classes=num_classes,
            epochs=args.gnn_epochs, hidden=args.gnn_hidden,
            num_layers=args.gnn_layers, dropout=args.gnn_dropout,
            lr=args.gnn_lr, tag="gnn_full")
        if gnn_res:
            print(f"\n  [GNN GroupCV 平均] {gnn_res}")

    # 6) Ablation study
    ablation_res = {}
    if args.do_ablation:
        print("\n" + "=" * 70)
        print("④ Ablation study (GNN)")
        print("=" * 70)
        num_classes = 2 if args.label_mode.startswith("binary") else 3
        for name, kwargs in [
            ("full",         {}),
            ("no_cross",     dict(drop_cross=True)),
            ("no_lock_edge", dict(drop_lock_edge=True)),
            ("no_call_edge", dict(drop_call_edge=True)),
        ]:
            res, _, _ = eval_gnn_on_folds(
                cycles, df, folds, num_classes=num_classes,
                epochs=args.gnn_epochs, hidden=args.gnn_hidden,
                num_layers=args.gnn_layers, dropout=args.gnn_dropout,
                lr=args.gnn_lr, tag=f"gnn_{name}",
                **kwargs)
            ablation_res[name] = res
            print(f"  [{name:15s}] {res}")

    # 7) Summary
    print("\n" + "=" * 70)
    print("最终对比 (GroupKFold by struct_hash)")
    print("=" * 70)
    is_binary = args.label_mode.startswith("binary")

    rows = []
    if rule_res:
        rows.append(("Rule baseline", rule_res))
    if rf_res:
        rows.append(("Random Forest", rf_res))
    if gnn_res:
        rows.append(("HeteroGNN", gnn_res))

    if is_binary:
        print(f"  {'Model':20s} {'AUC':>8s} {'F1':>8s} {'Acc':>8s}")
        for name, m in rows:
            print(f"  {name:20s} {m.get('auc', float('nan')):8.4f} "
                  f"{m.get('f1', float('nan')):8.4f} "
                  f"{m.get('acc', float('nan')):8.4f}")
    else:
        print(f"  {'Model':20s} {'macroF1':>10s} {'Acc':>10s}")
        for name, m in rows:
            print(f"  {name:20s} {m.get('f1_macro', float('nan')):10.4f} "
                  f"{m.get('acc', float('nan')):10.4f}")

    # 8) Saving
    summary = {
        "config": vars(args),
        "n_samples": len(df),
        "n_structures": int(df["struct_hash"].nunique()),
        "y_distribution": df["y"].value_counts().to_dict(),
        "rule_baseline": rule_res,
        "random_forest": rf_res,
        "hetero_gnn": gnn_res,
        "ablation": ablation_res,
    }
    with open(os.path.join(args.out_dir, "summary.json"), "w") as f:
        json.dump(summary, f, indent=2, default=str)
    print(f"\n[save] {args.out_dir}/summary.json")


if __name__ == "__main__":
    main()
