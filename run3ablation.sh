#!/bin/bash

LABEL_MODES="binary_strong"
SEEDS="42 43 44"

for S in $SEEDS; do
    for L in $LABEL_MODES; do
        echo "============================================"
        echo "Running: python evaluate4.py --label_mode $L --seed $S" --do_gnn_ablation
        echo "============================================"
        python evaluate4.py --label_mode "$L" --seed "$S" --do_gnn_ablation
        if [ $? -ne 0 ]; then
            echo "[ERROR] Failed for label_mode=$L seed=$S"
        fi
    done
done

echo "All runs completed."