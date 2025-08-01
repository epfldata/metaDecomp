#!/bin/bash

DATASET_PATH="datasets/imdb"

mkdir -p datasets/imdb
wget http://event.cwi.nl/da/job/imdb.tgz -O imdb.tgz
tar -xzf imdb.tgz -C "$DATASET_PATH"
rm imdb.tgz

duckdb "$DATASET_PATH/imdb.db" < "$DATASET_PATH/schematext.sql"

for CSV_FILE in "$DATASET_PATH"/*.csv; do
  TABLE=$(basename "$CSV_FILE" .csv)
  duckdb "$DATASET_PATH/imdb.db" "COPY $TABLE FROM '$CSV_FILE' (DELIMITER ',', ESCAPE '\');"
done
