#!/bin/zsh

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"

wget "https://drive.usercontent.google.com/download?id=16PE9TIeTvDZQQWR3eDxgyfZzFmeVZzgA&export=download&authuser=1&confirm=t&uuid=9b5fb26c-5c4b-4015-8953-7e565f1a54e3&at=AKSUxGMsrKSSDlYVNa2OtlhDFcl_:1760628758367" -O "$PROJECT_ROOT/benchmarks.zip"
tar -xzf "$PROJECT_ROOT/benchmarks.zip" -C "$PROJECT_ROOT"
# rm "$PROJECT_ROOT/benchmarks.zip"

mkdir experiment-results
mkdir experiment-results/join-trees
mkdir experiment-results/figures

