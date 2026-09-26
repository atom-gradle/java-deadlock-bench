# 异构 GNN 死锁检测：lock 节点 + acq 节点，四类边
import os
import glob
import argparse
import random
from typing import List, Dict

import torch
import torch.nn as nn
import torch.nn.functional as F
from torch_geometric.data import HeteroData
from torch_geometric.loader import DataLoader
from torch_geometric.nn import HeteroConv, GINConv, SAGEConv, global_add_pool
from sklearn.metrics import roc_auc_score

TRAIN_DIR = 'train'
VAL_DIR = 'val'
TEST_DIR = 'test'

LOCK_NODE_DIM = 4      # [is_static, is_sync, nesting, pts_size]
ACQ_NODE_DIM = 4       # 同上
LOCK_EDGE_DIM = 4      # [same_class, call_distance, may_alias, same_lock_obj]
CALL_EDGE_DIM = 4      # [same_class, call_distance, same_thread, is_recursive]
CROSS_EDGE_DIM = 1     # 跨层边特征（常数 1）


# ============================================================
# 1. 解析 graph-*.txt -> List[HeteroData]
# ============================================================
def _parse_bool(s: str) -> int:
    return int(s.strip().lower() == "true")

def parse_graph_file(path: str) -> List[HeteroData]:
    datasets = []

    cur_lock_x = []
    cur_acq_x = []
    cur_lock_edges = []
    cur_call_edges = []
    cur_acq_to_lock = []
    cur_lock_to_acq = []
    cur_label = None
    cycle_id = -1

    lock_id_map = {}
    acq_id_map = {}

    def flush():
        nonlocal cur_lock_x, cur_acq_x, cur_lock_edges, cur_call_edges
        nonlocal cur_acq_to_lock, cur_lock_to_acq, cur_label, cycle_id
        nonlocal lock_id_map, acq_id_map

        # 完全空块：静默跳过
        if not cur_lock_x and not cur_acq_x and cur_label is None:
            return

        if cur_label is None:
            print(f"[warn] {path} cycle {cycle_id} missing LABEL_CYCLE, skip")
        else:
            data = HeteroData()

            # ----- lock 节点 -----
            if cur_lock_x:
                data['lock'].x = torch.tensor(cur_lock_x, dtype=torch.float)
            else:
                data['lock'].x = torch.empty((0, LOCK_NODE_DIM), dtype=torch.float)

            # ----- acq 节点 -----
            if cur_acq_x:
                data['acq'].x = torch.tensor(cur_acq_x, dtype=torch.float)
            else:
                data['acq'].x = torch.empty((0, ACQ_NODE_DIM), dtype=torch.float)

            # ----- lock -> lock -----
            if cur_lock_edges:
                ei = torch.tensor([[e[0], e[1]] for e in cur_lock_edges],
                                  dtype=torch.long).t().contiguous()
                ea = torch.tensor([e[2] for e in cur_lock_edges], dtype=torch.float)
            else:
                ei = torch.empty((2, 0), dtype=torch.long)
                ea = torch.empty((0, LOCK_EDGE_DIM), dtype=torch.float)
            data['lock', 'lock_edge', 'lock'].edge_index = ei
            data['lock', 'lock_edge', 'lock'].edge_attr = ea

            # ----- acq -> acq -----
            if cur_call_edges:
                ei = torch.tensor([[e[0], e[1]] for e in cur_call_edges],
                                  dtype=torch.long).t().contiguous()
                ea = torch.tensor([e[2] for e in cur_call_edges], dtype=torch.float)
            else:
                ei = torch.empty((2, 0), dtype=torch.long)
                ea = torch.empty((0, CALL_EDGE_DIM), dtype=torch.float)
            data['acq', 'call_edge', 'acq'].edge_index = ei
            data['acq', 'call_edge', 'acq'].edge_attr = ea

            # ----- acq -> lock -----
            if cur_acq_to_lock:
                ei = torch.tensor(cur_acq_to_lock, dtype=torch.long).t().contiguous()
                ea = torch.ones((ei.size(1), CROSS_EDGE_DIM), dtype=torch.float)
            else:
                ei = torch.empty((2, 0), dtype=torch.long)
                ea = torch.empty((0, CROSS_EDGE_DIM), dtype=torch.float)
            data['acq', 'acq_to_lock', 'lock'].edge_index = ei
            data['acq', 'acq_to_lock', 'lock'].edge_attr = ea

            # ----- lock -> acq -----
            if cur_lock_to_acq:
                ei = torch.tensor(cur_lock_to_acq, dtype=torch.long).t().contiguous()
                ea = torch.ones((ei.size(1), CROSS_EDGE_DIM), dtype=torch.float)
            else:
                ei = torch.empty((2, 0), dtype=torch.long)
                ea = torch.empty((0, CROSS_EDGE_DIM), dtype=torch.float)
            data['lock', 'lock_to_acq', 'acq'].edge_index = ei
            data['lock', 'lock_to_acq', 'acq'].edge_attr = ea

            data.y = torch.tensor([cur_label], dtype=torch.float)
            data.cycle_id = cycle_id
            datasets.append(data)

        # 重置
        cur_lock_x, cur_acq_x = [], []
        cur_lock_edges, cur_call_edges = [], []
        cur_acq_to_lock, cur_lock_to_acq = [], []
        lock_id_map, acq_id_map = {}, {}
        cur_label = None
        cycle_id = -1

    with open(path, "r", encoding="utf-8") as f:
        for raw in f:
            line = raw.strip()

            # ★ 关键修复：空行和注释行直接跳过，不触发 flush
            if not line:
                continue
            if line.startswith("#"):
                continue

            parts = line.split(" ", 1)
            tag = parts[0]
            rest = parts[1] if len(parts) > 1 else ""

            # ★ 关键修复：遇到新 CYCLE 时先 flush 上一个
            if tag == "CYCLE":
                flush()
                cycle_id = int(rest)
                continue

            # ---------------- 节点 ----------------
            if tag == "LOCK_NODE":
                fields = [x.strip() for x in rest.split(",")]
                node_id = int(fields[0])
                is_static = _parse_bool(fields[3]) if len(fields) > 3 else 0
                pts_size = int(fields[4]) if len(fields) > 4 else 1
                lock_id_map[node_id] = len(cur_lock_x)
                cur_lock_x.append([is_static, 1, 1, pts_size])

            elif tag == "ACQ_NODE":
                fields = [x.strip() for x in rest.split(",")]
                node_id = int(fields[0])
                is_static = _parse_bool(fields[4]) if len(fields) > 4 else 0
                is_sync = _parse_bool(fields[5]) if len(fields) > 5 else 1
                nesting = int(fields[6]) if len(fields) > 6 else 1
                pts_size = int(fields[7]) if len(fields) > 7 else 1
                acq_id_map[node_id] = len(cur_acq_x)
                cur_acq_x.append([is_static, is_sync, nesting, pts_size])

            elif tag == "LOCK_EDGE":
                fields = [x.strip() for x in rest.split(",")]
                u = lock_id_map.get(int(fields[0]))
                v = lock_id_map.get(int(fields[1]))
                if u is None or v is None:
                    continue
                call_dist = float(fields[6]) if len(fields) > 6 else 0.0
                may_alias = _parse_bool(fields[7]) if len(fields) > 7 else 0
                same_obj = _parse_bool(fields[8]) if len(fields) > 8 else 0
                cur_lock_edges.append((u, v, [0.0, call_dist, float(may_alias), float(same_obj)]))

            elif tag == "CALL_EDGE":
                fields = [x.strip() for x in rest.split(",")]
                u = acq_id_map.get(int(fields[0]))
                v = acq_id_map.get(int(fields[1]))
                if u is None or v is None:
                    continue
                call_dist = float(fields[2]) if len(fields) > 2 else 0.0
                same_thread = _parse_bool(fields[3]) if len(fields) > 3 else 0
                recursive = _parse_bool(fields[4]) if len(fields) > 4 else 0
                cur_call_edges.append((u, v, [0.0, call_dist, float(same_thread), float(recursive)]))

            elif tag == "ACQ_TO_LOCK":
                fields = [x.strip() for x in rest.split(",")]
                a = acq_id_map.get(int(fields[0]))
                l = lock_id_map.get(int(fields[1]))
                if a is None or l is None:
                    continue
                cur_acq_to_lock.append((a, l))

            elif tag == "LOCK_TO_ACQ":
                fields = [x.strip() for x in rest.split(",")]
                l = lock_id_map.get(int(fields[0]))
                a = acq_id_map.get(int(fields[1]))
                if l is None or a is None:
                    continue
                cur_lock_to_acq.append((l, a))

            elif tag in ("LABEL", "LABEL_CYCLE"):
                cur_label = int(rest)

            elif tag.startswith("LABEL_"):
                pass  # LABEL_LOCK_NODES / LABEL_LOCK_EDGES / LABEL_ACQ_NODES 忽略

        # 文件结束，flush 最后一个 cycle
        flush()

    return datasets


