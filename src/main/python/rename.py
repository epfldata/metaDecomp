#!/usr/bin/env python3

import os
import sys
import argparse


def rename_matching_files(directory: str, prefix: str, new_name: str, dry_run: bool = False):
    """
    Finds all files in `directory` whose names start with `prefix`,
    and renames them to `<new_name>-i.csv` for i = 1..k.

    Files are processed in sorted (alphabetical) order for reproducibility.
    """
    # Only look at files (not subdirectories) directly inside `directory`
    all_entries = sorted(os.listdir(directory))
    matching_files = [
        f for f in all_entries
        if f.startswith(prefix) and os.path.isfile(os.path.join(directory, f))
    ]

    if not matching_files:
        print(f"No files found starting with '{prefix}' in '{directory}'.")
        return []

    renamed = []
    for i, old_filename in enumerate(matching_files, start=1):
        old_path = os.path.join(directory, old_filename)
        new_filename = f"{new_name}-{i}.csv"
        new_path = os.path.join(directory, new_filename)

        if dry_run:
            print(f"[DRY RUN] Would rename: {old_filename} -> {new_filename}")
        else:
            os.rename(old_path, new_path)
            print(f"Renamed: {old_filename} -> {new_filename}")

        renamed.append((old_filename, new_filename))

    print(f"\nTotal files renamed: {len(renamed)}")
    return renamed


def main():
    parser = argparse.ArgumentParser(
        description="Rename all files starting with a given prefix to <new_name>-i.csv"
    )
    parser.add_argument("directory", help="Directory to search in")
    parser.add_argument("prefix", help="Prefix that file names must start with")
    parser.add_argument("new_name", help="Base name to use for renamed files")
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Show what would be renamed without actually renaming",
    )

    args = parser.parse_args()

    if not os.path.isdir(args.directory):
        print(f"Error: '{args.directory}' is not a valid directory.")
        sys.exit(1)

    rename_matching_files(args.directory, args.prefix, args.new_name, dry_run=args.dry_run)


if __name__ == "__main__":
    main()