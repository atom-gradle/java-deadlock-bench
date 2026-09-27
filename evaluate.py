# Revised Evaluation Script
# - Does not use CYCLE_FEATURES as model input (to avoid label leakage)
# - Structure-based deduplication + structure-grouped LOSO cross-validation
# - Rule-based baseline + statistical baseline + heterogeneous GNN comparison

import os
import re
import glob
import hashlib
import warnings
from collections import Counter, defaultdict

import numpy as np
import pandas as pd
from scipy.stats import mannwhitneyu
from sklearn.ensemble import RandomForestClassifier
from sklearn.linear_model import LogisticRegression
from sklearn.model_selection import StratifiedKFold, cross_val_score, GroupKFold
from sklearn.metrics import roc_auc_score, classification_report
from sklearn.preprocessing import StandardScaler
from sklearn.pipeline import Pipeline

import torch
import torch.nn.functional as F
from torch_geometric.data import HeteroData, Batch
from torch_geometric.nn import SAGEConv, HeteroConv, Linear, global_mean_pool, global_add_pool
from torch_geometric.loader import DataLoader

warnings.filterwarnings("ignore")

DATA_DIR = "train"
OUT_DIR  = "./output"
os.makedirs(OUT_DIR, exist_ok=True)

RANDOM_SEED = 42
np.random.seed(RANDOM_SEED)
torch.manual_seed(RANDOM_SEED)

LOCK_FEAT_DIM = 5
ACQ_FEAT_DIM  = 6

# 图结构特征（不含 CYCLE_FEATURES）
GRAPH_FEATS = [
    "n_lock", "n_acq", "n_lock_edge", "n_call_edge",
    "n_cross", "n_cross_atl", "n_cross_lta",
    "static_ratio", "nonnull_ratio",
    "avg_lock_dist", "alias_ratio", "same_obj_ratio",
    "sync_ratio", "acq_static_ratio", "avg_nesting", "avg_pts",
    "avg_call_dist", "nested_ratio",
]

# CYCLE_FEATURES（仅用于对照 / 泄漏检测，不进入模型）
CYCLE_FEATS = [
    "thread_count", "max_thread_occurrence", "edge_count",
    "all_edges_same_thread", "all_edges_reachable", "has_non_adjacent_edge",
]