def load_all_graphs(graph_dir: str, tag: str = "") -> List[HeteroData]:
    files = sorted(glob.glob(os.path.join(graph_dir, "graph-*.txt")))
    print(f"[load{tag}] found {len(files)} graph files in {graph_dir}")
    all_data = []
    for fp in files:
        data_list = parse_graph_file(fp)
        print(f"[load{tag}] {os.path.basename(fp)}: {len(data_list)} cycles")
        all_data.extend(data_list)
    print(f"[load{tag}] total cycles: {len(all_data)}")
    return all_data


# ============================================================
# 2. 异构 GNN 模型
# ============================================================
class HeteroGINClassifier(nn.Module):
    def __init__(self,
                 lock_dim=LOCK_NODE_DIM,
                 acq_dim=ACQ_NODE_DIM,
                 lock_edge_dim=LOCK_EDGE_DIM,
                 call_edge_dim=CALL_EDGE_DIM,
                 cross_edge_dim=CROSS_EDGE_DIM,
                 hidden_dim=64,
                 num_layers=3):
        super().__init__()

        self.lock_encoder = nn.Linear(lock_dim, hidden_dim)
        self.acq_encoder = nn.Linear(acq_dim, hidden_dim)

        # 每种边类型独立的 edge encoder
        self.lock_edge_encoder = nn.Linear(lock_edge_dim, hidden_dim)
        self.call_edge_encoder = nn.Linear(call_edge_dim, hidden_dim)
        self.cross_edge_encoder = nn.Linear(cross_edge_dim, hidden_dim)

        # 边类型元组
        self.edge_types = [
            ('lock', 'lock_edge', 'lock'),
            ('acq', 'call_edge', 'acq'),
            ('acq', 'acq_to_lock', 'lock'),
            ('lock', 'lock_to_acq', 'acq'),
        ]

        self.convs = nn.ModuleList()
        self.norms = nn.ModuleList()

        for _ in range(num_layers):
            conv_dict = {}
            for et in self.edge_types:
                mlp = nn.Sequential(
                    nn.Linear(hidden_dim, hidden_dim),
                    nn.ReLU(),
                    nn.Linear(hidden_dim, hidden_dim),
                )
                if et in [('lock', 'lock_edge', 'lock'), ('acq', 'call_edge', 'acq')]:
                    conv_dict[et] = GINConv(mlp)
                else:
                    conv_dict[et] = SAGEConv(hidden_dim, hidden_dim)
            self.convs.append(HeteroConv(conv_dict, aggr='sum'))
            # 用 LayerNorm 避免 BN 在 batch 内某类节点为 0 时报错
            self.norms.append(nn.ModuleDict({
                'lock': nn.LayerNorm(hidden_dim),
                'acq': nn.LayerNorm(hidden_dim),
            }))

        self.classifier = nn.Sequential(
            nn.Linear(hidden_dim * 2, hidden_dim),
            nn.ReLU(),
            nn.Dropout(0.2),
            nn.Linear(hidden_dim, 1),
        )

    def _encode_edges(self, data, device):
        x_dict = {
            'lock': self.lock_encoder(data['lock'].x.to(device)),
            'acq': self.acq_encoder(data['acq'].x.to(device)),
        }

        # lock -> lock
        et = ('lock', 'lock_edge', 'lock')
        ei = data[et].edge_index.to(device)
        ea = data[et].edge_attr.to(device)
        if ea.numel() > 0:
            e = self.lock_edge_encoder(ea)
            x_dict['lock'] = x_dict['lock'].index_add(0, ei[0], e)

        # acq -> acq
        et = ('acq', 'call_edge', 'acq')
        ei = data[et].edge_index.to(device)
        ea = data[et].edge_attr.to(device)
        if ea.numel() > 0:
            e = self.call_edge_encoder(ea)
            x_dict['acq'] = x_dict['acq'].index_add(0, ei[0], e)

        # acq -> lock
        et = ('acq', 'acq_to_lock', 'lock')
        ei = data[et].edge_index.to(device)
        ea = data[et].edge_attr.to(device)
        if ea.numel() > 0:
            e = self.cross_edge_encoder(ea)
            x_dict['acq'] = x_dict['acq'].index_add(0, ei[0], e)

        # lock -> acq
        et = ('lock', 'lock_to_acq', 'acq')
        ei = data[et].edge_index.to(device)
        ea = data[et].edge_attr.to(device)
        if ea.numel() > 0:
            e = self.cross_edge_encoder(ea)
            x_dict['lock'] = x_dict['lock'].index_add(0, ei[0], e)

        return x_dict

    def forward(self, data):
        device = data['lock'].x.device
        batch_dict = {
            'lock': data['lock'].batch,
            'acq': data['acq'].batch,
        }

        x_dict = self._encode_edges(data, device)
        edge_index_dict = {et: data[et].edge_index.to(device) for et in self.edge_types}

        for conv, norm in zip(self.convs, self.norms):
            out_dict = conv(x_dict, edge_index_dict)
            new_x = {}
            for ntype in ['lock', 'acq']:
                h = out_dict.get(ntype, x_dict[ntype])
                if h.size(0) > 0:
                    h = norm[ntype](h)
                h = F.relu(h)
                new_x[ntype] = h
            x_dict = new_x

        g_lock = global_add_pool(x_dict['lock'], batch_dict['lock'])
        g_acq = global_add_pool(x_dict['acq'], batch_dict['acq'])
        g = torch.cat([g_lock, g_acq], dim=-1)

        return self.classifier(g).squeeze(-1)


