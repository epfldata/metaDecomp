import pandas as pd
import matplotlib.pyplot as plt
from matplotlib import font_manager as fm
from matplotlib.ticker import MaxNLocator
import os

from config import results_path

plt.rcParams['font.size'] = 16

bold_font = fm.FontProperties(size=12, weight='semibold')

for variant in ['job-original-exact', 'job-large-estimate']:
    # Load the CSV files
    meta_df = pd.read_csv(os.path.join(results_path, f'metadecomp-opt-{variant}.csv'))
    dpconv_df = pd.read_csv(os.path.join(results_path, f'dpconv-opt-{variant}.csv'))

    meta_df['num_rels'] = pd.to_numeric(meta_df['num_rels'], errors='coerce')
    dpconv_df['num_rels'] = pd.to_numeric(dpconv_df['num_rels'], errors='coerce')

    meta_df = meta_df[['num_rels', 'total_opt_time']]
    dpconv_df = dpconv_df[['num_rels', 'opt_time']]

    # Group data by num_rels and calculate mean and standard deviation
    meta_grouped = meta_df.groupby('num_rels')
    num_rels = meta_grouped.mean().index
    meta_mean = meta_grouped['total_opt_time'].mean()
    meta_std = meta_grouped['total_opt_time'].std()

    dpconv_grouped = dpconv_df.groupby('num_rels')
    dpconv_mean = dpconv_grouped['opt_time'].mean()
    dpconv_std = dpconv_grouped['opt_time'].std()

    # Group data by num_rels and calculate mean, min, and max
    meta_min = meta_grouped['total_opt_time'].min()
    meta_max = meta_grouped['total_opt_time'].max()
    dpconv_min = dpconv_grouped['opt_time'].min()
    dpconv_max = dpconv_grouped['opt_time'].max()

    # Calculate error bars as the range (mean to min/max)
    meta_error = [meta_mean - meta_min, meta_max - meta_mean]
    dpconv_error = [dpconv_mean - dpconv_min, dpconv_max - dpconv_mean]

    # Create the plot
    if variant == 'original':
        plt.figure(figsize=(6, 3))
    else:
        plt.figure(figsize=(5, 4))
    plt.errorbar(num_rels, meta_mean, yerr=meta_error, label='metaDecomp', fmt='o', alpha=0.85, capsize=3)
    plt.errorbar(num_rels, dpconv_mean, yerr=dpconv_error, label='DPconv', fmt='^', alpha=0.85, capsize=3)

    ax = plt.gca()
    ax.xaxis.set_major_locator(MaxNLocator(integer=True))

    # Set log scale for the y-axis
    plt.yscale('log')

    # Add labels, legend, and title
    plt.xlabel('Number of relations', weight='bold')
    plt.ylabel('Optimization time (μs)', weight='bold')
    plt.legend(framealpha=0.85, labelspacing=0.3, prop=bold_font)

    if variant == 'large-estimate':
        # Add a horizontal line at 3600000000 with a label 'Timeout'
        plt.axhline(y=3600000000, color='dimgray', linestyle=':')
        plt.text(x=num_rels.min(), y=3600000000, s='Timeout (1 hour)', color='dimgray', va='top', ha='left')

    # Show the plot
    plt.tight_layout(pad=0)
    plt.savefig(os.path.join(results_path, f'opt-time-{variant}.pdf'), format='pdf')
    plt.show()
