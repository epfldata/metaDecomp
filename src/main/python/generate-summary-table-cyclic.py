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
    'dsb-cyclic': 'DSB',
    'musicbrainz-cyclic': 'Musicbrainz',
    'subgraph-matching': 'Subgraph Matching'
}
BENCHMARK_ORDER = ['dsb-cyclic', 'musicbrainz-cyclic', 'subgraph-matching'] # 'dsb', 'job-original', 'musicbrainz', 'job-large']

METHODS = [
    'metadecomp',
    'dpconv',
    'duckdb',
    # 'uniondp',
    # 'yanplus', # Yannakakis+
    # 'learned-rewrite',
    # 'llm-r2'
]

# LaTeX Label mapping
METHOD_LABELS = {
    'metadecomp': r'\sys{}',
    'dpconv': 'DPconv',
    'duckdb': 'DuckDB',
    # 'uniondp': r'UnionDP',
    # 'yanplus': r'Yannakakis$^+$',
    # 'learned-rewrite': r'LearnedRewrite',
    # 'llm-r2': r'LLM-R$^2$'
}

def load_data(method, benchmark):
    filename = ''
    if method == 'duckdb':
        filename = f'duckdb-{benchmark}.csv'
    elif method == 'metadecomp' and benchmark == 'dsb' and os.path.exists(os.path.join(RESULTS_DIR, 'metadecomp-opt-dsb-exact-heuristic.csv')): # Heuristic check if needed, but sticking to requested names first
         # The user request said "experiment-results/{metadecomp...}-opt-{dsb...}.csv"
         # but let's stick to the standard names found in the dir listing or request.
         # The request said: metadecomp-opt-dsb.csv.
         # Wait, in the file listing I saw: metadecomp-opt-dsb.csv. Correct.
         filename = f'{method}-opt-{benchmark}.csv'
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
    # Structure: stats[metric][method][benchmark] = "Mean & 95th & 99th"
    stats = {
        'opt': {m: {b: "" for b in BENCHMARK_ORDER} for m in METHODS},
        'exec': {m: {b: "" for b in BENCHMARK_ORDER} for m in METHODS},
        'exec_speedup': {m: {b: "" for b in BENCHMARK_ORDER} for m in METHODS},
        'total': {m: {b: "" for b in BENCHMARK_ORDER} for m in METHODS},
        'speedup': {m: {b: "" for b in BENCHMARK_ORDER} for m in METHODS}
    }

    # Pre-load MetaDecomp data to determine valid queries
    meta_dfs = {}
    for bench in BENCHMARK_ORDER:
        df = load_data('metadecomp', bench)
        if df is not None:
             # Ensure index is query name
             if 'query' in df.columns:
                 df.set_index('query', inplace=True)
             meta_dfs[bench] = df

    for bench in BENCHMARK_ORDER:
        meta_df = meta_dfs.get(bench)
        if meta_df is None:
            continue
            
        valid_queries = set(meta_df.index)
        
        # Load other essential methods for filtering
        dpconv_df = load_data('dpconv', bench)
        duckdb_df = load_data('duckdb', bench)
        
        if dpconv_df is not None and 'query' in dpconv_df.columns: dpconv_df.set_index('query', inplace=True)
        if duckdb_df is not None and 'query' in duckdb_df.columns: duckdb_df.set_index('query', inplace=True)

        # Filter Logic
        # Exclude if meta_df > 300s AND dpconv > 300s AND duckdb > 300s
        queries_to_exclude = set()
        for q in valid_queries:
            t_meta = meta_df.loc[q, 'total_time'] if (q in meta_df.index and 'total_time' in meta_df.columns) else 0
            
            t_dp = 0
            if dpconv_df is not None and q in dpconv_df.index and 'total_time' in dpconv_df.columns:
                t_dp = dpconv_df.loc[q, 'total_time']
            
            t_duck = 0
            if duckdb_df is not None and q in duckdb_df.index and 'total_time' in duckdb_df.columns:
                t_duck = duckdb_df.loc[q, 'total_time']
            
            THRESHOLD = 300000000 - 1e5 # 300s in us
            if (t_meta >= THRESHOLD and t_dp >= THRESHOLD and t_duck >= THRESHOLD) or (bench == "job-large" and t_duck < 50000):
                queries_to_exclude.add(q)
        
        final_queries = list(valid_queries - queries_to_exclude)
        
        # Process each method
        for method in METHODS:
                
            df = load_data(method, bench)
            if df is None:
                continue
            
            if 'query' in df.columns:
                df.set_index('query', inplace=True)
            
            # Filter to final queries
            # reindex adds NaNs for missing queries, which dropna() would remove or we handle
            # We only want queries that exist in valid_queries (which came from metadecomp)
            # Intersection of existing queries in this method and final_queries
            common_queries = [q for q in final_queries if q in df.index]
            df_filtered = df.loc[common_queries]
            
            # 1. Optimization Time
            if 'opt_time' in df_filtered.columns:
                stats['opt'][method][bench] = get_stats(df_filtered['opt_time'])
                
            # 2. Execution Time
            if 'exec_time' in df_filtered.columns:
                 stats['exec'][method][bench] = get_stats(df_filtered['exec_time'])

            # Execution speedup of metaDecomp over X
            if method != 'metadecomp':
                meta_aligned = meta_df.loc[df_filtered.index]
                if 'exec_time' in df_filtered.columns and 'exec_time' in meta_aligned.columns:
                    t_other_exec = df_filtered['exec_time']
                    t_meta_exec = meta_aligned['exec_time']
                    
                    ratios_exec = []
                    for q in df_filtered.index:
                        m_val = t_meta_exec.loc[q]
                        o_val = t_other_exec.loc[q]
                        if m_val > 0 and o_val > 0:
                            ratios_exec.append(o_val / m_val)
                    
                    if ratios_exec:
                        gm_exec = gmean(ratios_exec)
                        median_exec = np.median(ratios_exec)
                        speedup_exec_series = pd.Series(ratios_exec)
                        p95_exec = speedup_exec_series.quantile(0.95)
                        p99_exec = speedup_exec_series.quantile(0.99)
                        stats['exec_speedup'][method][bench] = f"{gm_exec:.2f}x & {median_exec:.2f}x & {p95_exec:.2f}x & {p99_exec:.2f}x"

            # 3. Overall Time
            # DuckDB only has total_time usually, or we compute it?
            # User check: duckdb has total_time (from head cmd).
            col_total = 'total_time'
            
            if col_total in df_filtered.columns:
                 stats['total'][method][bench] = get_stats(df_filtered[col_total])
            
            # 4. Overall Speedup of MetaDecomp over X
            # Speedup = X.total / Meta.total
            # Skip if method is metadecomp
            if method != 'metadecomp':
                # We need aligned series
                # meta_df is already filtered? No, meta_df is global for bench.
                # Align meta_df to df_filtered
                meta_aligned = meta_df.loc[df_filtered.index]
                
                # Ensure we have total_time
                if 'total_time' in df_filtered.columns and 'total_time' in meta_aligned.columns:
                    t_other = df_filtered['total_time']
                    t_meta = meta_aligned['total_time']
                    
                    # Avoid division by zero
                    # speedups = t_other / t_meta
                    # geometric mean
                    
                    ratios = []
                    for q in df_filtered.index:
                        m_val = t_meta.loc[q]
                        o_val = t_other.loc[q]
                        if m_val > 0 and o_val > 0:
                            ratios.append(o_val / m_val)
                    
                    if ratios:
                        gm = gmean(ratios)
                        # The screenshot shows empty cells. Let's assume just Mean column is populated for speedup, or maybe empty for others.
                        # Re-reading: "For speedups, calculate the geometric mean... Other mean values are arithemtic mean."
                        # Implies we put GeoMean in the 'Mean' column.
                        # What about 95th/99th? Maybe blank? Or maybe 95th percentile of speedup?
                        # I'll calculate 95th/99th of speedup distribution too, why not? It fits the table.
                        median = np.median(ratios)
                        
                        speedup_series = pd.Series(ratios)
                        p95 = speedup_series.quantile(0.95)
                        p99 = speedup_series.quantile(0.99)
                        stats['speedup'][method][bench] = f"{gm:.2f}x & {median:.2f}x & {p95:.2f}x & {p99:.2f}x"
                        

    # Generate LaTeX
    print(r"\begin{tabular}{cc|cccc|cccc|cccc}")
    print(r"    \hline")
    print(r"    \multicolumn{2}{r|}{\textbf{Benchmark $\rightarrow$}}")
    print(r"        & \multicolumn{4}{c|}{\textbf{DSB}} & \multicolumn{4}{c|}{\textbf{Musicbrainz}} & \multicolumn{4}{c}{\textbf{Subgraph Matching}} \\")
    print(r"    \textbf{Metric $\downarrow$} & \textbf{Method $\downarrow$}")
    print(r"        & Mean & Median & 95th & 99th & Mean & Median & 95th & 99th & Mean & Median & 95th & 99th \\")
    print(r"    \hline")

    def print_section_header(title, multi_row_count):
        print(r"    \multirow{" + str(multi_row_count) + r"}{*}{" + r"\bf\shortstack{" + title + r"}}")

    def bold_row_values(val):
        # val is "X & Y & Z". We want "\textbf{X} & \textbf{Y} & \textbf{Z}"
        if not val or val.strip() == "": return "& &"
        parts = val.split('&')
        bolded = [r"\textbf{" + p.strip() + "}" for p in parts]
        return " & ".join(bolded)

    # Optimization Time
    print_section_header("Optimization\\\\time", len(METHODS))
    for method in METHODS:
        row_str = f"        & {METHOD_LABELS[method]}  & "
        parts = []
        for bench in BENCHMARK_ORDER:
             val = stats['opt'][method][bench]

             if val == "": val = "& & " # Empty 3 columns
             
            #  if method == 'metadecomp':
                #  val = bold_row_values(val)
                 
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

             if val == "": val = "& & "
             
            #  if method == 'metadecomp':
                #  val = bold_row_values(val)

             parts.append(val)
        row_str += " & ".join(parts) + r" \\"
        print(row_str)
    print(r"    \hline")

    speedup_methods = [m for m in METHODS if m != 'metadecomp']

    # Execution Speedup
    print_section_header("Execution\\\\speedup over...", len(speedup_methods))
    for method in speedup_methods:
        row_str = f"        & {METHOD_LABELS[method]}  & "
        parts = []
        for bench in BENCHMARK_ORDER:
             val = stats['exec_speedup'][method][bench]

             if val == "": val = "& & "
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

             if val == "": val = "& & "

            #  if method == 'metadecomp':
                #  val = bold_row_values(val)

             parts.append(val)
        row_str += " & ".join(parts) + r" \\"
        print(row_str)
    print(r"    \hline")

    # Overall Speedup
    print_section_header("Overall\\\\speedup over...", len(speedup_methods))
    for method in speedup_methods:
        row_str = f"        & {METHOD_LABELS[method]}  & "
        parts = []
        for bench in BENCHMARK_ORDER:
             val = stats['speedup'][method][bench]

             if val == "": val = "& & "
             parts.append(val)
        row_str += " & ".join(parts) + r" \\"
        print(row_str)
    print(r"    \hline")
    print(r"\end{tabular}")

if __name__ == "__main__":
    generate_table()