# ============================================================
# 3. 训练 / 评估
# ============================================================
def train_epoch(model, loader, optimizer, criterion, device):
    model.train()
    total_loss, total = 0.0, 0
    for data in loader:
        data = data.to(device)
        optimizer.zero_grad()
        logit = model(data)
        y = data.y.view(-1).to(device)
        loss = criterion(logit, y)
        loss.backward()
        optimizer.step()
        total_loss += loss.item() * data.num_graphs
        total += data.num_graphs
    return total_loss / max(total, 1)


def compute_auc_roc(probs, labels):
    if len(labels) == 0 or labels.sum() == 0 or labels.sum() == len(labels):
        return float("nan")
    try:
        return roc_auc_score(labels.numpy(), probs.numpy())
    except ValueError:
        return float("nan")


def compute_precision_at_k(probs, labels, k):
    if len(labels) == 0:
        return 0.0
    k = min(k, len(labels))
    _, topk_idx = torch.topk(probs, k)
    return labels[topk_idx].sum().item() / k


def compute_recall_at_k(probs, labels, k):
    total_pos = labels.sum().item()
    if total_pos == 0:
        return 0.0
    k = min(k, len(labels))
    _, topk_idx = torch.topk(probs, k)
    return labels[topk_idx].sum().item() / total_pos


