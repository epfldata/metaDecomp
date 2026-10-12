#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/../../.."
JOBS=${BUILD_JOBS:-2}

clone() {
  local name=$1 url=$2 revision=$3
  if [[ ! -e "$name/.git" ]]; then
    echo "Cloning $name at $revision..."
    git init "$name"
    git -C "$name" remote add origin "$url"
    git -C "$name" fetch --depth 1 origin "$revision"
    git -C "$name" checkout --detach FETCH_HEAD
  else
    echo "Using existing $name checkout."
  fi
  # Existing checkouts are retained; record their actual revisions with each run.
}
clone DPconv https://github.com/zhekai-jiang/DPconv.git 17347ab00386bdbdc9d104d5289d1671fc072491
clone duckdb https://github.com/zhekai-jiang/duckdb.git 1c75dc61a89ed4d843462048cfcd7c9b95ad9d33
clone DuckDBYanPlus https://github.com/ChampionNan/DuckDBYanPlus.git 38d165ee6dac9db621616121b09bb591fdd67065
clone SparkSQLPlus https://github.com/hkustDB/SparkSQLPlus.git f22188bba4e971da6defb97c983e06e18e66fd7a

# The pinned DPconv source needs this standard header for Linux builds.
if [[ "$(uname)" != Darwin ]]; then
  echo "Adding the standard header required by DPconv on Linux..."
  python3 - <<'PYCODE'
from pathlib import Path
p = Path("DPconv/src/util/Util.hpp")
s = p.read_text()
if "#include <functional>" not in s:
    p.write_text("#include <functional>\n" + s)
PYCODE
fi
echo "Building DPconv and UnionDP..."
mkdir -p DPconv/src/build
(cd DPconv/src/build && cmake .. && make -j "$JOBS")
echo "Building DuckDB..."
make -C duckdb
# The pinned Yannakakis+ checkout is missing optional test and Parquet files.
echo "Building DuckDBYanPlus..."
make -C DuckDBYanPlus CMAKE_VARS="-DBUILD_UNITTESTS=OFF -DSKIP_EXTENSIONS=parquet"
echo "Building SparkSQLPlus..."
(cd SparkSQLPlus && mvn clean package -DskipTests=true)

# The existing rewrite runners consume saved rewrites and optimization timings.
mkdir -p llm-r2
if [[ ! -f llm-r2/LR_dsb_results.csv ]]; then
  echo "Downloading and extracting LLM-R2..."
  wget "https://drive.usercontent.google.com/download?id=1z4pPsONM99HoVmpSSOyugVf62MslEUbE&export=download&confirm=t" -O llm-r2.zip
  unzip -o llm-r2.zip -d llm-r2
  rm llm-r2.zip
else
  echo "Using existing LLM-R2 files."
fi
echo "Installing Python dependencies..."
python3 -m venv src/main/python/venv
src/main/python/venv/bin/pip install -r src/main/python/requirements.txt
echo "Staging the Scala application..."
sbt -mem "$(( ${JVM_MEMORY_GB:-40} * 1024 ))" stage
echo "System setup complete."
