from math import ceil
from matplotlib.ticker import LogLocator, PercentFormatter
from matplotlib import font_manager as fm
import pandas as pd
import matplotlib.pyplot as plt
import numpy as np
import os

from config import results_path, colors, benchmarks, figures_path

bold_font = fm.FontProperties(size=24, weight='semibold')

series_keys = benchmarks
legends = ['DSB', 'JOB', 'Musicbrainz', 'JOBLarge']
hatches = ['..', '--', 'xx', '|']

for baseline in ['dpconv', 'duckdb']:
    for metric in ['exec_time', 'total_time'] if baseline == 'dpconv' else ['total_time']:
        meta_df = {}
        base_df = {}
        duckdb_df = {}
        meta = {}
        base = {}
        speedups = {}
        for benchmark in benchmarks:
            # Load the CSV files
            meta_df[benchmark] = pd.read_csv(os.path.join(results_path, f'metadecomp-opt-{benchmark}.csv'))
            base_df[benchmark] = pd.read_csv(os.path.join(results_path, f'{baseline}{"-opt" if baseline=="dpconv" else ""}-{benchmark}.csv'))
            duckdb_df[benchmark] = pd.read_csv(os.path.join(results_path, f'duckdb-{benchmark}.csv'))

            if metric == 'exec_time':
                base_timeout = (base_df[benchmark]['opt_exec_time'] == 0)
                duckdb_df[benchmark] = duckdb_df[benchmark][~base_timeout]
                meta_df[benchmark] = meta_df[benchmark][~base_timeout]
                base_df[benchmark] = base_df[benchmark][~base_timeout]

            # Cap the values at 3e8
            meta[benchmark] = np.clip(meta_df[benchmark][metric], None, 3e8)
            base[benchmark] = np.clip(base_df[benchmark]["opt_exec_time" if metric == "exec_time" else metric], None, 3e8)

            size = min(len(meta[benchmark]), len(base[benchmark]))
            meta[benchmark] = meta[benchmark][:size]
            base[benchmark] = base[benchmark][:size]

            # Compute speedups
            speedups[benchmark] = 1.0 * base[benchmark] / meta[benchmark]

            # Remove 0s
            speedups[benchmark] = speedups[benchmark][speedups[benchmark] > 0]

        if metric == 'exec_time':
            plt.rcParams['font.size'] = 24
            plt.figure(figsize=(10, 2))
        else:
            plt.rcParams['font.size'] = 32
            plt.figure(figsize=(14, 3.5))

        # numeric bin edges (may contain large gaps)
        edges = np.concatenate([np.arange(-0.05, 2.06, 0.1), [10, 100, 1000, 10000, 100000]])

        series = [speedups[k] for k in series_keys]

        # compute counts per numeric bin for each series
        counts_list = []
        for s in series:
            if len(s) == 0:
                counts = np.zeros(len(edges) - 1, dtype=int)
            else:
                counts, _ = np.histogram(s, bins=edges)
            counts_list.append(counts)

        # normalize to fractions of the total (so stacked bars sum to 1 across all bins & series)
        total_all = sum(counts_list).sum()
        if total_all > 0:
            stacked_props = [counts / total_all for counts in counts_list]
        else:
            stacked_props = [np.zeros(len(edges) - 1) for _ in counts_list]

        # positions: equally spaced categorical bins (one position per numeric bin)
        num_bins = len(edges) - 1
        positions = np.arange(num_bins)  # centers

        # plot stacked bars at equal spacing
        width = 1  # visual width for a stacked bar (in categorical units)
        bottom = np.zeros(num_bins)
        for i, (props, hatch) in enumerate(zip(stacked_props, hatches)):
            plt.bar(positions, props, bottom=bottom, width=width, align='center',
                    label=legends[i], color=colors[i], hatch=hatch, edgecolor='black', linewidth=0.4)
            bottom += props

        # build readable x tick labels from numeric edges
        def fmt_val(x):
            if x >= 1000:
                return f"{int(x)}"
            return f"{x:g}"

        # place ticks at bin boundaries so bars (centered at positions) sit between ticks
        # choose ticks: every 0.2 in the small range and all powers of ten in the large range
        small_step = 0.2
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
        ax.set_xlim(0.0, num_bins)                   # view limits include all boundaries

        if metric == 'total_time':
            if baseline == 'dpconv':
                plt.xlabel('(a) metaDecomp over DPconv', weight='bold')
                legend_font = fm.FontProperties(size=24, weight='semibold')
                plt.legend(framealpha=0.5, labelspacing=0.3, prop=legend_font)
            else:
                plt.xlabel('(b) metaDecomp over DuckDB', weight='bold')
            
        plt.ylabel('Frequency', weight='bold')

        plt.gca().yaxis.set_major_formatter(PercentFormatter(1, 0))

        # Show the plot
        plt.tight_layout(pad=0)
        plt.savefig(os.path.join(figures_path, f'{metric.removesuffix("_time")}-speedup-over-{baseline}.pdf'), format='pdf')
