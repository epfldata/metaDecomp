# metaDecomp: Succinct structure representations for efficient query optimization

## Prerequisites

* Scala 3.3.1 with sbt 1.6.2
* For [DPconv](https://github.com/utndatasystems/DPconv/tree/dc56bdc52c452bf86b3ac5c224b0176148c38757), DuckDB, and Yannakakis+: CMake 4.0.3, GNU Make 3.81, clang 20.1.3
* For join tree enumeration with traditional GYO algorithms (implemented by [SparkSQL+](https://github.com/hkustDB/SparkSQLPlus/tree/f22188bba4e971da6defb97c983e06e18e66fd7a)): Maven 3.8.6
* For Learned Rewrite, LLM-R2, and reproducing the figures: Python 3.10.1

Please execute all the following commands from the root directory of the repository. The commands and scripts are written on macOS Tahoe 26.0.1 with zsh 5.9 / GNU bash 3.2.57(1). For other operating systems or shells, you may need to adapt the commands accordingly.

## Setup

### Datasets, benchmark queries, cardinality estimation, folder structures

Given the need to convert datasets to DuckDB and the large sizes of required files, we provide them all in a setup script. Please run the following script to set up the folder structures and download the files. Note that these files take approximately 22 GB of storage space in total.
```
bash src/main/scripts/setup.sh
```

This script will download and unzip the files for the benchmarks, and create folders to be used to store experiment results. After running this script, the folder structure should look like the following:
```
metaDecomp (root)
|- benchmarks
   |- dsb
   |- job-large
   |- job-original
   |- musicbrainz
|- datasets
   |- dsb
   |- imdb
   |- musicbrainz
|- experiment-results
   |- figures
   |- join-trees
...
```
If this is not the case (e.g., some new folder is created as the zip file is unzipped), please move the folders accordingly.

The DSB dataset and queries are generated using the code in https://github.com/microsoft/dsb.
The IMDB dataset (for JOB and JOBLarge) comes directly from http://event.cwi.nl/da/job/imdb.tgz.
The Musicbrainz dataset was downloaded from https://data.metabrainz.org/pub/musicbrainz/data/fullexport/ on September 19, 2025 and converted to the format of DuckDB.
The queries are generated using the code in https://github.com/Manciukic/postgres-gpuqo/tree/master/scripts/databases/musicbrainz.
All cardinality information is formatted in the same way as in https://github.com/utndatasystems/DPconv.
These files that we use in the experiment can all be downloaded using the setup script. No further action is required.

### DPconv (for both DPconv and UnionDP)

We adapted the code of DPconv for us to use for our experiments. We also included an implementation of UnionDP on top of the code of DPconv. This adapted version is also given in another anonymous repository.

First download the code:
```
wget "https://anonymous.4open.science/api/repo/DPconv-8525/zip" -O "DPconv.zip"
mkdir DPconv
tar -xzf "DPconv.zip" -C DPconv
rm DPconv.zip
```

There should then be a folder DPconv under the root directory of this repository:
```
metaDecomp (root)
|- DPconv
   |- queries
   |- src
   |- ...
...
```

Then, build DPconv, as per the instructions in the DPConv repository:
```
cd DPconv/src
mkdir -p build
cd build
cmake ..
make
```

### DuckDB

We slightly modified the code of DuckDB to measure the optimization time. The modified code is given in another anonymous repository.

First download the code:
```
wget "https://anonymous.4open.science/api/repo/duckdb-1240/zip" -O "duckdb.zip"
mkdir duckdb
tar -xzf "duckdb.zip" -C duckdb
rm duckdb.zip
```

There should then be a folder duckdb under the root directory of this repository:
```
metaDecomp (root)
|- duckdb
   |- benchmark
   |- data
   |- examples
   |- ...
...
```

### DuckDBYanPlus (for Yannakakis+)

Simply clone the repository of [DuckDBYanPlus](https://github.com/ChampionNan/DuckDBYanPlus) as is, and checkout the commit we use in our experiments:
```
git clone https://github.com/ChampionNan/DuckDBYanPlus.git
cd DuckDBYanPlus
git checkout 38d165e
```

### LLM-R2 (for LLM-R2 and Learned Rewrite)

We use the code from the authors of LLM-R2, which contains the models and scripts for both LLM-R2 and Learned Rewrite. We adapted the code to run the experiments on the benchmarks used in this paper. The modified code is given in another anonymous repository.

First download the code:
```
wget "https://anonymous.4open.science/api/repo/llm-r2-F53D/zip" -O "llm-r2.zip"
mkdir llm-r2
tar -xzf "llm-r2.zip" -C llm-r2
rm llm-r2.zip
```

There should then be a folder duckdb under the root directory of this repository:
```
metaDecomp (root)
|- llm-r2
   |- data
   |- rules_for_selected
   |- src
   |- ...
...
```

### SparkSQL+ (join tree enumeration by the traditional GYO algorithm)

Clone the SparkSQLPlus repository, checkout the commit we use in our experiments, and build the project:
```
# Remember to change back to the root of the project repository
git clone https://github.com/hkustDB/SparkSQLPlus.git
cd SparkSQLPlus
git checkout f22188bba4e971da6defb97c983e06e18e66fd7a
mvn clean package -DskipTests=true
```

### Python (for Learned Rewrite, LLM-R2, and plotting figures)

Create and activate a virtual environment:
```
# Remember to change back to the root of the project repository
python -m venv src/main/python/venv
source src/main/python/venv/bin/activate
```

Install the required dependencies:
```
pip install -r src/main/python/requirements.txt
```

## Experiments

### Query optimization and execution

#### metaDecomp

```
sbt -mem 32000 "runMain experiments.runner.MetaDecompRunner"
```
(`-mem 32000` indicates a limit of 32,000 MB of memory for the JVM. This can be adjusted based on the amount of available memory in your system. For certain queries in JOBLarge, such a large memory may be required due to need to load the cardinality estimation of a very large number of possible intermediate results.)

The results are stored in `experiment-results/metadecomp-opt-{dsb, job-original, musicbrainz, job-large}-<timestamp>.csv`

#### DPConv

```
sbt "runMain experiments.runner.DPconvRunner"
```

The results are stored in `experiment-results/dpconv-opt-{dsb, job-original, musicbrainz, job-large}-<timestamp>.csv`

#### DuckDB

```
sbt "runMain experiments.runner.DuckDBRunner"
```

The results are stored in `experiment-results/duckdb-{dsb, job-original, musicbrainz, job-large}-<timestamp>.csv`

#### UnionDP

```
sbt "runMain experiments.runner.DPconvRunner UnionDP"
```

The results are stored in `experiment-results/uniondp-opt-{dsb, job-original, musicbrainz, job-large}-<timestamp>.csv`

#### Yannakakis+

```
sbt "runMain experiments.runner.DuckDBYanPlusRunner"
```

The results are stored in `experiment-results/yanplus-opt-{dsb, job-original, musicbrainz, job-large}-<timestamp>.csv`

#### LLM-R2

```
sbt "runMain experiments.runner.LLMR2Runner"
```

The results are stored in `experiment-results/llm-r2-opt-{dsb, job-original, musicbrainz, job-large}-<timestamp>.csv`

#### Learned Rewrite

```
sbt "runMain experiments.runner.LearnedRewriteRunner"
```

The results are stored in `experiment-results/learned-rewrite-opt-{dsb, job-original, musicbrainz, job-large}-<timestamp>.csv`

### Join tree enumeration

#### metaDecomp

```
bash src/main/scripts/enumerate-join-trees-metadecomp.sh
```

The results are stored in `experiment-results/metadecomp-enum-{dsb, job-original, musicbrainz, job-large}.csv`

#### Traditional GYO algorithm (implemented by SparkSQL+)

```
bash src/main/scripts/enumerate-join-trees-sparksqlplus.sh
```

The results are stored in `experiment-results/sparksqlplus-enum-{dsb, job-original, musicbrainz, job-large}.csv`

## Figures

After obtaining the experiment results, the figures in the paper can be reproduced as follows. All figures will be stored in `experiment-results/figures` as PDF files.

Before executing the scripts below, please remove the timestamps from the generated csv files. For example, rename `experiment-results/metadecomp-opt-dsb-2025-10-16T17-24-04.csv` to `experiment-results/metadecomp-opt-dsb.csv`.

### Optimization time – Figures 8 & 13

```
python src/main/python/plot-opt-time.py
```

### Comparison of the costs of optimal width-1 query plans and those of the gloally optimal plans – Figures 9a & 14

```
python src/main/python/plot-cost-ratio-stacked.py
python src/main/python/plot-cost-ratio-individual.py
```

### All speedups – Figures 9b, 10, 15, 16, 18, 20, 22, 24, 26
```
python src/main/python/plot-speedups-stacked.py
python src/main/python/plot-speedups-individual.py
```

### Scatter plots comparing total runtime – Figures 11, 17, 19, 21, 23, 25, 27

```
python src/main/python/plot-scatter-all.py
python src/main/python/plot-scatter-individual.py
```

### Join tree enumeration – Figures 12 & 28

```
python src/main/python/plot-enum-time-vs-num-jts-all.py
python src/main/python/plot-enum-time-vs-num-jts-individual.py
```
