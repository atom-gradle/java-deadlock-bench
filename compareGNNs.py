# -*- coding: utf-8 -*-
import argparse, glob, hashlib, json, os, re, sys, time, warnings
from collections import Counter
import numpy as np
import pandas as pd
import torch
import torch.nn.functional as F
from scipy.stats import t, ttest_rel, wilcoxon
from sklearn.metrics import (
    accuracy_score,
    f1_score,
    precision_score,
    recall_score,
    roc_auc_score,
)
from sklearn.model_selection import GroupKFold
from torch_geometric.data import Batch, HeteroData
from torch_geometric.loader import DataLoader
from torch_geometric.nn import (
    GATConv,
    GCNConv,
    GINConv,
    HeteroConv,
    Linear,
    SAGEConv,
    global_mean_pool,
)

warnings.filterwarnings("ignore")

RANDOM_SEED = 42
np.random.seed(RANDOM_SEED)
torch.manual_seed(RANDOM_SEED)

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

ET_LOCK = ("lock", "lock_edge", "lock")
ET_CALL = ("acq", "call_edge", "acq")
ET_ATL = ("acq", "acq_to_lock", "lock")
ET_LTA = ("lock", "lock_to_acq", "acq")

HOMOGENEOUS_EDGE_TYPES = {ET_LOCK, ET_CALL}
HETEROGENEOUS_EDGE_TYPES = {ET_ATL, ET_LTA}


