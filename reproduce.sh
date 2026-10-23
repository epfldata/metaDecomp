#!/usr/bin/env bash
set -euo pipefail
ROOT=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
cd "$ROOT"
STAGE=${1:-all}
BENCHMARK=${2:-}
QUERY_REGEX=${3:-.*}
if [[ "$STAGE" == --help ]]; then
  echo 'Usage: bash reproduce.sh [all|setup|experiments|figures|paper] [benchmark] [query filename regex]'
  echo 'See README.md for Docker, memory settings, selective runs, and resuming a run.'
  exit 0
fi
case "$STAGE" in all|setup|experiments|figures|paper) ;; *) echo "Unknown stage: $STAGE" >&2; exit 1 ;; esac
if [[ "$STAGE" == all && ( -n "$BENCHMARK" || "$QUERY_REGEX" != '.*' ) ]]; then
  echo 'Use the experiments stage for a query subset; the paper plots require all benchmarks.' >&2
  exit 1
fi
export RESULTS_DIR=${RESULTS_DIR:-$ROOT/experiment-results/runs/$(date +%Y-%m-%dT%H-%M-%S)}
mkdir -p "$RESULTS_DIR/join-trees"
RESULTS_DIR=$(cd "$RESULTS_DIR" && pwd)
export RESULTS_DIR
export JVM_MEMORY=${JVM_MEMORY:-40g}
export DUCKDB_MEMORY=${DUCKDB_MEMORY:-40g}
export REPEAT_TIMES=${REPEAT_TIMES:-10}
export REWRITE_MODE=${REWRITE_MODE:-saved}
case "$REWRITE_MODE" in saved|fresh) ;; *) echo "Unknown REWRITE_MODE: $REWRITE_MODE" >&2; exit 1 ;; esac
export MPLBACKEND=Agg
PYTHON="$ROOT/src/main/python/venv/bin/python"
# Save console output alongside measurements, including setup failures.
exec > >(tee -a "$RESULTS_DIR/pipeline.log") 2>&1
printf 'Results: %s\n' "$RESULTS_DIR"

# Each system/benchmark is a restartable unit. Reuse RESULTS_DIR to keep other
# units; rerunning a unit writes a new CSV and replaces its normalized copy.
SYSTEM_LIST=(${SYSTEMS:-metadecomp dpconv duckdb uniondp yanplus llm-r2 learned-rewrite enum-metadecomp enum-sparksqlplus})
BENCHMARK_LIST=(dsb job-original job-large musicbrainz)
if [[ -n "$BENCHMARK" ]]; then BENCHMARK_LIST=("$BENCHMARK"); fi

# Quote commands so they can be copied directly, including paths and regexes.
# In a container, print Docker Compose commands usable from the host checkout.
resume_command() {
  local stage=$1 system=${2:-} benchmark=${3:-$BENCHMARK}
  local command=(env "RESULTS_DIR=$RESULTS_DIR" "JVM_MEMORY=$JVM_MEMORY"
    "DUCKDB_MEMORY=$DUCKDB_MEMORY" "REPEAT_TIMES=$REPEAT_TIMES"
    "REWRITE_MODE=$REWRITE_MODE" "BUILD_JOBS=${BUILD_JOBS:-2}" "ENUM_TIMEOUT_SECONDS=${ENUM_TIMEOUT_SECONDS:-3600}")
  if [[ "$stage" == experiments && -z "$system" ]]; then system="${SYSTEM_LIST[*]}"; fi
  if [[ -n "$system" ]]; then command+=("SYSTEMS=$system"); fi
  if [[ -f /.dockerenv ]]; then
    command+=("DOCKER_MEMORY=${DOCKER_MEMORY:-48g}" docker compose run --rm artifact)
  else
    command+=(bash reproduce.sh)
  fi
  command+=("$stage")
  if [[ "$stage" == experiments && -n "$benchmark" ]]; then command+=("$benchmark" "$QUERY_REGEX"); fi
  printf '%q ' "${command[@]}"
}
CURRENT_COMMAND="$(resume_command "$STAGE")"
# On failure or interruption, the last announced command identifies the unit
# to restart. Continue with the later commands in the plan, then figures/paper.
trap 'status=$?; if (( status != 0 )); then printf "\nStopped (exit %s). Restart the current unit from the repository root:\n%s\n" "$status" "$CURRENT_COMMAND"; fi' EXIT
announce() {
  CURRENT_COMMAND=$(resume_command "$2" "${3:-}" "${4:-$BENCHMARK}")
  printf '\n[%s] %s\nCommand: %s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "$1" "$CURRENT_COMMAND"
}

