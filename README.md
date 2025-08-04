# metaDecomp: Succinct structure representations for efficient query optimization

**Full technical report:** https://github.com/epfldata/metaDecomp/blob/main/technical-report.pdf

## Prerequisites

* Scala 3.3.1 with sbt 1.6.2
* DuckDB 1.2.2 — Please make the executable available in `$PATH`

Please execute all the following commands from the root directory of the repository.

## Setup

### IMDB dataset

Run the following script to download the IMDB dataset (from http://event.cwi.nl/da/job/imdb.tgz) and load the schema and data.
```
bash src/scripts/setup-imdb.sh
```

### Cardinality estimation

#### Original join order benchmark (JOB)

We include the exact cardinalities for queries in the subdirectory for the original join order benchmark. So no action is required.

#### JOBLarge

For JOBLarge, since the queries are much larger and more complex, please use the following script to download the cardinality information:
```
bash src/main/scripts/download-job-large-cardinalities.sh
```

## Experiments

First create a subdirectory `experiment-results` under the root directory of the repository:
```
mkdir experiment-results
```

### Query optimization and execution

```
sbt -mem 40000 "runMain experiments.runner.MetaDecompRunner"
```
(`-mem 40000` indicates a limit of 40,000 MB of memory for the JVM. This can be adjusted based on the amount of available memory in your system.)

The results are stored in `experiment-results/metadecomp-runtime-{job-original-exact, job-large-{estimate, all-0}}.csv`

### Join tree enumeration

```
bash src/main/scripts/enumerate-join-trees.sh
```

The results are stored in `experiment-results/metadecomp-enum-{job-original, job-large}.csv`