def _needs_self_loops(edge_type):
    return edge_type in HOMOGENEOUS_EDGE_TYPES

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
            key_part, obj_type, is_static, obj_nonnull = rest.rsplit(",", 3)
            nid, obj_key = key_part.split(",", 1)
            cur["lock_nodes"].append({
                "id": int(nid), "obj_key": obj_key, "obj_type": obj_type,
                "is_static": is_static.strip().lower() == "true",
                "obj_nonnull": int(obj_nonnull.strip()),
            })
        elif line.startswith("LOCK_EDGE"):
            _, rest = line.split(None, 1)
            p = rest.split(",")
            cur["lock_edges"].append({
                "from": int(p[0]), "to": int(p[1]), "from_method": p[2],
                "from_line": int(p[3]), "to_method": p[4], "to_line": int(p[5]),
                "dist": int(p[6]), "may_alias": p[7].strip().lower() == "true",
                "same_lock_obj": p[8].strip().lower() == "true",
            })
        elif line.startswith("ACQ_NODE"):
            _, rest = line.split(None, 1)
            p = rest.split(",", 7)
            cur["acq_nodes"].append({
                "id": int(p[0]), "method": p[1], "line": int(p[2]), "var": p[3],
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
            cur["labels"]["real"] = int(line.split()[1])
    if cur is not None:
        cycles.append(cur)
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
        except Exception:
            pass
    return cycles


def build_feature_table(cycles):
    rows = []
    for c in cycles:
        f = c["features"]
        n_lock = len(c["lock_nodes"])
        n_acq = len(c["acq_nodes"])
        n_le = len(c["lock_edges"])
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

        struct_key = (
            n_lock, n_acq, n_le, n_ce, n_cross, n_atl, n_lta,
            round(static_ratio, 2), round(nonnull_ratio, 2), round(avg_lock_dist, 2),
            round(alias_ratio, 2), round(same_obj_ratio, 2), round(sync_ratio, 2),
            round(acq_static_ratio, 2), round(avg_nesting, 2), round(avg_pts, 2),
            round(avg_call_dist, 2), round(nested_ratio, 2),
        )
        struct_hash = hashlib.md5(str(struct_key).encode()).hexdigest()

        rows.append({
            "file": c["file"], "cycle_id": c["cycle_id"],
            "label_cycle": c["labels"].get("cycle", -1),
            "label_real": c["labels"].get("real", -1),
            "struct_hash": struct_hash,
            "n_lock": n_lock, "n_acq": n_acq, "n_lock_edge": n_le,
            "n_call_edge": n_ce, "n_cross": n_cross,
            "n_cross_atl": n_atl, "n_cross_lta": n_lta,
            "static_ratio": static_ratio, "nonnull_ratio": nonnull_ratio,
            "avg_lock_dist": avg_lock_dist, "alias_ratio": alias_ratio,
            "same_obj_ratio": same_obj_ratio, "sync_ratio": sync_ratio,
            "acq_static_ratio": acq_static_ratio, "avg_nesting": avg_nesting,
            "avg_pts": avg_pts, "avg_call_dist": avg_call_dist,
            "nested_ratio": nested_ratio,
            "thread_count": int(f.get("thread_count", 0)),
            "max_thread_occurrence": int(f.get("max_thread_occurrence", 0)),
            "edge_count": int(f.get("edge_count", 0)),
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
        raise ValueError(f"Too few unique structures ({n_groups})")
    gkf = GroupKFold(n_splits=n_splits)
    return list(gkf.split(np.zeros(len(df)), y, groups))

def cycle_to_hetero(c):
    data = HeteroData()
    n_lock = len(c["lock_nodes"])
    n_acq = len(c["acq_nodes"])
    n_le = len(c["lock_edges"])
    n_ce = len(c["call_edges"])
    n_cross = len(c["cross"])

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
        static_ratio, nonnull_ratio, avg_lock_dist, alias_ratio,
        same_obj_ratio, sync_ratio, acq_static_ratio, avg_nesting,
        avg_pts, avg_call_dist, nested_ratio,
    ]
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
                float(n["is_static"]), float(n["obj_nonnull"]),
                float(lock_in.get(nid, 0)), float(lock_out.get(nid, 0)),
                float(lock_in.get(nid, 0) + lock_out.get(nid, 0)),
            ]
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
                float(a["is_static"]), float(a["is_sync"]),
                float(a["nesting"]), float(a["pts_size"]),
                float(acq_in.get(aid, 0) + acq_out.get(aid, 0)),
            ]
            af.append(feat)
        data["acq"].x = torch.tensor(af, dtype=torch.float)
    else:
        data["acq"].x = torch.zeros((0, ACQ_FEAT_DIM), dtype=torch.float)

    if c["lock_edges"]:
        data["lock", "lock_edge", "lock"].edge_index = torch.tensor(
            [[e["from"] for e in c["lock_edges"]],
             [e["to"] for e in c["lock_edges"]]], dtype=torch.long)
    else:
        data["lock", "lock_edge", "lock"].edge_index = torch.zeros((2, 0), dtype=torch.long)

    if c["call_edges"]:
        data["acq", "call_edge", "acq"].edge_index = torch.tensor(
            [[e["src"] for e in c["call_edges"]],
             [e["dst"] for e in c["call_edges"]]], dtype=torch.long)
    else:
        data["acq", "call_edge", "acq"].edge_index = torch.zeros((2, 0), dtype=torch.long)

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

def _normalize(x: torch.Tensor, key: str):
    st = FEAT_STATS.get(key)
    if st is None or st["mean"] is None:
        return x
    mean = torch.as_tensor(st["mean"], dtype=x.dtype, device=x.device)
    std = torch.as_tensor(st["std"], dtype=x.dtype, device=x.device)
    return (x - mean) / std.clamp_min(1e-6)


def compute_feat_stats(data_list):
    for key, _ in [("lock", LOCK_FEAT_DIM), ("acq", ACQ_FEAT_DIM)]:
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


def reset_feat_stats():
    for k in FEAT_STATS:
        FEAT_STATS[k]["mean"] = None
        FEAT_STATS[k]["std"] = None


def collate_hetero(batch):
    return Batch.from_data_list(batch)


def make_gcn_conv(in_ch, out_ch, add_self_loops=True):
    return GCNConv(in_ch, out_ch, add_self_loops=add_self_loops, normalize=True)


def make_sage_conv(in_ch, out_ch, add_self_loops=True):
    return SAGEConv((in_ch, in_ch), out_ch, aggr="mean")


def make_gat_conv(in_ch, out_ch, add_self_loops=True, heads=4):
    if out_ch % heads != 0:
        heads = 1
    return GATConv(in_ch, out_ch // heads, heads=heads, concat=True,
                   add_self_loops=add_self_loops, dropout=0.0)


def make_gin_conv(in_ch, out_ch, add_self_loops=True):
    nn = torch.nn.Sequential(
        torch.nn.Linear(in_ch, out_ch),
        torch.nn.ReLU(),
        torch.nn.Linear(out_ch, out_ch),
    )
    return GINConv(nn, train_eps=True)


CONV_FACTORY = {
    "gcn": make_gcn_conv,
    "sage": make_sage_conv,
    "gat": make_gat_conv,
    "gin": make_gin_conv,
}

class HeteroGNNBase(torch.nn.Module):
    conv_kind = "sage"

    def __init__(self, hidden=32, num_classes=2, num_layers=1, dropout=0.3):
        super().__init__()
        self.hidden = hidden
        self.num_layers = num_layers

        self.lin_lock = Linear(LOCK_FEAT_DIM, hidden)
        self.lin_acq = Linear(ACQ_FEAT_DIM, hidden)
        self.lin_global = Linear(GLOBAL_FEAT_DIM, hidden)

        factory = CONV_FACTORY[self.conv_kind]
        self.convs = torch.nn.ModuleList()
        for _ in range(num_layers):
            conv_dict = {}
            for et in (ET_LOCK, ET_CALL, ET_ATL, ET_LTA):
                if et in HETEROGENEOUS_EDGE_TYPES:
                    conv_dict[et] = make_sage_conv(hidden, hidden)
                else:
                    conv_dict[et] = factory(
                        hidden, hidden,
                        add_self_loops=_needs_self_loops(et),
                    )
            self.convs.append(HeteroConv(conv_dict, aggr="sum"))

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
            ET_LOCK: data[ET_LOCK].edge_index,
            ET_CALL: data[ET_CALL].edge_index,
            ET_ATL: data[ET_ATL].edge_index,
            ET_LTA: data[ET_LTA].edge_index,
        }

        for conv in self.convs:
            x_dict = conv(x_dict, edge_index_dict)
            x_dict = {k: F.relu(v) for k, v in x_dict.items()}
            x_dict = {k: F.dropout(v, p=self.dropout, training=self.training)
                      for k, v in x_dict.items()}

        num_graphs = data.y.size(0)
        pooled = []
        for k in ["lock", "acq"]:
            if x_dict[k].size(0) > 0 and hasattr(data[k], "batch"):
                pooled.append(global_mean_pool(x_dict[k], data[k].batch))
            else:
                pooled.append(torch.zeros(num_graphs, self.hidden,
                                          device=data.y.device))

        g_pool = torch.cat(pooled, dim=-1)
        g_glob = F.relu(self.lin_global(glob_x))
        g = torch.cat([g_pool, g_glob], dim=-1)
        g = self.norm(g)
        return self.classifier(g)


