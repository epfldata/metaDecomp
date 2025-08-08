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
REPEAT_TIMES=10
TIMEOUT_SECONDS=3600 # Timeout in seconds

for BENCHMARK in "job-original" "job-large"; do

  QUERIES_PATH="$PROJECT_ROOT/benchmarks/$BENCHMARK/queries"
  SQL_FILES=$(find "$QUERIES_PATH" -type f -maxdepth 1 -name "*.sql" | sort)

  OUTPUT_CSV="$PROJECT_ROOT/experiment-results/metadecomp-enum-${BENCHMARK}.csv"

  # Write the CSV header
  echo "query,num_rels,time_us" > "$OUTPUT_CSV"

  tmp_file=$(mktemp)
  
  # Loop over all SQL files
  for SQL_FILE in $SQL_FILES; do
    echo "Processing $SQL_FILE..."

    query_name=$(basename "$SQL_FILE" .sql)
    num_rels=0
    times=()
    first_time_timeout=0

    for ((i=1; i<=REPEAT_TIMES; i++)); do
      echo "Run $i for $query_name..."
      # Run the Scala program with a timeout
      gtimeout "$TIMEOUT_SECONDS"s "$PROJECT_ROOT/target/universal/stage/bin/join-tree-enumeration-runner" "-J-Xms512m" "-J-Xmx40g" "$SQL_FILE" > "$tmp_file" 2>&1 &
      TIMEOUT_PID=$!
      wait "$TIMEOUT_PID"
      exit_code=$?
      response=$(cat "$tmp_file")
      
      echo $response

      num_rels=$(echo $response | sed -e "s/^\(.*\),\(.*\)$/\1/g")

      if [ $exit_code -eq 0 ]; then
        times+=($(echo $response | sed -e "s/^\(.*\),\(.*\)$/\2/g"))
      else
        echo "Enumeration timed out for $SQL_FILE."
        times+=("${TIMEOUT_SECONDS}000000")
        if [ $i -eq 1 ]; then
          first_time_timeout=1
          break
        fi
      fi
    done

    if [ $first_time_timeout -eq 1 ]; then
      echo "$query_name,$num_rels,${TIMEOUT_SECONDS}000000" >> "$OUTPUT_CSV"
    else
      # Sort times and get the median
      sorted=($(printf '%s\n' "${times[@]}" | sort -n))
      len=${#sorted[@]}
      if (( $len % 2 == 1 )); then
          median=${sorted[$((len/2))]}
      else
          lo=${sorted[$((len/2-1))]}
          hi=${sorted[$((len/2))]}
          median=$(awk "BEGIN {print ($lo+$hi)/2}")
      fi
      echo "Median time for $query_name: $median us"
      echo "$query_name,$num_rels,$median" >> "$OUTPUT_CSV"
    fi

  done

done
