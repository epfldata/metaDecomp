import pandas as pd
import numpy as np
import os
from scipy.stats import gmean

# Configuration
RESULTS_DIR = 'experiment-results'
BENCHMARKS = {
    # 'dsb': 'DSB',
    # 'job-original': 'JOB',
    # 'musicbrainz': 'Musicbrainz',
    # 'job-large': 'JOBLarge'
    #'dsb-cyclic': 'DSB',
    #'musicbrainz-cyclic': 'Musicbrainz',
    #'subgraph-matching': 'Subgraph Matching',
    'custom-large' : 'Highly Constrained (Large)',
    'custom': 'Highly Constrained'
}
BENCHMARK_ORDER = [
#'dsb-cyclic', 'musicbrainz-cyclic', 'subgraph-matching',
 'custom-large', 'custom'
 ]

META_VARIANTS = [
    'metadecomp-complete',
    'metadecomp-random',
]

BASELINES = [
    'dpconv',
    'duckdb',
    'neo4j',
    # 'uniondp',
    # 'yanplus', # Yannakakis+
    # 'learned-rewrite',
    # 'llm-r2'
]

METHODS = META_VARIANTS + BASELINES

# LaTeX Label mapping
METHOD_LABELS = {
    'metadecomp-complete': r'\completealgo{}',
    'metadecomp-random': r'\randomalgo{}',
    'dpconv': 'DPconv',
    'duckdb': 'DuckDB',
    'neo4j': 'Neo4j',
    # 'uniondp': r'UnionDP',
    # 'yanplus': r'Yannakakis$^+$',
    # 'learned-rewrite': r'LearnedRewrite',
    # 'llm-r2': r'LLM-R$^2$'
}

def load_data(method, benchmark):
    filename = ''
    if method == 'duckdb' or method == 'neo4j':
        filename = f'{method}-{benchmark}.csv'
    else:
        filename = f'{method}-opt-{benchmark}.csv'

    path = os.path.join(RESULTS_DIR, filename)

    if not os.path.exists(path):
        # Fallback or specific file handling could go here, but for now return None
        return None

    try:
        df = pd.read_csv(path)
        if 'total_opt_time' in df.columns and 'opt_time' not in df.columns:
            df.rename(columns={'total_opt_time': 'opt_time'}, inplace=True)
        return df
    except Exception as e:
        print(f"Error reading {path}: {e}")
        return None

def get_stats(series):
    if series.empty:
        return ""
    series = series.drop(series[series == 0].index)
    if series.empty:
        return ""
    mean_val = series.mean()
    median_val = series.median()
    p95_val = series.quantile(0.95)
    p99_val = series.quantile(0.99)

    def fmt(x):
        if x > 300000000 - 1e-5:
             # User asked: "If the time is over 300000000 microseconds"
             return r"$>$ 5 min"

        s = x / 1000000.0
        if s < 0.1 or (series.name == "opt_time" and s < 0.1):
            ms = s * 1000.0
            if 0 < ms < 0.01:
                import math
                magnitude = int(math.floor(math.log10(ms)))
                precision = max(2, -magnitude)
                return f"{ms:.{precision}f} ms"
            return f"{ms:.2f} ms"

        return f"{s:.2f} s"

    return f"{fmt(mean_val)} & {fmt(median_val)} & {fmt(p95_val)} & {fmt(p99_val)}"

def format_cell(val):
    if pd.isna(val) or val == "":
        return ""
    return f"{val:,.2f}" if isinstance(val, float) else f"{val:,}"