class GCNGNN(HeteroGNNBase):
    conv_kind = "gcn"


class SAGEGNN(HeteroGNNBase):
    conv_kind = "sage"


class GATGNN(HeteroGNNBase):
    conv_kind = "gat"


class GINGNN(HeteroGNNBase):
    conv_kind = "gin"


MODEL_REGISTRY = {
    "gcn": ("GCN", GCNGNN),
    "sage": ("GraphSAGE", SAGEGNN),
    "gat": ("GAT", GATGNN),
    "gin": ("GIN", GINGNN),
}


def _split_train_val(train_ds, val_ratio=0.15, seed=RANDOM_SEED):
    n = len(train_ds)
    if n < 5:
        return train_ds, train_ds[:0]
    rng = np.random.RandomState(seed)
    idx = np.arange(n)
    rng.shuffle(idx)
    n_val = max(1, int(n * val_ratio))
    return [train_ds[i] for i in idx[n_val:]], [train_ds[i] for i in idx[:n_val]]


def train_and_eval_one(train_ds, test_ds, num_classes=2, epochs=100, hidden=32,
                       num_layers=1, dropout=0.3, lr=1e-3, patience=15,
                       model_cls=SAGEGNN, fold_seed=None,
                       reset_stats=True):
    if fold_seed is None:
        fold_seed = RANDOM_SEED
    torch.manual_seed(fold_seed)
    np.random.seed(fold_seed)

    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    if reset_stats:
        reset_feat_stats()
    compute_feat_stats(train_ds)

    model = model_cls(hidden=hidden, num_classes=num_classes,
                      num_layers=num_layers, dropout=dropout).to(device)
    opt = torch.optim.Adam(model.parameters(), lr=lr, weight_decay=1e-3)
    criterion = torch.nn.CrossEntropyLoss()

    tr_ds, val_ds = _split_train_val(train_ds, val_ratio=0.15, seed=RANDOM_SEED)
    train_loader = DataLoader(tr_ds, batch_size=16, shuffle=True,
                              collate_fn=collate_hetero, num_workers=0)
    val_loader = DataLoader(val_ds, batch_size=16, shuffle=False,
                            collate_fn=collate_hetero) if val_ds else None

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

        if val_loader is not None:
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
                if num_classes == 2:
                    score = roc_auc_score(all_y, all_prob[:, 1])
                else:
                    score = f1_score(all_y, all_prob.argmax(axis=1),
                                     average="macro", zero_division=0)
            else:
                score = 0
        else:
            score = -epoch

        if score > best_score + 1e-5:
            best_score = score
            best_state = {k: v.detach().cpu().clone()
                          for k, v in model.state_dict().items()}
            no_improve = 0
        else:
            no_improve += 1
            if no_improve >= patience:
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
    return (best_score,
            np.concatenate(all_prob) if all_prob else None,
            np.concatenate(all_y) if all_y else None)