# Download data and build dependencies. Manual: bash reproduce.sh setup
run_setup() {
  announce "Setup: benchmark data" setup
  bash src/main/scripts/setup.sh
  announce "Setup: baseline builds, Python dependencies, and Scala application" setup
  bash src/main/scripts/setup-systems.sh
}
# Run one system/benchmark at a time. For example:
# SYSTEMS=duckdb bash reproduce.sh experiments job-original '01a[.]sql'
run_experiments() {
  # Print the full plan first: later commands remain available in pipeline.log
  # even when a long-running unit is interrupted.
  echo 'Experiment plan (each command restarts one complete system/benchmark):'
  for system in "${SYSTEM_LIST[@]}"; do
    for benchmark in "${BENCHMARK_LIST[@]}"; do
      printf '  %s / %s: ' "$system" "$benchmark"
      resume_command experiments "$system" "$benchmark"; echo
    done
  done
  echo 'After the experiment units, generate figures and compile the paper:'
  resume_command figures; echo
  resume_command paper; echo
  if [[ "$REWRITE_MODE" == fresh && ( " ${SYSTEM_LIST[*]} " == *" llm-r2 "* || " ${SYSTEM_LIST[*]} " == *" learned-rewrite "* ) ]]; then
    if [[ " ${SYSTEM_LIST[*]} " == *" llm-r2 "* && -z "${OPENAI_API_KEY:-}" ]]; then
      echo 'Set OPENAI_API_KEY to generate fresh LLM-R2 rewrites.' >&2
      exit 1
    fi
    if [[ ! -f llm-r2/rewriter_java.jar ]]; then
      echo 'Fresh rewrites need rewriter_java.jar from the updated LLM-R2 ZIP.' >&2
      exit 1
    fi
    if [[ " ${SYSTEM_LIST[*]} " == *" llm-r2 "* && ! -f llm-r2/simcse_result/sup-train-42-8-0/pytorch_model.bin ]]; then
      echo 'Fresh LLM-R2 rewrites need the trained checkpoint from the updated ZIP.' >&2
      exit 1
    fi
    announce "Experiments: install rewrite dependencies" experiments
    python3 -m venv llm-r2/venv
    llm-r2/venv/bin/pip install -r llm-r2/requirements.txt
  fi
  announce "Experiments: stage the Scala application" experiments
  sbt "-J-Xmx${JVM_MEMORY}" stage
  {
    date -u
    uname -a
    java -version 2>&1
    echo "benchmark=$BENCHMARK query_regex=$QUERY_REGEX"
    echo "REPEAT_TIMES=$REPEAT_TIMES JVM_MEMORY=$JVM_MEMORY DUCKDB_MEMORY=$DUCKDB_MEMORY REWRITE_MODE=$REWRITE_MODE"
    if ! revision=$(git rev-parse HEAD 2>/dev/null); then
      revision=unavailable
    fi
    echo "artifact_revision=$revision"
    for system in DPconv duckdb DuckDBYanPlus SparkSQLPlus; do
      if [[ -e "$system/.git" ]]; then echo "$system=$(git -C "$system" rev-parse HEAD)"; fi
    done
  } >> "$RESULTS_DIR/environment.txt"
  unit=0
  total=$((${#SYSTEM_LIST[@]} * ${#BENCHMARK_LIST[@]}))
  for system in "${SYSTEM_LIST[@]}"; do
    for benchmark in "${BENCHMARK_LIST[@]}"; do
      unit=$((unit + 1))
      announce "Experiment $unit/$total: $system / $benchmark (query regex: $QUERY_REGEX)" experiments "$system" "$benchmark"
      case "$system" in
        metadecomp) runner=MetaDecompRunner ;;
        dpconv|uniondp) runner=DPconvRunner ;;
        duckdb) runner=DuckDBRunner ;;
        yanplus) runner=DuckDBYanPlusRunner ;;
        llm-r2) runner=LLMR2Runner ;;
        learned-rewrite) runner=LearnedRewriteRunner ;;
        enum-metadecomp|enum-sparksqlplus)
          bash "src/main/scripts/enumerate-join-trees-${system#enum-}.sh" "$benchmark" "$QUERY_REGEX"
          echo "Completed: $system / $benchmark"
          continue ;;
        *) echo "Unknown system: $system" >&2; exit 1 ;;
      esac
      args=("$benchmark" "$QUERY_REGEX")
      if [[ "$system" == uniondp ]]; then args=(UnionDP "${args[@]}"); fi
      # Fresh mode regenerates the selected SQL and optimization timings first.
      # Saved mode reads the CSVs in the downloaded LLM-R2 directory.
      if [[ "$REWRITE_MODE" == fresh && ( "$system" == llm-r2 || "$system" == learned-rewrite ) ]]; then
        if [[ "$system" == llm-r2 ]]; then
          script=llm_r2_rewriter_pg.py
        else
          script=learned_rewriter_pg.py
        fi
        (cd llm-r2 && venv/bin/python -u "$script" "$benchmark" "$QUERY_REGEX")
      fi
      java -Xms512m "-Xmx${JVM_MEMORY}" -cp 'target/universal/stage/lib/*' "experiments.runner.$runner" "${args[@]}"
      echo "Completed: $system / $benchmark"
    done
  done
  announce "Experiments: organize CSV results" experiments
  python3 src/main/python/organize-results.py
}

