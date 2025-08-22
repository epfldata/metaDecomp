import pandas as pd
import matplotlib.pyplot as plt
from matplotlib import font_manager as fm
import numpy as np
import os

from config import results_path

plt.rcParams['font.size'] = 14

bold_font = fm.FontProperties(size=12, weight='semibold')

for fig in ['job-original-exact']:
    # Load the CSV files
    meta_df = pd.read_csv(os.path.join(results_path, f'metadecomp-opt-{fig}.csv'))
    dpconv_df = pd.read_csv(os.path.join(results_path, f'dpconv-opt-{fig}.csv'))

    # Cap the values at 3e8
    meta = np.clip(meta_df['total_time'], None, 3e8)
    dpconv = np.clip(dpconv_df['total_time'], None, 3e8)

    # Create the scatter plot
    plt.figure(figsize=(3, 3))
    plt.scatter(dpconv, meta, label='metaDecomp', marker='o', alpha=0.85, s=10)

    # Add the reference line y = x
    xmin, xmax = dpconv.min(), dpconv.max()
    plt.plot([xmin, xmax], [xmin, xmax], linestyle=':', label='DPconv', color='darkorange')

    # Set log scale for both axes
    plt.xscale('log')
    plt.yscale('log')

    # Add labels, legend, and title
    plt.xlabel('DPconv run time (μs)', weight='bold')
    plt.ylabel('metaDecomp run time (μs)', weight='bold')
    plt.legend(framealpha=0.85, labelspacing=0, prop=bold_font)

    # Show the plot
    plt.tight_layout(pad=0)
    plt.savefig(os.path.join(results_path, f'runtime-{fig}.pdf'), format='pdf')
    plt.show()