def compute_mrr(probs, labels):
    if labels.sum() == 0:
        return 0.0
    sorted_idx = torch.argsort(probs, descending=True)
    sorted_labels = labels[sorted_idx]
    for rank, lab in enumerate(sorted_labels.tolist(), start=1):
        if lab == 1:
            return 1.0 / rank
    return 0.0


def compute_all_ranking_metrics(probs, labels, ks=(5, 10, 20)):
    metrics = {
        "auc_roc": compute_auc_roc(probs, labels),
        "mrr": compute_mrr(probs, labels),
    }
    for k in ks:
        metrics[f"p@{k}"] = compute_precision_at_k(probs, labels, k)
        metrics[f"r@{k}"] = compute_recall_at_k(probs, labels, k)
    return metrics


@torch.no_grad()
def eval_epoch(model, loader, criterion, device, threshold=0.5):
    model.eval()
    total_loss, total = 0.0, 0
    probs, labels = [], []
    for data in loader:
        data = data.to(device)
        logit = model(data)
        y = data.y.view(-1).to(device)
        loss = criterion(logit, y)
        total_loss += loss.item() * data.num_graphs
        total += data.num_graphs
        probs.append(torch.sigmoid(logit).cpu())
        labels.append(y.cpu())
    probs = torch.cat(probs) if probs else torch.tensor([])
    labels = torch.cat(labels) if labels else torch.tensor([])

    preds = (probs >= threshold).float()
    acc = (preds == labels).float().mean().item() if len(labels) > 0 else 0.0
    tp = ((preds == 1) & (labels == 1)).sum().item()
    fp = ((preds == 1) & (labels == 0)).sum().item()
    fn = ((preds == 0) & (labels == 1)).sum().item()
    precision = tp / (tp + fp) if (tp + fp) > 0 else 0.0
    recall = tp / (tp + fn) if (tp + fn) > 0 else 0.0
    f1 = 2 * precision * recall / (precision + recall) if (precision + recall) > 0 else 0.0

    ranking = compute_all_ranking_metrics(probs, labels, ks=(5, 10, 20))
    return (total_loss / max(total, 1), acc, precision, recall, f1,
            probs, labels, ranking)


