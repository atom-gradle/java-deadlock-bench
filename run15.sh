#!/bin/bash

LABEL_MODES="binary_strong binary_loose three_way"
SEEDS="42 43 44 45 46"

for S in $SEEDS; do
    for L in $LABEL_MODES; do
        echo "============================================"
        echo "Running: python evaluate4.py --label_mode $L --seed $S"
        echo "============================================"
        python evaluate4.py --label_mode "$L" --seed "$S"
        if [ $? -ne 0 ]; then
            echo "[ERROR] Failed for label_mode=$L seed=$S"
        fi
    done
done

echo "All runs completed."