# Normalize CSV filenames and generate every paper figure. Manual: bash reproduce.sh figures
run_figures() {
  announce "Figures: organize CSV results" figures
  python3 src/main/python/organize-results.py
  for plot in opt-time cost-ratio overall-speedups-stacked speedups-individual scatter-all scatter-individual enum-time; do
    announce "Figures: $plot" figures
    "$PYTHON" "src/main/python/plot-$plot.py"
  done
}

# Copy paper sources, install the generated figures, and compile. Manual: bash reproduce.sh paper
run_paper() {
  announce "Paper: copy sources, replace figures, and compile main-full.pdf" paper
  # Compile a copy, so the checked-in paper and its original figures stay intact.
  mkdir -p "$RESULTS_DIR/paper"
  cp -R paper-latex/. "$RESULTS_DIR/paper/"
  # Require every experimental PDF used by the full paper to be freshly generated.
  python3 - <<'PYCODE'
import os, re, shutil
from pathlib import Path
results = Path(os.environ['RESULTS_DIR'])
paper = results / 'paper'
for source in (paper / '6_experiments.tex', paper / 'app_exp.tex'):
    for name in re.findall(r'\\includegraphics(?:\[[^]]*\])?\{figs/exp/([^}]+)\}', source.read_text()):
        figure = results / 'figures' / name
        if not figure.is_file():
            raise FileNotFoundError(f'Missing reproduced figure: {figure}')
        target = paper / 'figs/exp' / name
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(figure, target)
PYCODE
  (cd "$RESULTS_DIR/paper" && latexmk -g -pdf -shell-escape -interaction=nonstopmode -halt-on-error main-full.tex)
}

# Choose a stage here; the four functions above can also guide manual runs.
case "$STAGE" in
  all) run_setup; run_experiments; run_figures; run_paper ;;
  setup) run_setup ;;
  experiments) run_experiments ;;
  figures) run_figures ;;
  paper) run_paper ;;
esac
printf 'Completed %s. Outputs: %s\n' "$STAGE" "$RESULTS_DIR"
