# Run 15 experiments: 5 seeds x 3 label_modes
# Usage:
#   nohup bash run_all.sh > run_all.log 2>&1 &

set -u

SEEDS=(42 43 44 45 46)
MODES=(binary_strong binary_loose three_way)
SCRIPT="evaluate.py"
LOG_DIR="./run_logs"

mkdir -p "$LOG_DIR"

TOTAL_START=$(date +%s)
echo "============================================================"
echo "Batch run started at: $(date '+%Y-%m-%d %H:%M:%S')"
echo "Seeds: ${SEEDS[*]}"
echo "Modes: ${MODES[*]}"
echo "Total runs: $(( ${#SEEDS[@]} * ${#MODES[@]} ))"
echo "============================================================"

i=0
total=$(( ${#SEEDS[@]} * ${#MODES[@]} ))

for seed in "${SEEDS[@]}"; do
    for mode in "${MODES[@]}"; do
        i=$((i + 1))
        tag="seed${seed}_${mode}"
        log_file="${LOG_DIR}/${tag}.log"

        echo ""
        echo "------------------------------------------------------------"
        echo "[$i/$total] seed=$seed  label_mode=$mode"
        echo "  start: $(date '+%Y-%m-%d %H:%M:%S')"
        echo "  log:   $log_file"
        echo "------------------------------------------------------------"

        run_start=$(date +%s)

        python "$SCRIPT" --seed "$seed" --label_mode "$mode" --do_gnn_ablation \
            > "$log_file" 2>&1

        exit_code=$?
        run_end=$(date +%s)
        run_sec=$((run_end - run_start))

        if [ $exit_code -eq 0 ]; then
            echo "  done:  $(date '+%Y-%m-%d %H:%M:%S')  (${run_sec}s)  [OK]"
        else
            echo "  FAILED (exit=$exit_code)  (${run_sec}s)"
        fi
    done
done

TOTAL_END=$(date +%s)
TOTAL_SEC=$((TOTAL_END - TOTAL_START))
TOTAL_MIN=$((TOTAL_SEC / 60))

echo ""
echo "============================================================"
echo "Batch run finished at: $(date '+%Y-%m-%d %H:%M:%S')"
echo "Total time: ${TOTAL_MIN} min (${TOTAL_SEC}s)"
echo "Logs saved in: $LOG_DIR"
echo "============================================================"