#!/bin/zsh

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"

wget "https://drive.usercontent.google.com/download?id=16PE9TIeTvDZQQWR3eDxgyfZzFmeVZzgA&export=download&authuser=1&confirm=t&uuid=2218cb4c-35cd-4ebc-9835-6ebe5937b80b&at=AKSUxGN-N7cZUXIYiR0tvoXWOA-e:1760623677259" -O "$PROJECT_ROOT/benchmarks.zip"
tar -xzf "$PROJECT_ROOT/benchmarks.zip" -C "$PROJECT_ROOT"
# rm "$PROJECT_ROOT/benchmarks.zip"

mkdir experiment-results
mkdir experiment-results/join-trees
mkdir experiment-results/figures