def mean_ci95(values):
    arr = np.asarray(values, dtype=float)
    arr = arr[~np.isnan(arr)]
    if len(arr) < 2:
        return (float(arr.mean()) if len(arr) else float("nan")), 0.0
    m = float(arr.mean())
    hw = float(t.interval(0.95, len(arr) - 1, loc=m,
                          scale=arr.std(ddof=1) / np.sqrt(len(arr)))[1] - m)
    return m, hw


def compute_binary_metrics(y_true, y_prob, threshold=0.5):
    y_pred = (y_prob >= threshold).astype(int)
    out = {
        "acc": float(accuracy_score(y_true, y_pred)),
        "f1": float(f1_score(y_true, y_pred, zero_division=0)),
        "precision": float(precision_score(y_true, y_pred, zero_division=0)),
        "recall": float(recall_score(y_true, y_pred, zero_division=0)),
    }
    if len(np.unique(y_true)) > 1:
        out["auc"] = float(roc_auc_score(y_true, y_prob))
    else:
        out["auc"] = float("nan")
    return out


def compute_multiclass_metrics(y_true, y_pred):
    return {
        "acc": float(accuracy_score(y_true, y_pred)),
        "f1_macro": float(f1_score(y_true, y_pred, average="macro",
                                   zero_division=0)),
    }


def eval_gnn_on_folds(data_list, folds, num_classes, epochs, hidden,
                      num_layers, dropout, lr, model_cls, tag):
    fold_metrics = []
    oof_prob = np.zeros((len(data_list), num_classes))
    oof_pred = np.zeros(len(data_list), dtype=int)

    for k, (tr, te) in enumerate(folds):
        tr_ds = [data_list[i] for i in tr]
        te_ds = [data_list[i] for i in te]
        y_te = np.array([d.y.item() for d in te_ds])

        if len(np.unique(y_te)) < 2 and num_classes == 2:
            fold_metrics.append({"auc": float("nan"), "f1": float("nan"),
                                 "acc": float("nan")})
            print(f"  [{tag}] fold {k}: single-class test set, skipped")
            continue

        _, prob, _ = train_and_eval_one(
            tr_ds, te_ds, num_classes=num_classes, epochs=epochs,
            hidden=hidden, num_layers=num_layers, dropout=dropout, lr=lr,
            model_cls=model_cls, fold_seed=RANDOM_SEED + k)

        if prob is None:
            fold_metrics.append({"auc": float("nan"), "f1": float("nan"),
                                 "acc": float("nan")})
            continue

        oof_prob[te] = prob
        pred = prob.argmax(axis=1)
        oof_pred[te] = pred

        if num_classes == 2:
            m = {
                "auc": float(roc_auc_score(y_te, prob[:, 1])),
                "f1": float(f1_score(y_te, pred, zero_division=0)),
                "acc": float(accuracy_score(y_te, pred)),
            }
        else:
            m = {
                "f1_macro": float(f1_score(y_te, pred, average="macro",
                                           zero_division=0)),
                "acc": float(accuracy_score(y_te, pred)),
            }
        fold_metrics.append(m)
        print(f"  [{tag}] fold {k}: {m}")

    if not fold_metrics:
        return None, oof_prob, oof_pred

    mean_metrics = {}
    keys = ["auc", "f1", "acc"] if num_classes == 2 else ["f1_macro", "acc"]
    for key in keys:
        vals = [x.get(key) for x in fold_metrics if x.get(key) is not None]
        m, hw = mean_ci95(vals)
        mean_metrics[key] = m
        mean_metrics[f"{key}_ci95"] = hw
    FOLD_METRICS[tag] = fold_metrics
    return mean_metrics, oof_prob, oof_pred


def bootstrap_metric_diff(y_true, prob_a, prob_b, metric="auc", n_boot=1000,
                          seed=RANDOM_SEED):
    rng = np.random.RandomState(seed)
    y_true = np.asarray(y_true)
    prob_a = np.asarray(prob_a)
    prob_b = np.asarray(prob_b)
    n = len(y_true)
    diffs = []
    for _ in range(n_boot):
        idx = rng.randint(0, n, size=n)
        yb = y_true[idx]
        if metric == "auc":
            if len(np.unique(yb)) < 2:
                continue
            try:
                va = roc_auc_score(yb, prob_a[idx])
                vb = roc_auc_score(yb, prob_b[idx])
            except Exception:
                continue
        elif metric == "f1":
            pa = (prob_a[idx] >= 0.5).astype(int)
            pb = (prob_b[idx] >= 0.5).astype(int)
            va = f1_score(yb, pa, zero_division=0)
            vb = f1_score(yb, pb, zero_division=0)
        elif metric == "acc":
            pa = (prob_a[idx] >= 0.5).astype(int)
            pb = (prob_b[idx] >= 0.5).astype(int)
            va = accuracy_score(yb, pa)
            vb = accuracy_score(yb, pb)
        else:
            raise ValueError(f"unsupported bootstrap metric: {metric}")
        diffs.append(va - vb)

    if not diffs:
        return float("nan"), float("nan"), float("nan"), float("nan")

    diffs = np.asarray(diffs)
    mean_diff = float(diffs.mean())
    ci_low = float(np.percentile(diffs, 2.5))
    ci_high = float(np.percentile(diffs, 97.5))
    p_val = float(2 * min((diffs <= 0).mean(), (diffs >= 0).mean()))
    p_val = min(p_val, 1.0)
    return mean_diff, ci_low, ci_high, p_val


