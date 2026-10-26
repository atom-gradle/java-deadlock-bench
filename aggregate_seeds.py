# -*- coding: utf-8 -*-

import argparse, glob, json, os
import numpy as np
import pandas as pd
from scipy.stats import ttest_rel, wilcoxon

BASELINES = [
    ("Random Forest", "rf"),
    ("LogisticReg",   "lr"),
    ("XGBoost",       "xgb"),
    ("SVM-RBF",       "svm"),
]

SETTINGS = ["within", "cross"]
REGIMES = ["binary_strong", "three_way"]
METRICS = {
    "binary_strong": ["auc", "f1", "acc"],
    "binary_loose":  ["auc", "f1", "acc"],
    "three_way":     ["f1_macro", "acc"],
}

def load_summaries(root):
    records = {}
    for p in glob.glob(os.path.join(root, "**", "summary-*.json"), recursive=True):
        try:
            with open(p, "r", encoding="utf-8") as f:
                s = json.load(f)
        except Exception as e:
            print(f"[warn] failed to read {p}: {e}")
            continue
        cfg = s.get("config", {})
        regime = cfg.get("label_mode")
        seed = cfg.get("seed")
        if regime is None or seed is None:
            base = os.path.basename(p)
            if base.startswith("summary-") and base.endswith(".json"):
                stem = base[len("summary-"):-len(".json")]
                if "-seed" in stem:
                    regime_part, seed_part = stem.rsplit("-seed", 1)
                    regime = regime or regime_part
                    try:
                        seed = seed if seed is not None else int(seed_part)
                    except ValueError:
                        pass
        if regime is None or seed is None:
            print(f"[warn] cannot infer regime/seed from {p}, skipped")
            continue
        records[(regime, int(seed))] = s
    return records

def seed_metric(summary, setting, tag, metric):
    if tag == "gnn_full":
        search_tag = "gnn_full" if setting == "within" else "gnn"
    else:
        search_tag = tag

    if setting == "within":
        sm = summary.get("seed_metrics") or {}
        return sm.get(search_tag, {}).get(metric)
    elif setting == "cross":
        cross = summary.get("cross_project") or {}
        return cross.get(search_tag, {}).get(metric)
    return None

def paired_test(a, b):
    a = np.asarray(a, dtype=float)
    b = np.asarray(b, dtype=float)
    mask = ~(np.isnan(a) | np.isnan(b))
    a, b = a[mask], b[mask]
    n = len(a)
    if n < 2:
        return {"n": n, "t_stat": None, "p_ttest": None,
                "p_wilcoxon": None, "mean_diff": None,
                "wins": int(np.sum(a > b)), "ties": int(np.sum(a == b))}
    t_stat, p_t = ttest_rel(a, b)
    try:
        _, p_w = wilcoxon(a, b)
        p_w = float(p_w)
    except Exception:
        p_w = None
    return {
        "n": n,
        "t_stat": float(t_stat),
        "p_ttest": float(p_t),
        "p_wilcoxon": p_w,
        "mean_diff": float(np.mean(a - b)),
        "wins": int(np.sum(a > b)),
        "ties": int(np.sum(a == b)),
        "per_seed_diff": [float(x) for x in (a - b)],
    }

def summarize(records, out_dir):
    os.makedirs(out_dir, exist_ok=True)
    all_rows = []
    sig_rows = []

    for setting in SETTINGS:
        for regime in REGIMES:
            seeds = sorted(s for (r, s) in records if r == regime)
            if not seeds:
                continue
            print(f"\n=== {setting.upper()}-PROJECT | {regime} : seeds = {seeds} ===")

            # GNN seed values
            gnn_vals = {m: [] for m in METRICS[regime]}
            for s in seeds:
                for m in METRICS[regime]:
                    gnn_vals[m].append(seed_metric(records[(regime, s)], setting, "gnn_full", m))

            if all(v is None for v in gnn_vals[METRICS[regime][0]]):
                print(f"  [skip] no {setting} data for {regime}")
                continue

            # per-model table
            for model_name, tag in [("HeteroGNN", "gnn_full")] + BASELINES:
                if tag == "gnn_full":
                    vals = gnn_vals
                else:
                    vals = {m: [] for m in METRICS[regime]}
                    for s in seeds:
                        for m in METRICS[regime]:
                            vals[m].append(seed_metric(records[(regime, s)], setting, tag, m))

                row = {"setting": setting, "regime": regime, "model": model_name, "n_seeds": len(seeds)}
                for m in METRICS[regime]:
                    arr = np.asarray([v for v in vals[m] if v is not None], dtype=float)
                    row[f"{m}_mean"] = float(np.mean(arr)) if len(arr) else None
                    row[f"{m}_std"]  = float(np.std(arr, ddof=1)) if len(arr) > 1 else None
                    row[f"{m}_values"] = [float(x) for x in arr]
                all_rows.append(row)

            # significance: GNN vs each baseline, seed-level paired
            for model_name, tag in BASELINES:
                for m in METRICS[regime]:
                    g = [seed_metric(records[(regime, s)], setting, "gnn_full", m) for s in seeds]
                    b = [seed_metric(records[(regime, s)], setting, tag, m) for s in seeds]
                    res = paired_test(g, b)
                    sig_rows.append({
                        "setting": setting,
                        "regime": regime,
                        "metric": m,
                        "baseline": model_name,
                        "baseline_tag": tag,
                        **res,
                    })

    pd.DataFrame(all_rows).to_csv(os.path.join(out_dir, "seed_means.csv"), index=False)
    sig_df = pd.DataFrame(sig_rows)
    sig_df.to_csv(os.path.join(out_dir, "seed_significance.csv"), index=False)

    with open(os.path.join(out_dir, "seed_significance.json"), "w", encoding="utf-8") as f:
        json.dump(sig_rows, f, indent=2, default=str)

    print(f"\n[save] {out_dir}/seed_means.csv")
    print(f"[save] {out_dir}/seed_significance.csv")
    print(f"[save] {out_dir}/seed_significance.json")

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--dir", default=".", help="directory containing summary-*.json")
    ap.add_argument("--out", default="./aggregated", help="output directory")
    args = ap.parse_args()

    records = load_summaries(args.dir)
    print(f"[load] {len(records)} summary files")
    for (r, s) in sorted(records):
        print(f"  - regime={r:14s} seed={s}")

    if not records:
        print("[ERROR] no summary-*.json found. Check --dir.")
        return

    summarize(records, args.out)

if __name__ == "__main__":
    main()
