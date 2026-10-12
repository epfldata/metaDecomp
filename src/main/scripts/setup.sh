#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/../../.."

# The archive contains benchmarks/ and datasets/ directly (despite its .zip name).
if [[ ! -f datasets/imdb/imdb.db || ! -f datasets/dsb/dsb-10g.db || ! -f datasets/musicbrainz/musicbrainz.db || ! -d benchmarks/musicbrainz/cardinalities ]]; then
  wget "https://drive.usercontent.google.com/download?id=16PE9TIeTvDZQQWR3eDxgyfZzFmeVZzgA&export=download&confirm=t" -O benchmarks.zip
  tar -xzf benchmarks.zip
  rm benchmarks.zip
fi
mkdir -p experiment-results/{join-trees,figures}
