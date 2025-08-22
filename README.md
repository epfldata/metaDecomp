# metaDecomp: Succinct structure representations for efficient query optimization

**Full technical report:** https://github.com/epfldata/metaDecomp/blob/main/technical-report.pdf

## Prerequisites

* Scala 3.3.1 with sbt 1.6.2
* DuckDB 1.2.2 — Please make the executable available in `$PATH`
* For [DPconv](https://github.com/utndatasystems/DPconv/tree/dc56bdc52c452bf86b3ac5c224b0176148c38757): CMake 4.0.3, GNU Make 3.81, clang 20.1.3
* For join tree enumeration with traditional GYO algorithms (implemented by [SparkSQL+](https://github.com/hkustDB/SparkSQLPlus/tree/f22188bba4e971da6defb97c983e06e18e66fd7a)): Maven 3.8.6
* For reproducing the figures: Python 3.10.1

Please execute all the following commands from the root directory of the repository.

## Setup

### IMDB dataset

Run the following script to download the IMDB dataset (from http://event.cwi.nl/da/job/imdb.tgz) and load the schema and data.
```
bash src/main/scripts/setup-imdb.sh
```

### Cardinality estimation

#### Original join order benchmark (JOB)

We include the exact cardinalities for queries in the subdirectory for the original join order benchmark. So no action is required.

#### JOBLarge

For JOBLarge, since the queries are much larger and more complex, please use the following script to download the cardinality information:
```
bash src/main/scripts/download-job-large-cardinalities.sh
```

### DPconv

First clone the DPconv repository and checkout the commit we use in our experiments:
```
git clone https://github.com/utndatasystems/DPconv.git
cd DPconv
git checkout dc56bdc
```

Then, replace the file `DPconv/src/util/BenchmarkRunner.hpp` by `replacement-files/DPconv/BenchmarkRunner.hpp` in this repository.

Finally, build DPconv, as per the instructions in the DPConv repository:
```
cd src
mkdir -p build
cd build
cmake ..
make
```

### SparkSQL+ (join tree enumeration by the traditional GYO algorithm)

Clone the SparkSQLPlus repository, checkout the commit we use in our experiments, and build the project:
```
git clone https://github.com/hkustDB/SparkSQLPlus.git
cd SparkSQLPlus
git checkout f22188bba4e971da6defb97c983e06e18e66fd7a
mvn clean package -DskipTests=true
```

### Python (for plotting figures)

Create and activate a virtual environment:
```
python -m venv src/main/python/venv
source src/main/python/venv/bin/activate
```

Install the required dependencies:
```
pip install src/main/python/requirements.txt
```

## Experiments

First create a subdirectory `experiment-results` under the root directory of the repository, as well as subdirectories:
```
mkdir -p experiment-results/join-trees experiment-results/metadecomp-plans experiment-results/dpconv-plans
```

### Query optimization and execution

#### metaDecomp

```
sbt -mem 40000 "runMain experiments.runner.MetaDecompRunner"
```
(`-mem 40000` indicates a limit of 40,000 MB of memory for the JVM. This can be adjusted based on the amount of available memory in your system.)

The results are stored in `experiment-results/metadecomp-opt-{job-original-exact, job-large-{estimate, all-0}}.csv`

#### DPConv

```
sbt "runMain experiments.runner.DPconvRunner"
```

The results are stored in `experiment-results/dpconv-opt-{job-original-exact, job-large-{estimate, all-0}}.csv`

### Join tree enumeration

#### metaDecomp

```
bash src/main/scripts/enumerate-join-trees-metadecomp.sh
```

The results are stored in `experiment-results/metadecomp-enum-{job-original, job-large}.csv`

#### Traditional GYO algorithm (implemented by SparkSQL+)

```
bash src/main/scripts/enumerate-join-trees-sparksqlplus.sh
```

The results are stored in `experiment-results/sparksqlplus-enum-{job-original, job-large}.csv`

## Figures

After obtaining the experiment results, the figures in the paper can be reproduced as follows.

### Figures 8a & 9a – Optimization time

```
python src/main/python/plot-opt-time.py
```

The figures are stored in `experiment-results/opt-time-{original-exact, large-estimate}.pdf`

### Figure 8b – Costs

```
python src/main/python/plot-cost-ratio.py
```

The figure is stored in `experiment-results/cost-ratios.pdf`

### Figure 8c – Total run time on the original JOB

```
python src/main/python/plot-original-time.py
```

The figure is stored in `experiment-results/runtime-job-original-exact.pdf`

### Figures 9c & 9d – Total run time on JOBLarge

```
python src/main/python/plot-large-time.py
```

The figures are stored in `experiment-results/opt-time-{large-estimate, large-all-0}.pdf`

### Figure 10 – Join tree enumeration time

```
python src/main/python/plot-enum-time.py
```

The figure is stored in `experiment-results/enum-time.pdf`
