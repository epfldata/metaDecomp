"""Generate the acyclic evaluation summary table from experiment CSVs."""
from pathlib import Path

import numpy as np
import pandas as pd

from config import results_path

BENCHMARKS = {
    'dsb': 'DSB',
    'job-original': 'JOB',
    'musicbrainz': 'Musicbrainz',
    'job-large': 'JOBLarge',
}
METHOD_LABELS = {
    'metadecomp': r'\sys{}',
    'dpconv': 'DPconv',
    'duckdb': 'DuckDB',
    'uniondp': 'UnionDP',
    'yanplus': r'Yannakakis$^+$',
    'learned-rewrite': 'LearnedRew.',
    'llm-r2': r'LLM-R$^2$',
}


def load_data(method, benchmark):
    prefix = method if method == 'duckdb' else f'{method}-opt'
    path = Path(results_path) / f'{prefix}-{benchmark}.csv'
    df = pd.read_csv(path, dtype={'query': str})
    # Older JOBLarge CSVs mix padded (001) and unpadded (1) numeric IDs.
    if benchmark == 'job-large':
        df['query'] = df['query'].str.zfill(3)
    df = df.set_index('query', verify_integrity=True)
    if 'total_opt_time' in df.columns and 'opt_time' not in df.columns:
        df = df.rename(columns={'total_opt_time': 'opt_time'})
    return df[['opt_time', 'exec_time', 'total_time']]


def get_stats(series):
    series = series[series > 0].dropna()
    if series.empty:
        raise ValueError("No positive timing measurements")
    mean_val = series.mean()
    median_val = series.median()
    p95_val = series.quantile(0.95)
    p99_val = series.quantile(0.99)

    def fmt(x):
        if x > 300000000 - 1e-5:
            return r"$>$ 5 min"

        s = x / 1000000.0
        if s < 0.01 or (series.name == "opt_time" and s < 0.1):
            ms = s * 1000.0
            if 0 < ms < 0.01:
                import math
                magnitude = int(math.floor(math.log10(ms)))
                precision = max(2, -magnitude)
                return f"{ms:.{precision}f} ms"
            return f"{ms:.2f} ms"

        return f"{s:.2f} s"

    return f"{fmt(mean_val)} & {fmt(median_val)} & {fmt(p95_val)} & {fmt(p99_val)}"


def generate_table():
    stats = {metric: {method: {} for method in METHOD_LABELS}
             for metric in ['opt_time', 'exec_time', 'total_time', 'speedup']}
    for benchmark in BENCHMARKS:
        dfs = {method: load_data(method, benchmark) for method in METHOD_LABELS}
        meta = dfs['metadecomp']
        dpconv = dfs['dpconv'].reindex(meta.index)
        duckdb = dfs['duckdb'].reindex(meta.index)
        # Exclude queries where all three core systems time out.
        threshold = 300000000 - 100000
        excluded = ((meta['total_time'] >= threshold)
                    & (dpconv['total_time'] >= threshold)
                    & (duckdb['total_time'] >= threshold))
        queries = meta.index[~excluded]
        for method, df in dfs.items():
            selected = df.loc[df.index.intersection(queries)]
            if len(selected) != len(queries):
                raise ValueError(f'{method}/{benchmark}: missing selected queries')
            if selected.isna().any().any() or (selected < 0).any().any():
                raise ValueError(f'{method}/{benchmark}: invalid timing measurements')
            for metric in ['opt_time', 'exec_time', 'total_time']:
                stats[metric][method][benchmark] = get_stats(selected[metric])
            if method != 'metadecomp':
                ratios = selected['total_time'] / meta.loc[selected.index, 'total_time']
                ratios = ratios[(ratios > 0) & np.isfinite(ratios)]
                if ratios.empty:
                    raise ValueError(f'{method}/{benchmark}: no valid speedups')
                values = [np.exp(np.log(ratios).mean()), ratios.median(),
                          ratios.quantile(0.95), ratios.quantile(0.99)]
                stats['speedup'][method][benchmark] = ' & '.join(f'{v:.2f}x' for v in values)
        print(f'{benchmark}: {len(queries)}/{len(meta)} queries retained')

    # Keep the checked-in paper's compact table layout, caption, and label.
    lines = [r"\begin{table}", r"    \begin{tiny}",
             r"        \renewcommand{\arraystretch}{1.1}",
             r"        \setlength\tabcolsep{0.3pt}",
             r"\begin{tabular}{cr|rrrr|rrrr|rrrr|rrrr}", r"    \hline",
             r"    \multicolumn{2}{r|}{\textbf{Benchmark $\rightarrow$}}"]
    headers = [rf"\multicolumn{{4}}{{{'c' if i == 3 else 'c|'}}}{{\textbf{{{label}}}}}"
               for i, label in enumerate(BENCHMARKS.values())]
    lines.append('        & ' + ' & '.join(headers) + r" \\")
    lines.append(r"    \textbf{Metric $\downarrow$} & \textbf{Method $\downarrow$}")
    lines.append('        & ' + ' & '.join(['Mean & Med. & 95th & 99th'] * 4) + r" \\")
    lines.append(r"    \hline")
    sections = [
        ('opt_time', 'Optimization time'),
        ('exec_time', 'Execution time'),
        ('total_time', r'\shortstack[c]{Overall evaluation\\time}'),
        ('speedup', r'\shortstack[c]{Overall speedup\\[-2pt]of metaDecomp\\[-2pt]over ...}'),
    ]
    for metric, title in sections:
        methods = [m for m in METHOD_LABELS if metric != 'speedup' or m != 'metadecomp']
        lines.append(r"    \multirow{" + str(len(methods))
                     + r"}{*}{\bf\rotatebox[origin=c]{90}{" + title + "}}")
        for method in methods:
            values = ' & '.join(stats[metric][method][b] for b in BENCHMARKS)
            lines.append(f"        & {METHOD_LABELS[method]}  & " + values + r" \\")
        lines.append(r"    \hline")
    lines.extend([
        r"\end{tabular}", r"    \end{tiny}",
        r'    \caption{Statistics of query evaluation time. The "mean" values are arithmetic mean for time and geometric mean for speedup factors.}',
        r"    \vspace{-1\baselineskip}", r"    \label{tab:stats-new}", r"\end{table}",
    ])
    output = Path(results_path) / 'tables/tab-results.tex'
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text('\n'.join(lines) + '\n')
    print(f'Saved table to {output}')


if __name__ == '__main__':
    generate_table()
