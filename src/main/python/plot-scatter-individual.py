import pandas as pd
import matplotlib.pyplot as plt
from matplotlib import font_manager as fm
import numpy as np
import os

from config import results_path, figures_path, yellow_color

plt.rcParams['font.size'] = 18

bold_font = fm.FontProperties(size=15, weight='semibold')

for baseline in ['dpconv', 'duckdb']:
    for benchmark in ['dsb', 'job-original', 'musicbrainz', 'job-large']:

        # Load the CSV files
        meta_df = pd.read_csv(os.path.join(results_path, f'metadecomp-opt-{benchmark}.csv'))
        dpconv_df = pd.read_csv(os.path.join(results_path, f'dpconv-opt-{benchmark}.csv'))
        duckdb_df = pd.read_csv(os.path.join(results_path, f'duckdb-{benchmark}.csv'))

        # Cap the values at 3e8
        base = np.clip(duckdb_df['total_time'] if baseline == 'duckdb' else dpconv_df['total_time'], None, 3e8)
        meta = np.clip(meta_df['total_time'], None, 3e8)
        dpconv = np.clip(dpconv_df['total_time'], None, 3e8)

        if len(base) != len(meta) or len(base) != len(dpconv):
            print(f"Warning: Different sizes for {benchmark}: duckdb {len(base)}, meta {len(meta)}, dpconv {len(dpconv)}")

        size = min(len(base), len(meta), len(dpconv))
        base = base[:size]
        meta = meta[:size]
        dpconv = dpconv[:size]

        # Drop entries where all are timeout
        both_timeout = (meta >= 3e8 - 1e-5) & (dpconv >= 3e8 - 1e-5) & (base >= 3e8 - 1e-5)
        meta = meta[~both_timeout] / 1000
        dpconv = dpconv[~both_timeout] / 1000
        base = base[~both_timeout] / 1000

        # Create the scatter plot
        plt.figure(figsize=(5, 5))
        if baseline == 'duckdb':
            plt.scatter(base, meta, label='metaDecomp vs DuckDB', marker='o', alpha=0.7)
            plt.scatter(base, dpconv, label='DPconv vs DuckDB', marker='^', alpha=0.7)
        else:
            plt.scatter(dpconv, meta, label='metaDecomp vs DPconv', marker='o', alpha=0.7)

        # Add the reference line y = x
        xmin, xmax = (base.min(), base.max()) if baseline == 'duckdb' else (dpconv.min(), dpconv.max())
        plt.plot([xmin, xmax], [xmin, xmax], linestyle='--', color=yellow_color)

        # Set log scale for both axes
        plt.xscale('log')
        plt.yscale('log')

        # Add labels, legend, and title
        if baseline == 'duckdb':
            plt.xlabel('DuckDB (ms)', weight='bold')
            plt.ylabel('metaDecomp / DPconv (ms)', weight='bold')
            plt.legend(framealpha=0.5, labelspacing=0, prop=bold_font)
        else:
            plt.xlabel('DPconv (ms)', weight='bold')
            plt.ylabel('metaDecomp (ms)', weight='bold')

        if meta.max() >= 3e5 - 1e-5 or (baseline == 'duckdb' and dpconv.max() >= 3e5 - 1e-5):
            plt.axhline(y=3e5, color='dimgray', linestyle=':')
            plt.text(y=3e5, x=xmin, s='Timeout (5 min)', color='dimgray', va='bottom', ha='left')
        if (baseline == 'duckdb' and base.max() >= 3e5 - 1e-5) or (baseline == 'dpconv' and dpconv.max() >= 3e5 - 1e-5):
            plt.axvline(x=3e5, color='dimgray', linestyle=':')
            plt.text(x=3e5, y=xmin, s=f'{"DuckDB" if baseline == "duckdb" else "DPconv"} timeout (5 min)', color='dimgray', va='bottom', ha='left', rotation=270)

        # Show the plot
        plt.tight_layout(pad=0)
        plt.savefig(os.path.join(figures_path, f'runtime-{benchmark}-over-{baseline}.pdf'), format='pdf')
        plt.show()
