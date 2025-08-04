#!/bin/zsh

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
CARDINALITIES_PATH="$PROJECT_ROOT/benchmarks/job-large/cardinalities"

mkdir -p "$CARDINALITIES_PATH"
wget "https://drive.usercontent.google.com/download?id=19fUchVtL6l_rAlzBG3mIJVYl_W9lYP5l&export=download&confirm=t&uuid=a878691d-d6f4-4752-a29f-c409b904ade2" -O "$PROJECT_ROOT/job-large-cardinalities.zip"
tar -xzf job-large-cardinalities.zip -C "$CARDINALITIES_PATH"
rm "$PROJECT_ROOT/job-large-cardinalities.zip"
