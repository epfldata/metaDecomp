import pandas as pd
import matplotlib.pyplot as plt
import numpy as np
import os

from config import results_path, epfl_leman_color, epfl_canard_color

plt.rcParams['font.size'] = 16

# Paths to your CSV files
metadecomp_path = os.path.join(results_path, 'metadecomp-opt-job-original-exact.csv')
dpconv_path = os.path.join(results_path, 'dpconv-results-job-original-exact.csv')

# Read the CSV files
meta_df = pd.read_csv(metadecomp_path)
dpconv_df = pd.read_csv(dpconv_path)

# Merge on 'query'
merged = pd.merge(meta_df, dpconv_df, on='query')

# Compute the ratio
ratios = (merged['cout_opt_intm'] + merged['cost_inout']) / merged['cout_cost']

# Cap the data at 0.99 - so that the ones with 1.0 are shown in the range 0.9-1.0
ratios = np.clip(ratios, None, 0.99)

# Create the histogram
plt.figure(figsize=(6, 3))
plt.hist(ratios, bins=10, range=(0, 1.0), color=epfl_leman_color, edgecolor=epfl_canard_color)

# Add labels and title
plt.xlabel('Ratio', weight='bold')
plt.ylabel('Frequency', weight='bold')

# Show the plot
plt.tight_layout(pad=0)
plt.savefig(os.path.join(results_path, 'cost-ratios.pdf'), format='pdf')
plt.show()