def eval_cross_project_gnns(data_tr, data_te, df_tr, df_te, num_classes,
                            args):

    print("\n\n")
    print("Cross-Project Evaluation")
    print(f"  train samples = {len(data_tr)}  "
          f"(y dist = {df_tr['y'].value_counts().to_dict()})")
    print(f"  test  samples = {len(data_te)}  "
          f"(y dist = {df_te['y'].value_counts().to_dict()})")

    if len(data_te) == 0:
        print("  [WARN] test set is empty, skip cross-project evaluation")
        return {}, {}

    y_te = np.array([d.y.item() for d in data_te])
    results = {}
    oof_probs = {}

    for key, (display_name, model_cls) in MODEL_REGISTRY.items():
        print(f"\n  ----- {display_name} ({key}) -----")
        _, prob, _ = train_and_eval_one(
            data_tr, data_te, num_classes=num_classes,
            epochs=args.gnn_epochs, hidden=args.gnn_hidden,
            num_layers=args.gnn_layers, dropout=args.gnn_dropout,
            lr=args.gnn_lr, model_cls=model_cls,
            fold_seed=RANDOM_SEED, reset_stats=True)

        if prob is None:
            results[key] = None
            oof_probs[key] = None
            print("    [WARN] prob is None, skip")
            continue

        oof_probs[key] = prob
        if num_classes == 2:
            m = compute_binary_metrics(y_te, prob[:, 1], threshold=0.5)
        else:
            pred = prob.argmax(axis=1)
            m = compute_multiclass_metrics(y_te, pred)
        results[key] = m
        print(f"    -> {m}")

    return results, oof_probs


def save_cross_project_outputs(results, oof_probs, df_te, num_classes,
                               args, sig_rows):
    out_dir = args.out_dir

    for key, prob in oof_probs.items():
        if prob is None:
            continue
        oof_df = df_te[["file", "cycle_id", "struct_hash",
                        "label_real", "y"]].copy()
        if num_classes == 2:
            oof_df["cross_prob"] = prob[:, 1]
            oof_df["cross_pred"] = (prob[:, 1] >= 0.5).astype(int)
        else:
            oof_df["cross_prob"] = -1
            oof_df["cross_pred"] = prob.argmax(axis=1)
        oof_df.to_csv(os.path.join(out_dir, f"cross_gnn_{key}_oof.csv"),
                      index=False)

    rows = []
    for key in MODEL_REGISTRY:
        m = results.get(key)
        if m is None:
            continue
        name = MODEL_REGISTRY[key][0]
        row = {"model": name}
        row.update(m)
        rows.append(row)
    if rows:
        pd.DataFrame(rows).to_csv(
            os.path.join(out_dir, "cross_gnn_compare_summary.csv"),
            index=False)
        print(f"\n  [save] {out_dir}/cross_gnn_compare_summary.csv")

    if sig_rows:
        pd.DataFrame(sig_rows).to_csv(
            os.path.join(out_dir, "cross_gnn_compare_significance.csv"),
            index=False)
        print(f"  [save] {out_dir}/cross_gnn_compare_significance.csv")

def paired_t(values_a, values_b):
    a = np.asarray(values_a, dtype=float)
    b = np.asarray(values_b, dtype=float)
    mask = ~(np.isnan(a) | np.isnan(b))
    a, b = a[mask], b[mask]
    if len(a) < 2:
        return float("nan"), float("nan"), float("nan")
    t_stat, p_val = ttest_rel(a, b)
    return float(t_stat), float(p_val), float(np.mean(a - b))

