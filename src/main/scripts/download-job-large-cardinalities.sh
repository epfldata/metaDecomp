#!/bin/zsh

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
CARDINALITIES_PATH="$PROJECT_ROOT/benchmarks/job-large/cardinalities"

mkdir -p "$CARDINALITIES_PATH"
wget "https://drive.usercontent.google.com/download?id=1wcs_613P-aAWLoEIQJuo3xOEmbk4PcC-&export=download&authuser=1&confirm=t&uuid=8c64c2a8-811f-4b15-919f-73003b8c8900&at=AN8xHoqvgWDPun0Uv3t3gwkMOEUS:1754060710535" -O "$PROJECT_ROOT/job-large-cardinalities.zip"
tar -xzf job-large-cardinalities.zip -C "$CARDINALITIES_PATH"
rm "$PROJECT_ROOT/job-large-cardinalities.zip"