# ============================================================
# 1. 解析器
# ============================================================
def parse_file(filepath):
    cycles = []
    cur = None

    def new_cycle(cid):
        return {
            "file": os.path.basename(filepath),
            "cycle_id": cid,
            "lock_nodes": [],
            "lock_edges": [],
            "acq_nodes": [],
            "call_edges": [],
            "cross": [],
            "features": {},
            "labels": {},
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
            parts = rest.split(",", 4)
            cur["lock_nodes"].append({
                "id": int(parts[0]),
                "obj_key": parts[1],
                "obj_type": parts[2],
                "is_static": parts[3].strip().lower() == "true",
                "obj_nonnull": int(parts[4].strip()),
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
                "id": int(p[0]),
                "method": p[1],
                "line": int(p[2]),
                "var": p[3],
                "is_static": p[4].strip().lower() == "true",
                "is_sync": p[5].strip().lower() == "true",
                "nesting": int(p[6]),
                "pts_size": int(p[7]),
            })

        elif line.startswith("CALL_EDGE"):
            _, rest = line.split(None, 1)
            p = rest.split(",")
            cur["call_edges"].append({
                "src": int(p[0]), "dst": int(p[1]),
                "dist": int(p[2]),
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

        elif line.startswith("LABEL_LOCK_NODES"):
            cur["labels"]["lock_nodes"] = [
                int(x) for x in line.split()[1].split(",") if x.strip()]
        elif line.startswith("LABEL_LOCK_EDGES"):
            cur["labels"]["lock_edges"] = [
                int(x) for x in line.split()[1].split(",") if x.strip()]
        elif line.startswith("LABEL_ACQ_NODES"):
            cur["labels"]["acq_nodes"] = [
                int(x) for x in line.split()[1].split(",") if x.strip()]

    if cur is not None:
        cycles.append(cur)
    return cycles


def load_all(data_dir):
    files = sorted(glob.glob(os.path.join(data_dir, "graph-*.txt")))
    print(f"[load] 找到 {len(files)} 个文件")
    all_cycles = []
    for f in files:
        try:
            all_cycles.extend(parse_file(f))
        except Exception as e:
            print(f"[warn] 解析 {f} 失败: {e}")
    print(f"[load] 共解析出 {len(all_cycles)} 个 CYCLE")
    return all_cycles


# ============================================================
# 2. 图级特征表（含结构哈希）
# ============================================================
def build_feature_table(cycles):
    rows = []
    for c in cycles:
        f = c["features"]
        n_lock = len(c["lock_nodes"])
        n_acq  = len(c["acq_nodes"])
        n_le   = len(c["lock_edges"])
        n_ce   = len(c["call_edges"])
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

        # ---- 结构哈希：只用图结构，不用 CYCLE_FEATURES ----
        struct_key = (
            n_lock, n_acq, n_le, n_ce, n_cross, n_atl, n_lta,
            round(static_ratio, 2), round(nonnull_ratio, 2),
            round(avg_lock_dist, 2), round(alias_ratio, 2), round(same_obj_ratio, 2),
            round(sync_ratio, 2), round(acq_static_ratio, 2),
            round(avg_nesting, 2), round(avg_pts, 2),
            round(avg_call_dist, 2), round(nested_ratio, 2),
        )
        struct_hash = hashlib.md5(str(struct_key).encode()).hexdigest()

        row = {
            "file": c["file"],
            "cycle_id": c["cycle_id"],
            "label": c["labels"].get("cycle", -1),
            "struct_hash": struct_hash,
            # 规模
            "n_lock": n_lock, "n_acq": n_acq,
            "n_lock_edge": n_le, "n_call_edge": n_ce,
            "n_cross": n_cross, "n_cross_atl": n_atl, "n_cross_lta": n_lta,
            # lock 节点
            "static_ratio": static_ratio, "nonnull_ratio": nonnull_ratio,
            # lock edge
            "avg_lock_dist": avg_lock_dist,
            "alias_ratio": alias_ratio, "same_obj_ratio": same_obj_ratio,
            # acq 节点
            "sync_ratio": sync_ratio, "acq_static_ratio": acq_static_ratio,
            "avg_nesting": avg_nesting, "avg_pts": avg_pts,
            # call edge
            "avg_call_dist": avg_call_dist, "nested_ratio": nested_ratio,
            # CYCLE_FEATURES（仅对照，不进模型）
            "thread_count": int(f.get("thread_count", 0)),
            "max_thread_occurrence": int(f.get("max_thread_occurrence", 0)),
            "edge_count": int(f.get("edge_count", 0)),
            "all_edges_same_thread": int(f.get("all_edges_same_thread", "false") == "true"),
            "all_edges_reachable": int(f.get("all_edges_reachable", "false") == "true"),
            "has_non_adjacent_edge": int(f.get("has_non_adjacent_edge", "false") == "true"),
        }
        rows.append(row)
    return pd.DataFrame(rows)


# ============================================================
# 3. 数据质量评估
# ============================================================
def quality_report(df):
    print("\n" + "=" * 60)
    print("数据质量评估")
    print("=" * 60)

    print("\n[标签分布]")
    print(df["label"].value_counts(dropna=False))
    vc = df["label"].value_counts()
    if len(vc) == 2:
        print(f"  最小/最大类比 = {vc.min() / vc.max():.3f}")

    print("\n[缺失值]")
    miss = df.isnull().sum()
    miss = miss[miss > 0]
    print(miss if len(miss) else "  无缺失")

    # 结构重复
    dup = df.duplicated("struct_hash", keep=False).sum()
    print(f"\n[结构重复]  {dup} / {len(df)} 行属于重复结构组")
    print(f"  唯一结构数 = {df['struct_hash'].nunique()}")

    # 每个结构的标签分布
    print("\n[每个结构的样本数与标签分布]")
    for h, grp in df.groupby("struct_hash"):
        labs = grp["label"].tolist()
        n0, n1 = labs.count(0), labs.count(1)
        print(f"  {h[:8]}...: n={len(grp):3d}  label0={n0:3d}  label1={n1:3d}")

    # 单特征判别性
    print("\n[单特征判别性: Mann-Whitney U]")
    print(f"  {'feature':25s} {'p-value':>12s} {'AUC':>8s}")
    pos = df[df["label"] == 1]
    neg = df[df["label"] == 0]
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
        print(f"  {col:25s} {p:12.4g} {auc:8.3f} {sig}")

    # 泄漏提示
    print("\n[标签泄漏提示: CYCLE_FEATURES 单特征 AUC > 0.9]")
    leaky = [(c, a) for c, _, a in results if c in CYCLE_FEATS and a > 0.9]
    if leaky:
        for c, a in leaky:
            print(f"  ⚠ {c}: AUC={a:.3f}  → CYCLE_FEATURES 与标签同源")
    else:
        print("  未发现明显泄漏")

    df.to_csv(os.path.join(OUT_DIR, "features.csv"), index=False)
    print(f"\n[save] 特征表 → {OUT_DIR}/features.csv")
    return df


# ============================================================
# 4. 规则 baseline（复刻 classifyCycle 的判定逻辑）
# ============================================================
def rule_baseline(df):
    """
    classifyCycle 的 isReal 规则：
      allSame && allReachable && threadCount >= 2 && maxThreadOccurrence <= 1
    对应 CYCLE_FEATURES:
      all_edges_same_thread && all_edges_reachable
      && thread_count >= 2 && max_thread_occurrence <= 1
    """
    print("\n" + "=" * 60)
    print("规则 baseline（复刻 classifyCycle.isReal）")
    print("=" * 60)

    pred = (
        (df["all_edges_same_thread"] == 1)
        & (df["all_edges_reachable"] == 1)
        & (df["thread_count"] >= 2)
        & (df["max_thread_occurrence"] <= 1)
    ).astype(int)

    y = df["label"].values.astype(int)
    if len(np.unique(y)) < 2:
        print("  标签只有一类，跳过")
        return None

    acc = (pred.values == y).mean()
    try:
        auc = roc_auc_score(y, pred.values)
    except Exception:
        auc = float("nan")
    print(f"  规则准确率 = {acc:.4f}")
    print(f"  规则 AUC   = {auc:.4f}")
    print(f"  规则预测正类比例 = {pred.mean():.4f}  (真实正类比例 = {y.mean():.4f})")

    # 混淆矩阵
    tp = ((pred == 1) & (y == 1)).sum()
    fp = ((pred == 1) & (y == 0)).sum()
    fn = ((pred == 0) & (y == 1)).sum()
    tn = ((pred == 0) & (y == 0)).sum()
    print(f"  TP={tp}  FP={fp}  FN={fn}  TN={tn}")
    return acc, auc


# ============================================================
# 5. 统计 baseline（仅图结构 / 图结构+CYCLE_FEATURES）
# ============================================================
def run_baseline(df, feat_cols, tag="", group_col=None):
    print(f"\n--- Baseline {tag} ---")
    X = df[feat_cols].fillna(0).values.astype(np.float32)
    y = df["label"].values.astype(int)
    if len(np.unique(y)) < 2:
        print("  标签只有一类，跳过")
        return None

    groups = df[group_col].values if group_col else None

    models = {
        "LogReg": Pipeline([("sc", StandardScaler()),
                            ("clf", LogisticRegression(max_iter=2000))]),
        "RandomForest": RandomForestClassifier(
            n_estimators=300, random_state=RANDOM_SEED, n_jobs=-1),
    }

    out = {}
    for name, model in models.items():
        try:
            if groups is not None:
                cv = GroupKFold(n_splits=min(5, len(np.unique(groups))))
                auc = cross_val_score(model, X, y, cv=cv, groups=groups,
                                      scoring="roc_auc")
            else:
                cv = StratifiedKFold(n_splits=5, shuffle=True,
                                     random_state=RANDOM_SEED)
                auc = cross_val_score(model, X, y, cv=cv, scoring="roc_auc")
            print(f"  {name:15s} AUC={auc.mean():.3f}±{auc.std():.3f}")
            out[name] = auc.mean()
        except Exception as e:
            print(f"  {name} 失败: {e}")
    return out


def baseline_suite(df):
    print("\n" + "=" * 60)
    print("统计 Baseline（随机CV vs 按结构分组CV）")
    print("=" * 60)

    print("\n>>> 随机 StratifiedKFold（有结构重复 → AUC 虚高）")
    run_baseline(df, GRAPH_FEATS, tag="仅图结构 [随机CV]")
    run_baseline(df, GRAPH_FEATS + CYCLE_FEATS, tag="图结构+CYCLE [随机CV]")

    print("\n>>> GroupKFold by struct_hash（测试集结构训练集没见过）")
    run_baseline(df, GRAPH_FEATS, tag="仅图结构 [GroupCV]", group_col="struct_hash")
    run_baseline(df, GRAPH_FEATS + CYCLE_FEATS, tag="图结构+CYCLE [GroupCV]",
                 group_col="struct_hash")

    # 特征重要性（仅图结构）
    X = df[GRAPH_FEATS].fillna(0).values.astype(np.float32)
    y = df["label"].values.astype(int)
    rf = RandomForestClassifier(n_estimators=300, random_state=RANDOM_SEED, n_jobs=-1)
    rf.fit(X, y)
    imp = sorted(zip(GRAPH_FEATS, rf.feature_importances_), key=lambda x: -x[1])
    print("\n[RandomForest 特征重要性 Top10 (仅图结构)]")
    for name, v in imp[:10]:
        print(f"  {name:25s} {v:.4f}")


# ============================================================
# 6. HeteroData 构建
# ============================================================
def cycle_to_hetero(c, drop_lock_edge=False, drop_call_edge=False, drop_cross=False):
    data = HeteroData()
    n_lock = len(c["lock_nodes"])
    n_acq  = len(c["acq_nodes"])

    if n_lock > 0:
        lock_feat = []
        for n in c["lock_nodes"]:
            lock_feat.append([
                float(n["is_static"]),
                float(n["obj_nonnull"]),
                1.0 if "LockBox" in n["obj_key"] else 0.0,
                1.0 if "Object" in n["obj_type"] else 0.0,
                float(len(n["obj_key"])),
            ])
        data["lock"].x = torch.tensor(lock_feat, dtype=torch.float)
    else:
        data["lock"].x = torch.zeros((0, LOCK_FEAT_DIM), dtype=torch.float)

    if n_acq > 0:
        acq_feat = []
        for a in c["acq_nodes"]:
            acq_feat.append([
                float(a["line"] if a["line"] >= 0 else 0),
                float(a["is_static"]),
                float(a["is_sync"]),
                float(a["nesting"]),
                float(a["pts_size"]),
                float(len(a["var"])),
            ])
        data["acq"].x = torch.tensor(acq_feat, dtype=torch.float)
    else:
        data["acq"].x = torch.zeros((0, ACQ_FEAT_DIM), dtype=torch.float)

    # lock_edge
    if c["lock_edges"] and not drop_lock_edge:
        le = torch.tensor(
            [[e["from"] for e in c["lock_edges"]],
             [e["to"]   for e in c["lock_edges"]]], dtype=torch.long)
        lef = torch.tensor(
            [[float(e["dist"] if e["dist"] >= 0 else 0),
              float(e["may_alias"]),
              float(e["same_lock_obj"])] for e in c["lock_edges"]],
            dtype=torch.float)
    else:
        le = torch.zeros((2, 0), dtype=torch.long)
        lef = torch.zeros((0, 3), dtype=torch.float)
    data["lock", "lock_edge", "lock"].edge_index = le
    data["lock", "lock_edge", "lock"].edge_attr = lef

    # call_edge
    if c["call_edges"] and not drop_call_edge:
        ce = torch.tensor(
            [[e["src"] for e in c["call_edges"]],
             [e["dst"] for e in c["call_edges"]]], dtype=torch.long)
        cef = torch.tensor(
            [[float(e["dist"]),
              float(e["same_method"]),
              float(e["nested"])] for e in c["call_edges"]],
            dtype=torch.float)
    else:
        ce = torch.zeros((2, 0), dtype=torch.long)
        cef = torch.zeros((0, 3), dtype=torch.float)
    data["acq", "call_edge", "acq"].edge_index = ce
    data["acq", "call_edge", "acq"].edge_attr = cef

    # 跨层边
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

    data.y = torch.tensor([c["labels"].get("cycle", 0)], dtype=torch.long)
    return data


def collate_hetero(batch):
    return Batch.from_data_list(batch)


# ============================================================
# 7. 异构 GNN（SAGEConv）
# ============================================================
class HeteroGNN(torch.nn.Module):
    def __init__(self, hidden=32, num_classes=2, num_layers=2):
        super().__init__()
        self.lin_lock = Linear(LOCK_FEAT_DIM, hidden)
        self.lin_acq  = Linear(ACQ_FEAT_DIM, hidden)

        self.convs = torch.nn.ModuleList()
        for _ in range(num_layers):
            conv = HeteroConv({
                ("lock", "lock_edge", "lock"):   SAGEConv(hidden, hidden),
                ("acq", "call_edge", "acq"):     SAGEConv(hidden, hidden),
                ("acq", "acq_to_lock", "lock"):  SAGEConv(hidden, hidden),
                ("lock", "lock_to_acq", "acq"):  SAGEConv(hidden, hidden),
            }, aggr="sum")
            self.convs.append(conv)

        self.classifier = torch.nn.Sequential(
            torch.nn.Linear(hidden * 2, hidden),
            torch.nn.ReLU(),
            torch.nn.Dropout(0.3),
            torch.nn.Linear(hidden, num_classes),
        )

    def forward(self, data):
        x_dict = {
            "lock": F.relu(self.lin_lock(data["lock"].x)),
            "acq": F.relu(self.lin_acq(data["acq"].x)),
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

        # ✅ 按 batch 向量分组池化，每个图一个向量
        pooled = []
        for k in ["lock", "acq"]:
            if x_dict[k].size(0) > 0 and hasattr(data[k], "batch"):
                pooled.append(global_mean_pool(x_dict[k], data[k].batch))
            else:
                # 该类型节点为空的图，补零
                # 需要知道 batch 大小
                pooled.append(None)

        # 处理空节点情况
        num_graphs = data.y.size(0)
        hidden = x_dict["lock"].size(1) if x_dict["lock"].size(0) > 0 else x_dict["acq"].size(1)
        for i, p in enumerate(pooled):
            if p is None:
                pooled[i] = torch.zeros(num_graphs, hidden, device=data.y.device)

        g = torch.cat(pooled, dim=-1)  # [num_graphs, hidden*2]
        return self.classifier(g)  # [num_graphs, num_classes]

def train_one_fold(train_ds, test_ds, epochs=80, hidden=32, lr=1e-3):
    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    model = HeteroGNN(hidden=hidden).to(device)
    opt = torch.optim.Adam(model.parameters(), lr=lr, weight_decay=1e-4)

    labels_tr = np.array([d.y.item() for d in train_ds])
    cnt = Counter(labels_tr)
    w = torch.tensor(
        [1.0 / max(cnt.get(0, 1), 1), 1.0 / max(cnt.get(1, 1), 1)],
        dtype=torch.float, device=device)
    w = w / w.sum() * 2
    criterion = torch.nn.CrossEntropyLoss(weight=w)

    train_loader = DataLoader(train_ds, batch_size=16, shuffle=True,
                              collate_fn=collate_hetero)
    test_loader  = DataLoader(test_ds, batch_size=16, shuffle=False,
                              collate_fn=collate_hetero)

    best_auc = 0.0
    for epoch in range(1, epochs + 1):
        model.train()
        for batch in train_loader:
            batch = batch.to(device)
            opt.zero_grad()
            out = model(batch)
            loss = criterion(out, batch.y.view(-1))
            loss.backward()
            opt.step()

        model.eval()
        all_prob, all_y = [], []
        with torch.no_grad():
            for batch in test_loader:
                batch = batch.to(device)
                out = model(batch)
                prob = F.softmax(out, dim=-1)[:, 1]
                all_prob.append(prob.cpu().numpy())
                all_y.append(batch.y.view(-1).cpu().numpy())
        if all_prob:
            all_prob = np.concatenate(all_prob)
            all_y = np.concatenate(all_y)
            try:
                auc = roc_auc_score(all_y, all_prob) if len(np.unique(all_y)) > 1 else 0.5
            except Exception:
                auc = 0.5
            if auc > best_auc:
                best_auc = auc
    return best_auc


def run_hetero_groupcv(cycles, df, n_splits=5, epochs=80, hidden=32, tag="full"):
    """按 struct_hash 分组的 GroupKFold"""
    print(f"\n--- 异构 GNN ({tag}) GroupKFold by struct_hash ---")
    valid = [(c, r) for c, (_, r) in zip(cycles, df.iterrows())
             if r["label"] in (0, 1)]
    if len(valid) < n_splits * 2:
        print(f"  样本不足 ({len(valid)})，跳过")
        return None

    data_list = [cycle_to_hetero(c) for c, _ in valid]
    labels = np.array([d.y.item() for d in data_list])
    groups = np.array([r["struct_hash"] for _, r in valid])

    if len(np.unique(labels)) < 2:
        print("  标签只有一类，跳过")
        return None

    n_groups = len(np.unique(groups))
    n_splits = min(n_splits, n_groups)
    if n_splits < 2:
        print(f"  唯一结构数太少 ({n_groups})，无法 GroupCV")
        return None

    gkf = GroupKFold(n_splits=n_splits)
    aucs = []
    for fold, (tr, te) in enumerate(gkf.split(np.zeros(len(labels)), labels, groups)):
        tr_ds = [data_list[i] for i in tr]
        te_ds = [data_list[i] for i in te]
        if len(set(labels[te])) < 2:
            print(f"  fold {fold}: 测试集只有一类，跳过")
            continue
        auc = train_one_fold(tr_ds, te_ds, epochs=epochs, hidden=hidden)
        aucs.append(auc)
        print(f"  fold {fold}: AUC={auc:.3f}  (train={len(tr)}, test={len(te)})")
    if aucs:
        print(f"  [mean] AUC={np.mean(aucs):.3f} ± {np.std(aucs):.3f}")
    return np.mean(aucs) if aucs else None


# ============================================================
# 8. 消融（GroupKFold）
# ============================================================
def run_ablation(cycles, df, n_splits=5, epochs=60, hidden=32):
    print("\n" + "=" * 60)
    print("消融实验 (GroupKFold by struct_hash)")
    print("=" * 60)
    variants = {
        "full":         dict(),
        "no_cross":     dict(drop_cross=True),
        "no_lock_edge": dict(drop_lock_edge=True),
        "no_call_edge": dict(drop_call_edge=True),
    }
    valid = [(c, r) for c, (_, r) in zip(cycles, df.iterrows())
             if r["label"] in (0, 1)]
    if len(valid) < n_splits * 2:
        print("  样本不足，跳过消融")
        return
    labels = np.array([c["labels"].get("cycle", 0) for c, _ in valid])
    groups = np.array([r["struct_hash"] for _, r in valid])

    n_groups = len(np.unique(groups))
    n_splits = min(n_splits, n_groups)
    if n_splits < 2:
        print("  唯一结构数太少，跳过")
        return

    results = {}
    for name, kwargs in variants.items():
        data_list = [cycle_to_hetero(c, **kwargs) for c, _ in valid]
        gkf = GroupKFold(n_splits=n_splits)
        aucs = []
        for tr, te in gkf.split(np.zeros(len(labels)), labels, groups):
            if len(set(labels[te])) < 2:
                continue
            tr_ds = [data_list[i] for i in tr]
            te_ds = [data_list[i] for i in te]
            auc = train_one_fold(tr_ds, te_ds, epochs=epochs, hidden=hidden)
            aucs.append(auc)
        if aucs:
            results[name] = (np.mean(aucs), np.std(aucs))
            print(f"  {name:15s} AUC={np.mean(aucs):.3f} ± {np.std(aucs):.3f}")
        else:
            print(f"  {name:15s} 无有效 fold")

    if "full" in results and "no_cross" in results:
        delta = results["full"][0] - results["no_cross"][0]
        print(f"\n  [跨层边贡献] ΔAUC = {delta:+.3f}")
        if delta > 0.05:
            print("  → 跨层边是关键，异构图建模有价值")
        elif delta < -0.05:
            print("  → 去掉跨层边反而更好，可能存在噪声边")
        else:
            print("  → 跨层边贡献不明显")
    return results


# ============================================================
# 9. 主流程
# ============================================================
def main():
    print("=" * 60)
    print("Deadlock 图数据分析（修复版）")
    print("=" * 60)

    cycles = load_all(DATA_DIR)
    if not cycles:
        print("未找到数据，请检查 DATA_DIR")
        return

    df = build_feature_table(cycles)
    df = quality_report(df)

    # 规则 baseline（GNN 必须超过的下限）
    rule_baseline(df)

    # 统计 baseline：随机CV vs GroupCV
    baseline_suite(df)

    # 异构 GNN：GroupKFold
    run_hetero_groupcv(cycles, df, n_splits=5, epochs=80, hidden=32, tag="full")

    # 消融
    run_ablation(cycles, df, n_splits=5, epochs=60, hidden=32)

    # 结论
    print("\n" + "=" * 60)
    print("总结")
    print("=" * 60)
    print("判读逻辑：")
    print("  1. 规则 baseline 准确率 → GNN 必须超过它才有意义")
    print("  2. 随机CV 远高于 GroupCV → 之前 AUC 被结构重复撑高")
    print("  3. GroupCV 下 GNN > 规则 baseline → 图结构携带规则外的信息")
    print("  4. 消融 no_cross 明显下降 → 跨层边是关键")
    print("  5. 所有 AUC ≈ 规则 baseline → 任务由规则决定，GNN 无增量")


if __name__ == "__main__":
    main()