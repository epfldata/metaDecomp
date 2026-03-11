from matplotlib.ticker import LogLocator, PercentFormatter
import pandas as pd
import matplotlib.pyplot as plt
import numpy as np
import os
from math import ceil

from config import results_path, green_color, dark_green_color, benchmarks, figures_path

plt.rcParams['font.size'] = 31

for benchmark in benchmarks:
    for baseline in ['dpconv', 'duckdb', 'uniondp', 'yanplus', 'learned-rewrite', 'llm-r2']:
        for metric in ['exec_time', 'total_time'] if baseline == 'dpconv' else ['total_time']:
            # Load the CSV files
            meta_df = pd.read_csv(os.path.join(results_path, f'metadecomp-opt-{benchmark}.csv'))
            base_df = pd.read_csv(os.path.join(results_path, f'{baseline}{"" if baseline=="duckdb" else "-opt"}-{benchmark}.csv'))
            duckdb_df = pd.read_csv(os.path.join(results_path, f'duckdb-{benchmark}.csv'))

            # Cap the values at 3e8
            meta = np.clip(meta_df[metric], None, 3e8)
            base = np.clip(base_df["opt_exec_time" if metric == "exec_time" else metric], None, 3e8)

            if metric == 'exec_time':
                base_timeout = base_df['opt_exec_time'] == 0
                duckdb_df = duckdb_df[~base_timeout]
                meta_df = meta_df[~base_timeout]
                base_df = base_df[~base_timeout]

            size = min(len(meta), len(base))
            meta = meta[:size]
            base = base[:size]

            # Compute speedups
            speedups = 1.0 * base / meta

            # Remove 0s
            speedups = speedups[speedups > 0]

            if metric == 'exec_time':
                plt.figure(figsize=(11, 3.75))
            else:
                plt.figure(figsize=(9, 3.5))

            if baseline == 'learned-rewrite' or baseline == 'llm-r2':
                # compute decade boundaries so bins align exactly at powers of ten
                min_exp = -3 # np.floor(np.log10(speedups.min()))
                max_exp = 5 # np.ceil(np.log10(speedups.max()))
                # ensure at least one decade
                if max_exp <= min_exp:
                    max_exp = min_exp + 1
                num_decades = int(max_exp - min_exp)
                # number of bars per decade (adjustable)
                bars_per_decade = 4 # 5 if num_decades >= 3 else 10
                num_bins = num_decades * bars_per_decade
                bins = np.logspace(min_exp, max_exp, num_bins + 1)

                plt.hist(speedups, weights=np.ones(len(speedups)) / len(speedups),
                         bins=bins, color=green_color, edgecolor=dark_green_color)
                plt.xscale('log')

                # set major ticks at powers of ten and minor ticks to show subdivisions
                from matplotlib.ticker import LogLocator
                ax = plt.gca()
                ax.xaxis.set_major_locator(LogLocator(base=10.0))
                # subs: equally spaced multipliers inside a decade (between 1 and 10)
                subs = np.linspace(1, 10, bars_per_decade + 1, endpoint=False)[1:]
                ax.xaxis.set_minor_locator(LogLocator(base=10.0, subs=subs))

            else:

                # numeric bin edges (may contain large gaps)
                edges = np.concatenate([np.arange(-0.05, 2.06, 0.1), [10, 100, 1000, 10000, 100000]])

                # compute counts per numeric bin for each series
                if len(speedups) == 0:
                    counts = np.zeros(len(edges) - 1, dtype=int)
                else:
                    counts, _ = np.histogram(speedups, bins=edges)

                # normalize to fractions of the total (so stacked bars sum to 1 across all bins & series)
                total_all = counts.sum()
                if total_all > 0:
                    props = counts / total_all
                else:
                    props = np.zeros(len(edges) - 1)

                # positions: equally spaced categorical bins (one position per numeric bin)
                num_bins = len(edges) - 1
                positions = np.arange(num_bins)  # centers

                # plot stacked bars at equal spacing
                width = 1  # visual width for a stacked bar (in categorical units)
                bottom = np.zeros(num_bins)
                plt.bar(positions, props, bottom=bottom, width=width, align='center',
                        color=green_color, edgecolor=dark_green_color, linewidth=0.4)
                bottom += props

                # build readable x tick labels from numeric edges
                def fmt_val(x):
                    if x >= 1000:
                        return f"{int(x)}"
                    return f"{x:g}"

                # place ticks at bin boundaries so bars (centered at positions) sit between ticks
                # choose ticks: every 0.2 in the small range and all powers of ten in the large range
                small_step = 0.5
                small_max = 2.0
                powers = [10, 1000, 100000]

                # find indices in edges corresponding to desired tick values
                tick_indices = []
                for v in np.arange(-0.05, small_max + 1e-9, small_step):
                    idx = np.where(np.isclose(edges, v))[0]
                    if idx.size:
                        tick_indices.append(int(idx[0]))
                for p in powers:
                    idx = np.where(np.isclose(edges, p))[0]
                    if idx.size:
                        tick_indices.append(int(idx[0]) - 0.5)

                tick_indices = sorted(set(tick_indices))

                # format labels: show powers of ten as 10^n and small numbers normally
                def edge_label(e):
                    if e == 0:
                        return "0"
                    if e >= 10:
                        log10 = np.log10(e)
                        if np.isclose(log10, np.round(log10)):
                            return rf"$10^{{{int(np.round(log10))}}}$"
                    return fmt_val(e + 0.05)

                tick_labels = [edge_label(edges[ceil(i)]) for i in tick_indices]

                ax = plt.gca()
                ax.set_xticks(tick_indices)
                # tick_font = fm.FontProperties(size=25)
                ax.set_xticklabels(tick_labels, ha='center')
                ax.set_xlim(-0.5, num_bins)                   # view limits include all boundaries

            plt.gca().yaxis.set_major_formatter(PercentFormatter(1, 0))

            # Add labels and title
            plt.xlabel('Speedup', weight='bold')
            # plt.xlabel(f'{"Execution" if metric == "exec_time" else "Overall"} speedup over {"DPconv" if baseline == "dpconv" else "DuckDB"}', weight='bold')
            plt.ylabel('Frequency', weight='bold')

            # Show the plot
            plt.tight_layout(pad=0)

            save_path = os.path.join(figures_path, 'exec-speedup' if metric == 'exec_time' else 'overall-speedup-individual')

            if not os.path.exists(save_path):
                os.makedirs(save_path)
            plt.savefig(os.path.join(save_path, f'{'exec' if metric == 'exec_time' else 'overall'}-speedups-over-{baseline}-{benchmark}.pdf'), format='pdf')
            # plt.show()
