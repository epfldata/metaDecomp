from matplotlib.ticker import LogLocator, PercentFormatter
import pandas as pd
import matplotlib.pyplot as plt
import numpy as np
import os

from config import results_path, green_color, dark_green_color, benchmarks, figures_path

plt.rcParams['font.size'] = 18

save_path = figures_path + '/speedups-overall-individual'

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

                # Create the histogram
                plt.figure(figsize=(6, 3))
                # Cap the speedups at 2.0 - so that the ones with >2.0 are shown in the range 1.5-2.0
                speedups = np.clip(speedups, None, 2.01)
                plt.hist(speedups, weights=np.ones(len(speedups)) / len(speedups), bins=11, range=(0, 2.2), color=green_color, edgecolor=dark_green_color)
            else:
                plt.figure(figsize=(6, 3))
                # compute decade boundaries so bins align exactly at powers of ten
                min_exp = np.floor(np.log10(speedups.min()))
                max_exp = np.ceil(np.log10(speedups.max()))
                # ensure at least one decade
                if max_exp <= min_exp:
                    max_exp = min_exp + 1
                num_decades = int(max_exp - min_exp)
                # number of bars per decade (adjustable)
                bars_per_decade = 5 if num_decades >= 3 else 10
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

            plt.gca().yaxis.set_major_formatter(PercentFormatter(1, 0))

            # Add labels and title
            plt.xlabel('Speedup', weight='bold')
            # plt.xlabel(f'{"Execution" if metric == "exec_time" else "Overall"} speedup over {"DPconv" if baseline == "dpconv" else "DuckDB"}', weight='bold')
            plt.ylabel('Frequency', weight='bold')

            # Show the plot
            plt.tight_layout(pad=0)

            if not os.path.exists(save_path):
                os.makedirs(save_path)
            plt.savefig(os.path.join(save_path, f'speedups-over-{baseline}-{metric}-{benchmark}.pdf'), format='pdf')
            # plt.show()
