#!/bin/bash

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"

# --- Configuration ---
DDL_FILE="$PROJECT_ROOT/replacement-files/SparkSQLPlus/scheme.sql"
API_URL="http://localhost:8848/api/v1/parse"
TIMEOUT_SECONDS=3600 # 1 hour
REPEAT_TIMES=10

# --- Pre-flight Checks ---

# Check if the DDL file exists
if [ ! -f "$DDL_FILE" ]; then
    echo "Error: DDL file '$DDL_FILE' not found in the current directory."
    exit 1
fi

# Check if jq is installed, as it's required for processing the JSON response
if ! command -v jq &> /dev/null; then
    echo "Error: 'jq' is not installed. Please install it to process the JSON response."
    echo "Installation instructions: https://jqlang.github.io/jq/download/"
    exit 1
fi

echo "Found DDL file: $DDL_FILE. Processing other .sql files..."

# --- Start Spring Boot Application ---
APP_JAR="$PROJECT_ROOT/SparkSQLPlus/sqlplus-web/target/sparksql-plus-web-jar-with-dependencies.jar"
APP_LOG="$PROJECT_ROOT/SparkSQLPlus/app.log"
APP_PID_FILE="$PROJECT_ROOT/SparkSQLPlus/app.pid"

# Start the application in the background and redirect output to a log file
java -Xms256m -Xmx40g -jar "$APP_JAR" > "$APP_LOG" 2>&1 &
APP_PID=$!
echo $APP_PID > "$APP_PID_FILE"

# Wait for the application to start
while ! grep -q "Started Application in" "$APP_LOG"; do
    sleep 1
done

# --- Function to Restart Spring Boot Application ---
restart_app() {
    echo "Restarting Spring Boot application..."

    if [ -f "$APP_PID_FILE" ]; then
        kill -9 $(cat "$APP_PID_FILE")
        rm "$APP_PID_FILE"
    fi

    sleep 10

    # Clear the log file
    > "$APP_LOG"

    java -Xms256m -Xmx40g -jar "$APP_JAR" > "$APP_LOG" 2>&1 &
    APP_PID=$!
    echo $APP_PID > "$APP_PID_FILE"

    # Wait for the application to start
    while ! grep -q "Started Application in" "$APP_LOG"; do
        if grep -q "Web server failed to start" "$APP_LOG"; then
            restart_app
            return
        fi
        sleep 1
    done
    echo "Spring Boot application restarted successfully."
}

# Exit the script on Ctrl+C
cleanup() {
    echo "Script terminated"
    if [ -f "$APP_PID_FILE" ]; then
        kill -9 $(cat "$APP_PID_FILE")
        rm "$APP_PID_FILE"
    fi
    exit 1
}
trap cleanup SIGINT

# --- Main Processing Loop ---

# Read the content of the DDL file once
# Escapes newlines and double quotes for JSON compatibility
ddl_content=$(cat "$DDL_FILE" | sed 's/"/\\"/g' | sed -e ':a' -e 'N' -e '$!ba' -e 's/\n/\\n/g')

for BENCHMARK in "job-original" "job-large"; do

    output_csv_file="$PROJECT_ROOT/experiment-results/sparksqlplus-enum-$BENCHMARK.csv"
    echo "query,time_ms" > $output_csv_file

    # Loop through all .sql files in the current directory
    for query_name in $(find "$PROJECT_ROOT/benchmarks/$BENCHMARK/queries" -maxdepth 1 -type f \( -name "*.sql" \) -exec basename {} \; | sort); do
        sql_file="$PROJECT_ROOT/benchmarks/$BENCHMARK/queries/$query_name"
        # Skip the DDL file itself
        if [ "$sql_file" == "$DDL_FILE" ]; then
            continue
        fi

        echo "----------------------------------------"
        echo "Processing query file: $sql_file"

        # Define the output CSV file name based on the SQL file name
        # output_csv_file="result.csv"

        # Read the content of the query file
        # Escapes newlines and double quotes for JSON compatibility
        sql_content=$(cat "$sql_file" | sed 's/"/\\"/g' | sed 's/;//g' | sed -e ':a' -e 'N' -e '$!ba' -e 's/\n/\\n/g')

        # Create the JSON payload
        json_payload=$(printf '{"query": "%s", "ddl": "%s"}' "$sql_content" "$ddl_content")

        # Make the API call using curl
        # -m: sets the maximum time in seconds for the request
        # -s: silent mode (don't show progress meter)
        # -S: show error message on failure
        # The response from curl is piped directly to jq for conversion to CSV
        #
        # NOTE: The jq filter `(.[0] | keys_unsorted), (.[] | to_entries | .[] | .value) | @csv`
        # assumes the JSON response is an array of objects. If your API returns a different
        # structure, you may need to adjust this filter.
        times=()
        first_time_timeout=0
        unsupported_query=0
        for ((i=1; i<=REPEAT_TIMES; i++)); do
            response=$(curl -sS -m "$TIMEOUT_SECONDS" -X POST -H "Content-Type: application/json" --data "$json_payload" "$API_URL")
            return_code=$?
            if [ $return_code -eq 28 ]; then
                echo "Error: The request timed out after $TIMEOUT_SECONDS seconds."
                times+=("${TIMEOUT_SECONDS}000")
                restart_app
                if [ $i -eq 1 ]; then
                    first_time_timeout=1
                    break
                fi
            elif [ $return_code -eq 0 ]; then
                # Extract the time value from the response
                time_val=$(echo "$response" | grep "time.*size.*" | sed -e "s/^.*\"time\":\(.*\),.*/\1/g")
                if [ -n "$time_val" ]; then
                    echo "Query: $query_name, Attempt: $i, Time: $time_val ms"
                    times+=("$time_val")
                else
                    unsupported_query=1
                    break
                fi
            else
                unsupported_query=1
                break
            fi
        done
        if [ $first_time_timeout -eq 1 ] ; then
            echo "$query_name,${TIMEOUT_SECONDS}000" >> "$output_csv_file"
        elif [ $unsupported_query -eq 0 ] ; then
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
            echo "Median time for $query_name: $median ms"
            echo "$query_name,$median" >> "$output_csv_file"
        fi

      # printf "$sql_file," >> "$output_csv_file"
        # curl -sS -m "$TIMEOUT_SECONDS" -X POST -H "Content-Type: application/json" --data "$json_payload" "$API_URL" | grep "time.*size*" | sed -e 's/^.*time\":\(.*\),.*size\":\(.*\)\}\}$/\1,\2/g' | tr -d '\n' >> "$output_csv_file"
      # echo "" >> "$output_csv_file"

        # Check if the CSV file was created and is not empty
        # if [ -s "$output_csv_file" ]; then
            # echo "Successfully created result file: $output_csv_file"
        # else
            # echo "Warning: Result file '$output_csv_file' is empty. The API might not have returned data or an error occurred."
            # Optional: remove empty file
            # rm "$output_csv_file"
        # fi
    done
done

echo "----------------------------------------"
echo "All queries processed."

kill -9 $(cat "$APP_PID_FILE")
rm "$APP_PID_FILE"
