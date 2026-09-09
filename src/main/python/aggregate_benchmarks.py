#!/usr/bin/env python3
"""
1. Find all files in a given directory whose names start with a given prefix.
2. Each such file is a CSV with columns:
   query,num_rels,max_fanout,metagyo_time,planning_time,total_opt_time,
   exec_time,total_time,cost_intm,cost_in,cout_cost
3. Produce a new file named "<prefix>.csv" with the same columns, where
   for each query, every non-"query" value is the average of that value
   across all matching input files.

Usage:
    python average_csvs.py <directory> <prefix>

Example:
    python average_csvs.py . run_result_
    # reads run_result_1.csv, run_result_2.csv, run_result_3.csv, ...
    # writes run_result_.csv (i.e. "<prefix>.csv") with per-query averages
"""

import os
import sys
import argparse
import pandas as pd

COLUMNS = [
    "query",
    "num_rels",
    "max_fanout",
    "metagyo_time",
    "planning_time",
    "total_opt_time",
    "exec_time",
    "total_time",
    "cost_intm",
    "cost_in",
    "cout_cost",
]

VALUE_COLUMNS = [c for c in COLUMNS if c != "query"]


def find_matching_files(directory: str, prefix: str):
    """Return sorted list of full paths of files in `directory` starting with `prefix`."""
    all_entries = sorted(os.listdir(directory))
    matches = [
        os.path.join(directory, f)
        for f in all_entries
        if f.startswith(prefix) and os.path.isfile(os.path.join(directory, f))
    ]
    return matches


def average_csvs(directory: str, prefix: str) -> str:
    matching_files = find_matching_files(directory, prefix)

    if not matching_files:
        print(f"No files found starting with '{prefix}' in '{directory}'.")
        return None

    print(f"Found {len(matching_files)} matching file(s):")
    for f in matching_files:
        print(f"  {f}")

    frames = []
    for f in matching_files:
        df = pd.read_csv(f)

        missing = set(COLUMNS) - set(df.columns)
        if missing:
            raise ValueError(f"File '{f}' is missing expected columns: {missing}")

        frames.append(df[COLUMNS])

    combined = pd.concat(frames, ignore_index=True)

    # Average all value columns per query, preserving first-seen query order
    query_order = combined["query"].drop_duplicates().tolist()
    averaged = combined.groupby("query", sort=False)[VALUE_COLUMNS].mean().reset_index()
    averaged["query"] = pd.Categorical(averaged["query"], categories=query_order, ordered=True)
    averaged = averaged.sort_values("query").reset_index(drop=True)

    # Reorder columns to match original column order
    averaged = averaged[COLUMNS]

    output_filename = f"{prefix}.csv"
    output_path = os.path.join(directory, output_filename)
    averaged.to_csv(output_path, index=False)

    print(f"\nWrote averaged results for {len(averaged)} unique quer{'y' if len(averaged)==1 else 'ies'} to: {output_path}")
    return output_path


def main():
    parser = argparse.ArgumentParser(
        description="Average per-query metrics across all CSV files starting with a given prefix."
    )
    parser.add_argument("directory", help="Directory to search in")
    parser.add_argument("prefix", help="Prefix that file names must start with")

    args = parser.parse_args()

    if not os.path.isdir(args.directory):
        print(f"Error: '{args.directory}' is not a valid directory.")
        sys.exit(1)

    average_csvs(args.directory, args.prefix)


if __name__ == "__main__":
    main()