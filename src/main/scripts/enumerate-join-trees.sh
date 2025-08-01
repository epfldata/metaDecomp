#!/bin/bash

# Exit the script on Ctrl+C
cleanup() {
  echo "Script terminated"
  if [[ -n "$TIMEOUT_PID" ]]; then
    kill -9 "$TIMEOUT_PID"
  fi
  exit 1
}
trap cleanup SIGINT

sbt clean stage
sbt clean stage

# Paths

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"

echo "$PROJECT_ROOT"

for BENCHMARK in "job-original" "job-large"; do

  QUERIES_PATH="$PROJECT_ROOT/benchmarks/$BENCHMARK/queries"
  SQL_FILES=$(find "$QUERIES_PATH" -type f -maxdepth 1 -name "*.sql" | sort)

  OUTPUT_CSV="$PROJECT_ROOT/experiment-results/metadecomp-enum-${BENCHMARK}.csv"
  TIMEOUT=3600 # Timeout in seconds

  # Write the CSV header
  echo "query,num_rels,meta_gyo_time,enum_time,total_time" > "$OUTPUT_CSV"

  # Loop over all SQL files

  for SQL_FILE in $SQL_FILES; do
    RESULT_FILE="${OUTPUT_CSV}"
    echo "Processing $SQL_FILE..."

    # Run the Scala program with a timeout
    gtimeout "$TIMEOUT"s "$PROJECT_ROOT/target/universal/stage/bin/join-tree-enumeration-runner" "-J-Xms512m" "-J-Xmx40g" "$SQL_FILE" "$RESULT_FILE" &
    TIMEOUT_PID=$!

    # Wait for the process to complete
    if [[ -n "$TIMEOUT_PID" ]]; then
      wait "$TIMEOUT_PID"
      EXIT_CODE=$?

      # Check if the program timed out
      if [ $EXIT_CODE -eq 124 ]; then
        echo "Enumeration timed out for $SQL_FILE."
      elif [ $EXIT_CODE -ne 0 ]; then
        echo "An error occurred while running the Scala program for $SQL_FILE."
      else
        echo "Enumeration completed successfully for $SQL_FILE."
      fi
    else
      echo "Failed to start the gtimeout process for $SQL_FILE."
    fi

    echo "" >> "$OUTPUT_CSV"
  done

done
