"""Generate the exact vs misestimated cardinality table from execution results."""
from pathlib import Path

import pandas as pd

from config import results_path
from generate_summary_table import get_stats

BENCHMARKS = ['dsb', 'job-original']

METHODS = [
    'metadecomp',
    'dpconv',
    'uniondp'
]

METHOD_LABELS = {
    'metadecomp': r'\sys{}',
    'dpconv': 'DPconv',
    'uniondp': r'UnionDP'
}

VARIANTS = {
    'Exact': 'opt',
    'Misestimated': 'misestimated'
}


def load_data(method, variant_suffix, benchmark):
    path = Path(results_path) / f'{method}-{variant_suffix}-{benchmark}.csv'
    return pd.read_csv(path, dtype={'query': str}).set_index('query', verify_integrity=True)


def generate_table():
    stats = {m: {v: {} for v in VARIANTS} for m in METHODS}
    for bench in BENCHMARKS:
        queries = load_data('metadecomp', 'opt', bench).index
        for method in METHODS:
            for variant_name, variant_suffix in VARIANTS.items():
                # Require the same query population for exact and misestimated runs.
                execution = load_data(method, variant_suffix, bench).loc[queries, 'exec_time']
                if execution.isna().any() or (execution < 0).any():
                    raise ValueError(f'{method}/{variant_name}/{bench}: invalid execution times')
                stats[method][variant_name][bench] = get_stats(execution)
        print(f'{bench}: {len(queries)} queries')

    lines = [r"\begin{table}", r"    \begin{footnotesize}",
             r"\begin{tabular}{cc|cccc|cccc}", r"    \hline",
             r"    \multicolumn{2}{r|}{\textbf{Benchmark $\rightarrow$}}",
             r"        & \multicolumn{4}{c|}{\textbf{DSB}} & \multicolumn{4}{c}{\textbf{JOB}} \\",
             r"    \textbf{Method $\downarrow$} & \textbf{Cardinality $\downarrow$}",
             r"        & Mean & Median & 95th & 99th & Mean & Median & 95th & 99th \\",
             r"    \hline"]
    for method in METHODS:
        lines.append(r"    \multirow{2}{*}{" + METHOD_LABELS[method] + "}")
        for variant_name in VARIANTS:
            values = ' & '.join(stats[method][variant_name][b] for b in BENCHMARKS)
            lines.append(f"        & {variant_name} & " + values + r" \\")
        lines.append(r"    \hline")
    lines.extend([
        r"\end{tabular}", r"    \end{footnotesize}",
        r"    \caption{Query execution time when optimized with exact vs misestimated cardinalities}",
        r"    \label{tab:misestimation}", r"\end{table}",
    ])
    output = Path(results_path) / 'tables/tab-misestimation.tex'
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text('\n'.join(lines) + '\n')
    print(f'Saved table to {output}')


if __name__ == '__main__':
    generate_table()
