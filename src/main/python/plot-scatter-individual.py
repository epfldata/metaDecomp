import pandas as pd
import matplotlib.pyplot as plt
from matplotlib import font_manager as fm
import numpy as np
import os

from config import results_path, figures_path, yellow_color

plt.rcParams['font.size'] = 19

bold_font = fm.FontProperties(size=15, weight='semibold')

save_path = os.path.join(figures_path, 'scatter-individual')

for (baseline, baseline_full_name) in [('dpconv', 'DPconv'), ('duckdb', 'DuckDB'), ('uniondp', 'UnionDP'), ('yanplus', 'Yannakakis$^+$'), ('learned-rewrite', 'Learned Rewrite'), ('llm-r2', 'LLM-R$^2$')]:
    for benchmark in ['dsb', 'job-original', 'musicbrainz', 'job-large']:

        # Load the CSV files
        meta_df = pd.read_csv(os.path.join(results_path, f'metadecomp-opt-{benchmark}.csv'))
        base_df = pd.read_csv(os.path.join(results_path, f'{baseline}{"" if baseline == "duckdb" else "-opt"}-{benchmark}.csv'))
        duckdb_df = pd.read_csv(os.path.join(results_path, f'duckdb-{benchmark}.csv'))
        dpconv_df = pd.read_csv(os.path.join(results_path, f'dpconv-opt-{benchmark}.csv'))

        if (benchmark == 'job-large'):

            small = duckdb_df['total_time'] < 5e4

            duckdb_df = duckdb_df[~small]
            meta_df = meta_df[~small]
            base_df = base_df[~small]
        
        # Rename columns to avoid collision and prepare for merge
        meta_df = meta_df[['query', 'total_time']].rename(columns={'total_time': 'meta_time'})
        base_df = base_df[['query', 'total_time']].rename(columns={'total_time': 'base_time'})
        duckdb_df = duckdb_df[['query', 'total_time']].rename(columns={'total_time': 'duckdb_time'})
        dpconv_df = dpconv_df[['query', 'total_time']].rename(columns={'total_time': 'dpconv_time'})

        # Merge dataframes using inner join to align by query
        merged = meta_df.merge(base_df, on='query').merge(duckdb_df, on='query').merge(dpconv_df, on='query')
        
        # Cap the values at 3e8
        meta = np.clip(merged['meta_time'], None, 3e8)
        base = np.clip(merged['base_time'], None, 3e8)
        duckdb = np.clip(merged['duckdb_time'], None, 3e8)
        dpconv = np.clip(merged['dpconv_time'], None, 3e8)

        # Drop entries where all are timeout
        all_timeout = (meta >= 3e8 - 1e-5) & (duckdb >= 3e8 - 1e-5) & (dpconv >= 3e8 - 1e-5)
        # print(all_timeout)
        meta = meta[~all_timeout] / 1000
        base = base[~all_timeout] / 1000
        duckdb = duckdb[~all_timeout] / 1000
        dpconv = dpconv[~all_timeout] / 1000

        # Create the scatter plot
        plt.figure(figsize=(3.5, 3.5))
        # if baseline == 'duckdb':
        #     plt.scatter(base, meta, label='metaDecomp vs DuckDB', marker='o', alpha=0.7)
        #     plt.scatter(base, dpconv, label='DPconv vs DuckDB', marker='^', alpha=0.7)
        # else:
        #     plt.scatter(dpconv, meta, label='metaDecomp vs DPconv', marker='o', alpha=0.7)
        plt.scatter(base, meta, marker='o', alpha=0.7)

        # Add the reference line y = x
        xmin, xmax = (min(base.min(), meta.min()), max(base.max(), meta.max()))
        ymin, ymax = (min(base.min(), meta.min()), max(base.max(), meta.max()))
        plt.plot([xmin, xmax], [xmin, xmax], linestyle='--', color=yellow_color)

        # Set log scale for both axes
        plt.xscale('log')
        plt.yscale('log')

        # Add labels, legend, and title
        plt.xlabel(f'{baseline_full_name} (ms)', weight='bold')
        plt.ylabel('metaDecomp (ms)', weight='bold')

        if meta.max() >= 3e5 - 1e-5:
            plt.axhline(y=3e5, color='dimgray', linestyle=':')
            plt.text(y=3e5, x=xmin, s='Timeout (5 min)', color='dimgray', va='bottom', ha='left')
        if base.max() >= 3e5 - 1e-5:
            plt.axvline(x=3e5, color='dimgray', linestyle=':')
            plt.text(x=3e5, y=ymin, s=f'Timeout (5 min)', color='dimgray', va='bottom', ha='left', rotation=270)

        # Show the plot
        plt.tight_layout(pad=0)
        if not os.path.exists(save_path):
            os.makedirs(save_path)
        plt.savefig(os.path.join(save_path, f'scatter-{benchmark}-over-{baseline}.pdf'), format='pdf')
        # plt.show()