# ============================================================
# 4. 主程序
# ============================================================
def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--epochs", type=int, default=100)
    parser.add_argument("--batch_size", type=int, default=32)
    parser.add_argument("--lr", type=float, default=1e-3)
    parser.add_argument("--hidden_dim", type=int, default=64)
    parser.add_argument("--num_layers", type=int, default=3)
    parser.add_argument("--threshold", type=float, default=0.5)
    parser.add_argument("--seed", type=int, default=42)
    parser.add_argument("--save", type=str, default="hetero_gin_deadlock.pt")
    args = parser.parse_args()

    torch.manual_seed(args.seed)
    random.seed(args.seed)

    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    print(f"[device] {device}")

    train_data = load_all_graphs(TRAIN_DIR, tag="/train")
    val_data = load_all_graphs(VAL_DIR, tag="/val")
    test_data = load_all_graphs(TEST_DIR, tag="/test")
    if len(train_data) == 0 or len(test_data) == 0:
        print("[error] no graph data found")
        return

    for name, dl in [("train", train_data), ("val", val_data), ("test", test_data)]:
        n_pos = sum(1 for d in dl if d.y.item() == 1)
        n_neg = len(dl) - n_pos
        print(f"[label/{name}] positive={n_pos}, negative={n_neg}")

    train_loader = DataLoader(train_data, batch_size=args.batch_size, shuffle=True)
    val_loader = DataLoader(val_data, batch_size=args.batch_size, shuffle=False)
    test_loader = DataLoader(test_data, batch_size=args.batch_size, shuffle=False)

    model = HeteroGINClassifier(
        hidden_dim=args.hidden_dim,
        num_layers=args.num_layers,
    ).to(device)

    n_pos_train = sum(1 for d in train_data if d.y.item() == 1)
    n_neg_train = len(train_data) - n_pos_train
    if n_pos_train > 0 and n_neg_train > 0:
        pos_weight = torch.tensor([n_neg_train / n_pos_train], dtype=torch.float).to(device)
    else:
        pos_weight = torch.tensor([1.0], dtype=torch.float).to(device)
    criterion = nn.BCEWithLogitsLoss(pos_weight=pos_weight)
    optimizer = torch.optim.Adam(model.parameters(), lr=args.lr)
    scheduler = torch.optim.lr_scheduler.StepLR(optimizer, step_size=30, gamma=0.5)

    best_f1, best_state = 0.0, None
    for epoch in range(1, args.epochs + 1):
        train_loss = train_epoch(model, train_loader, optimizer, criterion, device)
        (val_loss, val_acc, val_p, val_r, val_f1,
         _, _, val_rank) = eval_epoch(model, val_loader, criterion, device,
                                      threshold=args.threshold)
        scheduler.step()

        print(f"[epoch {epoch:03d}] "
              f"train_loss={train_loss:.4f} "
              f"val_loss={val_loss:.4f} "
              f"val_acc={val_acc:.4f} "
              f"val_p={val_p:.4f} val_r={val_r:.4f} val_f1={val_f1:.4f} "
              f"val_auc={val_rank['auc_roc']:.4f} "
              f"val_p@10={val_rank['p@10']:.4f} "
              f"val_mrr={val_rank['mrr']:.4f}")

        if val_f1 > best_f1:
            best_f1 = val_f1
            best_state = {k: v.detach().cpu().clone()
                          for k, v in model.state_dict().items()}

    if best_state is not None:
        model.load_state_dict(best_state)
    (test_loss, test_acc, test_p, test_r, test_f1,
     probs, labels, test_rank) = eval_epoch(model, test_loader, criterion,
                                            device, threshold=args.threshold)

    print("\n=== Test ===")
    print(f"loss={test_loss:.4f} acc={test_acc:.4f} "
          f"precision={test_p:.4f} recall={test_r:.4f} f1={test_f1:.4f}")
    print("\n=== Ranking Metrics ===")
    print(f"AUC-ROC = {test_rank['auc_roc']:.4f}")
    print(f"MRR     = {test_rank['mrr']:.4f}")
    for k in (5, 10, 20):
        print(f"P@{k:<2d}    = {test_rank[f'p@{k}']:.4f}   "
              f"R@{k:<2d} = {test_rank[f'r@{k}']:.4f}")

    print("\n=== Per-sample probabilities ===")
    for i, (p, y) in enumerate(zip(probs.tolist(), labels.tolist())):
        print(f"  sample {i}: prob={p:.4f}, label={int(y)}, "
              f"pred={int(p >= args.threshold)}")

    torch.save({
        "model_state": model.state_dict(),
        "args": vars(args),
    }, args.save)
    print(f"\n[save] model saved to {args.save}")


if __name__ == "__main__":
    main()