def wilcoxon_p(values_a, values_b):
    a = np.asarray(values_a, dtype=float)
    b = np.asarray(values_b, dtype=float)
    mask = ~(np.isnan(a) | np.isnan(b))
    a, b = a[mask], b[mask]
    if len(a) < 2:
        return float("nan")
    if np.allclose(a - b, 0):
        return 1.0
    try:
        _, p_val = wilcoxon(a, b)
        return float(p_val)
    except Exception:
        return float("nan")

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--data_dir", default="dataset/train")
    ap.add_argument("--test_dir", default="dataset/test")
    ap.add_argument("--out_dir", default="./output_gnn_compare")
    ap.add_argument("--seed", type=int, default=42)
    ap.add_argument("--label_mode", default="binary_strong",
                    choices=["binary_strong", "binary_loose", "three_way"])
    ap.add_argument("--n_splits", type=int, default=5)
    ap.add_argument("--gnn_epochs", type=int, default=100)
    ap.add_argument("--gnn_hidden", type=int, default=32)
    ap.add_argument("--gnn_layers", type=int, default=1)
    ap.add_argument("--gnn_lr", type=float, default=1e-3)
    ap.add_argument("--gnn_dropout", type=float, default=0.3)
    ap.add_argument("--do_cross_project", action="store_true", default=True,
                    help="run cross-project (train on data_dir, test on test_dir) ?")
    ap.add_argument("--skip_cross_project", action="store_true", default=False,
                    help="skip cross-project")
    ap.add_argument("--n_boot", type=int, default=1000,
                    help="Cross-project bootstrap count")
    args = ap.parse_args()

    global RANDOM_SEED
    RANDOM_SEED = args.seed
    np.random.seed(RANDOM_SEED)
    torch.manual_seed(RANDOM_SEED)

    timestamp = time.strftime("%Y%m%d-%H%M%S")
    run_tag = f"gnn-compare-seed{RANDOM_SEED}-{timestamp}"
    args.out_dir = os.path.join(args.out_dir, run_tag)
    os.makedirs(args.out_dir, exist_ok=True)

    console_path = os.path.join(args.out_dir,
                                f"console-{args.label_mode}-seed{args.seed}.txt")
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

    print("Compare 4 GNN variants: GIN / GCN / GAT / GraphSAGE")
    print(f"  data_dir   = {args.data_dir}")
    print(f"  test_dir   = {args.test_dir}")
    print(f"  out_dir    = {args.out_dir}")
    print(f"  label_mode = {args.label_mode}")
    print(f"  n_splits   = {args.n_splits}")
    print(f"  hidden={args.gnn_hidden}, layers={args.gnn_layers}, "
          f"lr={args.gnn_lr}, dropout={args.gnn_dropout}, "
          f"epochs={args.gnn_epochs}")

    cycles_train = load_all(args.data_dir)
    cycles_test = load_all(args.test_dir)
    cycles = cycles_train + cycles_test
    if not cycles:
        print("[ERROR] no data found")
        return

    df = build_feature_table(cycles)
    df = apply_label_mode(df, args.label_mode)
    print(f"\n[data] Total={len(df)}, unique struct_hash={df['struct_hash'].nunique()}")
    print(f"[data] y dist = {df['y'].value_counts().to_dict()}")

    df.to_csv(os.path.join(args.out_dir, "features.csv"), index=False)

    # 1. In-distribution GroupKFold
    folds = make_folds(df, n_splits=args.n_splits)
    print(f"[fold] GroupKFold n_splits={len(folds)}")

    num_classes = 2 if args.label_mode.startswith("binary") else 3

    valid_cycles = [cycles[i] for i in df.index.values]
    data_list = [cycle_to_hetero(c) for c in valid_cycles]
    y_eff = df["y"].values.astype(int)
    for i, d in enumerate(data_list):
        d.y = torch.tensor([int(y_eff[i])], dtype=torch.long)
    print(f"[graph] built {len(data_list)} heterogeneous graphs")

    results = {}
    for key, (display_name, model_cls) in MODEL_REGISTRY.items():
        print(f"\n===== {display_name} ({key}) =====")
        tag = f"gnn_{key}"
        res, oof_prob, oof_pred = eval_gnn_on_folds(
            data_list, folds, num_classes=num_classes,
            epochs=args.gnn_epochs, hidden=args.gnn_hidden,
            num_layers=args.gnn_layers, dropout=args.gnn_dropout,
            lr=args.gnn_lr, model_cls=model_cls, tag=tag)
        results[key] = res
        print(f"  [{display_name} CV] {res}")

        oof_df = df[["file", "cycle_id", "struct_hash",
                     "label_real", "y"]].copy()
        if num_classes == 2 and oof_prob is not None:
            oof_df["oof_prob"] = oof_prob[:, 1]
        else:
            oof_df["oof_prob"] = -1
        oof_df["oof_pred"] = oof_pred
        oof_df.to_csv(os.path.join(args.out_dir, f"{tag}_oof.csv"), index=False)

    print("\n\n")
    print("Summary (GroupKFold by struct_hash)")
    rows = []
    for key in MODEL_REGISTRY:
        m = results.get(key)
        if m is None:
            continue
        name = MODEL_REGISTRY[key][0]
        if num_classes == 2:
            rows.append({
                "model": name,
                "auc": m.get("auc"),
                "auc_ci95": m.get("auc_ci95"),
                "f1": m.get("f1"),
                "f1_ci95": m.get("f1_ci95"),
                "acc": m.get("acc"),
                "acc_ci95": m.get("acc_ci95"),
            })
        else:
            rows.append({
                "model": name,
                "f1_macro": m.get("f1_macro"),
                "f1_macro_ci95": m.get("f1_macro_ci95"),
                "acc": m.get("acc"),
                "acc_ci95": m.get("acc_ci95"),
            })
    summary_df = pd.DataFrame(rows)
    summary_df.to_csv(os.path.join(args.out_dir, "gnn_compare_summary.csv"),
                      index=False)

    if num_classes == 2:
        print(f"  {'Model':12s} {'AUC (95% CI)':>22s} {'F1':>8s} {'Acc':>8s}")
        for _, r in summary_df.iterrows():
            print(f"  {r['model']:12s} "
                  f"{r['auc']:.4f} ± {r['auc_ci95']:.4f}       "
                  f"{r['f1']:8.4f} {r['acc']:8.4f}")
    else:
        print(f"  {'Model':12s} {'macroF1 (95% CI)':>24s} {'Acc':>8s}")
        for _, r in summary_df.iterrows():
            print(f"  {r['model']:12s} "
                  f"{r['f1_macro']:.4f} ± {r['f1_macro_ci95']:.4f}        "
                  f"{r['acc']:8.4f}")

    print("\n\n")
    print("Pairwise significance tests (paired by fold, in-distribution)")
    metric = "auc" if num_classes == 2 else "f1_macro"
    model_keys = list(MODEL_REGISTRY.keys())
    sig_rows = []
    print(f"  metric = {metric}")
    print(f"  {'A':10s} {'B':10s} {'mean(A)':>9s} {'mean(B)':>9s} "
          f"{'diff':>8s} {'t-p':>10s} {'w-p':>10s} {'sig':>5s}")
    for i in range(len(model_keys)):
        for j in range(i + 1, len(model_keys)):
            ka, kb = model_keys[i], model_keys[j]
            ta = f"gnn_{ka}"
            tb = f"gnn_{kb}"
            if ta not in FOLD_METRICS or tb not in FOLD_METRICS:
                continue
            va = [m.get(metric) for m in FOLD_METRICS[ta]
                  if m.get(metric) is not None]
            vb = [m.get(metric) for m in FOLD_METRICS[tb]
                  if m.get(metric) is not None]
            n = min(len(va), len(vb))
            if n < 2:
                continue
            va, vb = va[:n], vb[:n]
            _, p_t, diff = paired_t(va, vb)
            p_w = wilcoxon_p(va, vb)
            sig = "***" if p_t < 0.001 else "**" if p_t < 0.01 else "*" if p_t < 0.05 else ""
            print(f"  {MODEL_REGISTRY[ka][0]:10s} "
                  f"{MODEL_REGISTRY[kb][0]:10s} "
                  f"{np.mean(va):>9.4f} {np.mean(vb):>9.4f} "
                  f"{diff:>8.4f} {p_t:>10.4g} {p_w:>10.4g} {sig:>5s}")
            sig_rows.append({
                "model_A": MODEL_REGISTRY[ka][0],
                "model_B": MODEL_REGISTRY[kb][0],
                "metric": metric,
                "mean_A": float(np.mean(va)),
                "mean_B": float(np.mean(vb)),
                "mean_diff": float(diff),
                "paired_ttest_p": p_t,
                "wilcoxon_p": p_w,
                "n_folds": n,
            })
    if sig_rows:
        pd.DataFrame(sig_rows).to_csv(
            os.path.join(args.out_dir, "gnn_compare_significance.csv"),
            index=False)

    # 2. Cross-project
    cross_results, cross_oof_probs = {}, {}
    cross_sig_rows = []
    if (not args.skip_cross_project) and args.do_cross_project:
        df_train_only = build_feature_table(cycles_train)
        df_test_only = build_feature_table(cycles_test)
        df_train_only = apply_label_mode(df_train_only, args.label_mode)
        df_test_only = apply_label_mode(df_test_only, args.label_mode)

        train_cycles_valid = [cycles_train[i] for i in df_train_only.index.values]
        test_cycles_valid = [cycles_test[i] for i in df_test_only.index.values]

        data_tr = [cycle_to_hetero(c) for c in train_cycles_valid]
        data_te = [cycle_to_hetero(c) for c in test_cycles_valid]

        y_tr = df_train_only["y"].values.astype(int)
        y_te = df_test_only["y"].values.astype(int)
        for i, d in enumerate(data_tr):
            d.y = torch.tensor([int(y_tr[i])], dtype=torch.long)
        for i, d in enumerate(data_te):
            d.y = torch.tensor([int(y_te[i])], dtype=torch.long)

        cross_results, cross_oof_probs = eval_cross_project_gnns(
            data_tr, data_te, df_train_only, df_test_only,
            num_classes=num_classes, args=args)

        print("\n\n")
        print("Cross-Project Summary (test set metrics)")
        c_rows = []
        for key in MODEL_REGISTRY:
            m = cross_results.get(key)
            if m is None:
                continue
            name = MODEL_REGISTRY[key][0]
            row = {"model": name}
            row.update(m)
            c_rows.append(row)
        if c_rows:
            c_df = pd.DataFrame(c_rows)
            if num_classes == 2:
                print(f"  {'Model':12s} {'AUC':>8s} {'F1':>8s} {'P':>8s} "
                      f"{'R':>8s} {'Acc':>8s}")
                for _, r in c_df.iterrows():
                    print(f"  {r['model']:12s} {r['auc']:8.4f} "
                          f"{r['f1']:8.4f} {r['precision']:8.4f} "
                          f"{r['recall']:8.4f} {r['acc']:8.4f}")
            else:
                print(f"  {'Model':12s} {'macroF1':>10s} {'Acc':>8s}")
                for _, r in c_df.iterrows():
                    print(f"  {r['model']:12s} {r['f1_macro']:10.4f} "
                          f"{r['acc']:8.4f}")

        if num_classes == 2:
            print("\n\n")
            print(f"Cross-Project pairwise bootstrap significance "
                  f"(n_boot={args.n_boot})")
            print(f"  {'A':10s} {'B':10s} {'metric':8s} "
                  f"{'diff':>8s} {'95% CI':>22s} {'p':>10s} {'sig':>5s}")
            for i in range(len(model_keys)):
                for j in range(i + 1, len(model_keys)):
                    ka, kb = model_keys[i], model_keys[j]
                    if cross_oof_probs.get(ka) is None or \
                       cross_oof_probs.get(kb) is None:
                        continue
                    pa = cross_oof_probs[ka][:, 1]
                    pb = cross_oof_probs[kb][:, 1]
                    for metric_name in ["auc", "f1"]:
                        md, lo, hi, p = bootstrap_metric_diff(
                            y_te, pa, pb, metric=metric_name,
                            n_boot=args.n_boot, seed=RANDOM_SEED)
                        sig = "***" if p < 0.001 else "**" if p < 0.01 else \
                              "*" if p < 0.05 else ""
                        print(f"  {MODEL_REGISTRY[ka][0]:10s} "
                              f"{MODEL_REGISTRY[kb][0]:10s} "
                              f"{metric_name:8s} "
                              f"{md:>8.4f} [{lo:>8.4f}, {hi:>8.4f}] "
                              f"{p:>10.4g} {sig:>5s}")
                        cross_sig_rows.append({
                            "model_A": MODEL_REGISTRY[ka][0],
                            "model_B": MODEL_REGISTRY[kb][0],
                            "metric": metric_name,
                            "mean_diff": float(md),
                            "ci95_low": float(lo),
                            "ci95_high": float(hi),
                            "bootstrap_p": float(p),
                            "n_boot": args.n_boot,
                        })

        save_cross_project_outputs(
            cross_results, cross_oof_probs, df_test_only,
            num_classes, args, cross_sig_rows)

    summary = {
        "config": vars(args),
        "n_samples": len(df),
        "n_structures": int(df["struct_hash"].nunique()),
        "y_distribution": df["y"].value_counts().to_dict(),
        "in_distribution": {
            "results": results,
            "significance": sig_rows,
        },
        "cross_project": {
            "results": cross_results,
            "significance": cross_sig_rows,
        },
    }
    with open(os.path.join(args.out_dir,
                            f"summary-{args.label_mode}-seed{args.seed}.json"),
              "w") as f:
        json.dump(summary, f, indent=2, default=str)
    print(f"\n[save] {args.out_dir}/summary-{args.label_mode}-seed{args.seed}.json")

    sys.stdout = sys.__stdout__
    sys.stderr = sys.__stderr__
    console_file.close()
    print(f"[save] {console_path}")

if __name__ == "__main__":
    main()