#!/usr/bin/env python3
"""
Convert a graph file from Format 1 to Format 2.

Format 1:
    t N M
    v VertexID LabelId Degree   (N lines)
    e VertexId VertexId         (M lines)

Format 2:
    n m
    smaller_vertex_id larger_vertex_id   (m lines)

Usage:
    python convert_format1_to_format2.py input.txt output.txt
"""

import sys
import argparse


def parse_format1(input_path):
    """Parse a Format 1 file and return (N, M, edges)."""
    with open(input_path, 'r') as f:
        lines = [line.strip() for line in f if line.strip()]

    if not lines:
        raise ValueError("Input file is empty.")

    header = lines[0].split()
    if len(header) != 3 or header[0] != 't':
        raise ValueError(f"Expected header 't N M', got: '{lines[0]}'")

    N, M = int(header[1]), int(header[2])

    edges = []
    vertex_count = 0

    for line in lines[1:]:
        tokens = line.split()
        if not tokens:
            continue
        tag = tokens[0]

        if tag == 'v':
            vertex_count += 1
        elif tag == 'e':
            if len(tokens) != 3:
                raise ValueError(f"Malformed edge line: '{line}'")
            u, v = int(tokens[1]), int(tokens[2])
            a, b = (u, v) if u < v else (v, u)
            edges.append((a, b))
        else:
            raise ValueError(f"Unrecognized line prefix '{tag}' in line: '{line}'")

    if vertex_count != N:
        raise ValueError(f"Expected {N} vertex lines, found {vertex_count}")
    if len(edges) != M:
        raise ValueError(f"Expected {M} edge lines, found {len(edges)}")

    return N, M, edges


def write_format2(output_path, N, M, edges):
    """Write graph data out in Format 2."""
    with open(output_path, 'w') as f:
        f.write(f"{N} {M}\n")
        for a, b in edges:
            f.write(f"{a} {b}\n")


def convert(input_path, output_path):
    N, M, edges = parse_format1(input_path)
    write_format2(output_path, N, M, edges)
    return N, M


def main():
    parser = argparse.ArgumentParser(
        description="Convert a graph file from Format 1 (t/v/e lines) to Format 2 (n m + edge list)."
    )
    parser.add_argument("input", help="Path to the input file in Format 1")
    parser.add_argument("output", help="Path to write the output file in Format 2")
    args = parser.parse_args()

    try:
        N, M = convert(args.input, args.output)
    except (ValueError, OSError) as e:
        print(f"Error: {e}", file=sys.stderr)
        sys.exit(1)

    print(f"Converted successfully: {N} vertices, {M} edges.")
    print(f"Output written to: {args.output}")


if __name__ == "__main__":
    main()