def generate_table():
    # Store aggregated stats
    stats = {
        'opt': {m: {b: "" for b in BENCHMARK_ORDER} for m in METHODS},
        'exec': {m: {b: "" for b in BENCHMARK_ORDER} for m in METHODS},
        'exec_speedup': {v: {b_m: {b: "" for b in BENCHMARK_ORDER} for b_m in BASELINES} for v in META_VARIANTS},
        'total': {m: {b: "" for b in BENCHMARK_ORDER} for m in METHODS},
        'speedup': {v: {b_m: {b: "" for b in BENCHMARK_ORDER} for b_m in BASELINES} for v in META_VARIANTS},
    }

    # Pre-load data to determine valid queries
    bench_dfs = {}
    for bench in BENCHMARK_ORDER:
        bench_dfs[bench] = {}
        for method in METHODS:
            df = load_data(method, bench)
            if df is not None:
                if 'query' in df.columns:
                    df.set_index('query', inplace=True)
                bench_dfs[bench][method] = df

    for bench in BENCHMARK_ORDER:
        dfs = bench_dfs[bench]

        # Determine valid queries from metadecomp variants
        valid_queries = None
        for v in META_VARIANTS:
            if v in dfs:
                if valid_queries is None:
                    valid_queries = set(dfs[v].index)
                else:
                    valid_queries = valid_queries.intersection(set(dfs[v].index))

        if valid_queries is None:
            continue

        # Filter Logic
        # Exclude if all methods timeout (>300s)
        queries_to_exclude = set()
        THRESHOLD = 300000000 - 1e5 # 300s in us
        for q in valid_queries:
            all_timeout = True
            if q == 'q7-w2-r11-a':
                queries_to_exclude.add(q)
            for m in METHODS:
                if m in dfs and q in dfs[m].index and 'total_time' in dfs[m].columns:
                    if dfs[m].loc[q, 'total_time'] < THRESHOLD:
                        all_timeout = False
                        break
            if all_timeout or (bench == "job-large" and 'duckdb' in dfs and q in dfs['duckdb'].index and dfs['duckdb'].loc[q, 'total_time'] < 50000):
                queries_to_exclude.add(q)

        final_queries = [q for q in valid_queries if q not in queries_to_exclude]

        # Filter all method DataFrames
        filtered_dfs = {}
        for m in METHODS:
            if m in dfs:
                common_queries = [q for q in final_queries if q in dfs[m].index]
                filtered_dfs[m] = dfs[m].loc[common_queries]

        # 1. Optimization, Execution, Overall Time
        for method in METHODS:
            if method not in filtered_dfs:
                continue
            df_filtered = filtered_dfs[method]
            print(method)
            print(df_filtered)

            if 'opt_time' in df_filtered.columns:
                stats['opt'][method][bench] = get_stats(df_filtered['opt_time'])

            if 'exec_time' in df_filtered.columns:
                stats['exec'][method][bench] = get_stats(df_filtered['exec_time'])

            if 'total_time' in df_filtered.columns:
                stats['total'][method][bench] = get_stats(df_filtered['total_time'])

        # 2. Execution & Overall Speedups for each MetaDecomp variant over baselines
        for v in META_VARIANTS:
            if v not in filtered_dfs:
                continue
            df_v = filtered_dfs[v]

            for base in BASELINES:
                if base not in filtered_dfs:
                    continue
                df_b = filtered_dfs[base]
                common_q = [q for q in df_v.index if q in df_b.index]

                # Execution speedup
                if 'exec_time' in df_v.columns and 'exec_time' in df_b.columns:
                    t_base_exec = df_b.loc[common_q, 'exec_time']
                    t_v_exec = df_v.loc[common_q, 'exec_time']

                    ratios_exec = []
                    for q in common_q:
                        m_val = t_v_exec.loc[q]
                        o_val = t_base_exec.loc[q]
                        if m_val > 0 and o_val > 0:
                            ratios_exec.append(o_val / m_val)

                    if ratios_exec:
                        gm_exec = gmean(ratios_exec)
                        median_exec = np.median(ratios_exec)
                        speedup_exec_series = pd.Series(ratios_exec)
                        p95_exec = speedup_exec_series.quantile(0.95)
                        p99_exec = speedup_exec_series.quantile(0.99)
                        stats['exec_speedup'][v][base][bench] = f"{gm_exec:.2f}x & {median_exec:.2f}x & {p95_exec:.2f}x & {p99_exec:.2f}x"

                # Overall speedup
                if 'total_time' in df_v.columns and 'total_time' in df_b.columns:
                    t_base_total = df_b.loc[common_q, 'total_time']
                    t_v_total = df_v.loc[common_q, 'total_time']

                    ratios_total = []
                    for q in common_q:
                        m_val = t_v_total.loc[q]
                        o_val = t_base_total.loc[q]
                        if m_val > 0 and o_val > 0:
                            ratios_total.append(o_val / m_val)

                    if ratios_total:
                        gm = gmean(ratios_total)
                        median = np.median(ratios_total)
                        speedup_series = pd.Series(ratios_total)
                        p95 = speedup_series.quantile(0.95)
                        p99 = speedup_series.quantile(0.99)
                        stats['speedup'][v][base][bench] = f"{gm:.2f}x & {median:.2f}x & {p95:.2f}x & {p99:.2f}x"

    # Generate LaTeX
    print(r"\begin{tabular}{cc|cccc|cccc|cccc|cccc}")
    print(r"    \hline")
    print(r"    \multicolumn{2}{r|}{\textbf{Benchmark $\rightarrow$}}")
    print(r"        & \multicolumn{4}{c|}{\textbf{DSB}} & \multicolumn{4}{c|}{\textbf{Musicbrainz}} & \multicolumn{4}{c|}{\textbf{Subgraph Matching}} & \multicolumn{4}{c}{\textbf{Highly Constrained}} \\")
    print(r"    \textbf{Metric $\downarrow$} & \textbf{Method $\downarrow$}")
    print(r"        & Mean & Median & 95th & 99th & Mean & Median & 95th & 99th & Mean & Median & 95th & 99th & Mean & Median & 95th & 99th \\")
    print(r"    \hline")

    def print_section_header(title, multi_row_count):
        print(r"    \multirow{" + str(multi_row_count) + r"}{*}{" + r"\bf\shortstack{" + title + r"}}")

    # Optimization Time
    print_section_header("Optimization\\\\time", len(METHODS))
    for method in METHODS:
        row_str = f"        & {METHOD_LABELS[method]}  & "
        parts = []
        for bench in BENCHMARK_ORDER:
             val = stats['opt'][method][bench]
             if val == "": val = "& & & "
             parts.append(val)
        row_str += " & ".join(parts) + r" \\"
        print(row_str)
    print(r"    \hline")

    # Execution Time
    print_section_header("Execution\\\\time", len(METHODS))
    for method in METHODS:
        row_str = f"        & {METHOD_LABELS[method]}  & "
        parts = []
        for bench in BENCHMARK_ORDER:
             val = stats['exec'][method][bench]
             if val == "": val = "& & & "
             parts.append(val)
        row_str += " & ".join(parts) + r" \\"
        print(row_str)
    print(r"    \hline")

    # Execution Speedup (separate group for each MetaDecomp variant)
    for v in META_VARIANTS:
        print_section_header(f"Execution speedup:\\\\{METHOD_LABELS[v]}\\\\over...", len(BASELINES))
        for base in BASELINES:
            row_str = f"        & {METHOD_LABELS[base]}  & "
            parts = []
            for bench in BENCHMARK_ORDER:
                val = stats['exec_speedup'][v][base][bench]
                if val == "": val = "& & & "
                parts.append(val)
            row_str += " & ".join(parts) + r" \\"
            print(row_str)
        print(r"    \hline")

    # Overall Evaluation Time
    print_section_header("Overall\\\\evaluation\\\\time", len(METHODS))
    for method in METHODS:
        row_str = f"        & {METHOD_LABELS[method]}  & "
        parts = []
        for bench in BENCHMARK_ORDER:
             val = stats['total'][method][bench]
             if val == "": val = "& & & "
             parts.append(val)
        row_str += " & ".join(parts) + r" \\"
        print(row_str)
    print(r"    \hline")

    # Overall Speedup (separate group for each MetaDecomp variant)
    for v in META_VARIANTS:
        print_section_header(f"Overall speedup:\\\\{METHOD_LABELS[v]}\\\\over...", len(BASELINES))
        for base in BASELINES:
            row_str = f"        & {METHOD_LABELS[base]}  & "
            parts = []
            for bench in BENCHMARK_ORDER:
                val = stats['speedup'][v][base][bench]
                if val == "": val = "& & & "
                parts.append(val)
            row_str += " & ".join(parts) + r" \\"
            print(row_str)
        print(r"    \hline")
    print(r"\end{tabular}")

if __name__ == "__main__":
    generate_table()