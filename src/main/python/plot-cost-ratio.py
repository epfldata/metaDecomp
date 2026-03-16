from duckdb import df
from matplotlib.ticker import PercentFormatter
import pandas as pd
import matplotlib.pyplot as plt
import numpy as np
import os

from config import results_path, figures_path, green_color, dark_green_color

plt.rcParams['font.size'] = 31

save_path = os.path.join(figures_path, 'cost-ratios')

if not os.path.exists(save_path):
    os.makedirs(save_path)

for benchmark in ['dsb', 'job-original', 'musicbrainz', 'job-large']:

    # Paths to your CSV files
    metadecomp_path = os.path.join(results_path, f'metadecomp-opt-{benchmark}.csv')
    dpconv_path = os.path.join(results_path, f'dpconv-opt-{benchmark}.csv')

    # Read the CSV files
    meta_df = pd.read_csv(metadecomp_path)
    dpconv_df = pd.read_csv(dpconv_path)

    # Load the CSV files
    duckdb_df = pd.read_csv(os.path.join(results_path, f'duckdb-{benchmark}.csv'))

    # Merge on 'query'
    merged = pd.merge(meta_df, dpconv_df, on='query', suffixes=('_meta', '_dpconv'))

    merged = merged[merged['opt_exec_time'] > 0]

    # Compute the ratio
    ratios = (merged['cout_opt_intm'] + merged['cost_in']) / merged['cout_cost']

    # Cap the data at 0.99 - so that the ones with 1.0 are shown in the range 0.9-1.0
    ratios = np.clip(ratios, None, 0.99)

    # Create the histogram
    plt.figure(figsize=(5.5, 3))
    minBin = int(ratios.min() * 10.0) / 10.0
    r = (minBin, 1.0)
    coeff = 10.0 if ratios.min() < 0.3 else 20.0 if ratios.min() < 0.9 else 100.0
    bins = int((1.0 - minBin) * coeff + 1e-5)
    plt.hist(ratios, weights=np.ones(len(ratios)) / len(ratios), bins=bins, range=r, color=green_color, edgecolor=dark_green_color)

    plt.gca().yaxis.set_major_formatter(PercentFormatter(1, 0))

    # Add labels and title
    plt.xlabel('Ratio', weight='bold')
    plt.ylabel('Frequency', weight='bold')

    # Show the plot
    plt.tight_layout(pad=0)
    plt.savefig(os.path.join(save_path, f'cost-ratios-{benchmark}.pdf'), format='pdf')
    plt.show()
