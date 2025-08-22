import pandas as pd
import matplotlib.pyplot as plt
from matplotlib import font_manager as fm
import numpy as np
import os

from config import results_path, yellow_color

plt.rcParams['font.size'] = 15

bold_font = fm.FontProperties(size=12, weight='semibold')

for fig in ['job-large-estimate', 'job-large-all-0']:
    # Load the CSV files
    duckdb_df = pd.read_csv(os.path.join(results_path, f'duckdb-job-large.csv'))
    meta_df = pd.read_csv(os.path.join(results_path, f'metadecomp-opt-{fig}.csv'))
    dpconv_df = pd.read_csv(os.path.join(results_path, f'dpconv-opt-{fig}.csv'))

    # Cap the values at 3e8
    duckdb = np.clip(duckdb_df['total_time'], None, 3e8)
    meta = np.clip(meta_df['total_time'], None, 3e8)
    dpconv = np.clip(dpconv_df['total_time'], None, 3e8)

    # Create the scatter plot
    plt.figure(figsize=(5, 5))
    plt.scatter(dpconv, meta, label='metaDecomp', marker='o', alpha=0.85)
    plt.scatter(duckdb, dpconv, label='DPconv', marker='^', alpha=0.85)

    # Add the reference line y = x
    xmin, xmax = duckdb.min(), duckdb.max()
    plt.plot([xmin, xmax], [xmin, xmax], linestyle='--', label='DuckDB', color=yellow_color)

    # Set log scale for both axes
    plt.xscale('log')
    plt.yscale('log')

    # Add labels, legend, and title
    plt.xlabel('DuckDB run time (μs)', weight='bold')
    plt.ylabel('metaDecomp / DPconv / DuckDB run time (μs)', weight='bold')
    plt.legend(framealpha=0.5, labelspacing=0, prop=bold_font)

    plt.axhline(y=3e8, color='dimgray', linestyle=':')
    plt.text(y=3e8, x=xmin, s='Timeout (5 min)', color='dimgray', va='bottom', ha='left')
    plt.axvline(x=3e8, color='dimgray', linestyle=':')
    plt.text(x=3e8, y=xmin, s='DuckDB timeout (5 min)', color='dimgray', va='bottom', ha='left', rotation=270)

    # Show the plot
    plt.tight_layout(pad=0)
    plt.savefig(os.path.join(results_path, f'runtime-{fig}.pdf'), format='pdf')
    plt.show()
