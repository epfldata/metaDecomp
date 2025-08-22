import pandas as pd
import matplotlib.pyplot as plt
from matplotlib import font_manager as fm
import os

from config import results_path

plt.rcParams['font.size'] = 13

bold_font = fm.FontProperties(size=12, weight='semibold')

# Load the CSV file
meta_original = pd.read_csv(os.path.join(results_path, 'metadecomp-enum-job-original.csv'))
meta_large = pd.read_csv(os.path.join(results_path, 'metadecomp-enum-job-large.csv'))

meta_all = pd.concat([meta_original, meta_large])

# Ensure num_rels is numeric, dropping non-numeric values
meta_all['num_rels'] = pd.to_numeric(meta_all['num_rels'], errors='coerce')
meta_all = meta_all.dropna(subset=['num_rels'])

# Load the CSV file
ssp_original = pd.read_csv(os.path.join(results_path, 'sparksqlplus-enum-job-original.csv'))
ssp_large = pd.read_csv(os.path.join(results_path, 'sparksqlplus-enum-job-large.csv'))

ssp_all = pd.concat([ssp_original, ssp_large])

# Filter meta_data to keep only rows with 'query' values that exist in ssp_data
# meta_data = meta_data[meta_data['query'].isin(ssp_data['query'])]

# Create a mapping from query to num_rels in meta_data
query_to_num_rels = meta_all.set_index('query')['num_rels'].to_dict()

# Map the num_rels to ssp_data using the query attribute
ssp_all['num_rels'] = ssp_all['query'].map(query_to_num_rels)

# Drop the 'query' column if it exists
meta_all = meta_all.drop(columns=['query'], errors='ignore')

# Group data by num_rels and calculate mean and standard deviation
meta_all['time_ms'] = meta_all['time_us'] / 1000  # Convert to ms
meta_grouped = meta_all.groupby('num_rels')
meta_num_rels = meta_grouped.mean().index
meta_mean = meta_grouped['time_ms'].mean()
meta_std = meta_grouped['time_ms'].std()

# Group data by num_rels and calculate mean, min, and max
meta_min = meta_grouped['time_ms'].min()
meta_max = meta_grouped['time_ms'].max()

# Calculate error bars as the range (mean to min/max)
meta_error = [meta_mean - meta_min, meta_max - meta_mean]



# Drop the 'query' column if it exists
ssp_all = ssp_all.drop(columns=['query'], errors='ignore')

# Ensure num_rels is numeric, dropping non-numeric values
ssp_all['num_rels'] = pd.to_numeric(ssp_all['num_rels'], errors='coerce')
ssp_all = ssp_all.dropna(subset=['num_rels'])

# Group data by num_rels and calculate mean and standard deviation
ssp_grouped = ssp_all.groupby('num_rels')
ssp_num_rels = ssp_grouped.mean().index
ssp_mean = ssp_grouped['time_ms'].mean()
ssp_std = ssp_grouped['time_ms'].std()

# Group data by num_rels and calculate mean, min, and max
ssp_min = ssp_grouped['time_ms'].min()
ssp_max = ssp_grouped['time_ms'].max()

# Calculate error bars as the range (mean to min/max)
ssp_error = [ssp_mean - ssp_min, ssp_max - ssp_mean]


# Create the plot
plt.figure(figsize=(6, 2.5))
plt.errorbar(meta_num_rels, meta_mean, yerr=meta_error, label='metaDecomp', fmt='o', alpha=0.85, capsize=3)
plt.errorbar(ssp_num_rels, ssp_mean, yerr=ssp_error, label='GYO reduction', fmt='^', alpha=0.85, capsize=3)

# Set log scale for the y-axis
plt.yscale('log')

# Add labels, legend, and title
plt.xlabel('Number of relations', weight='bold')
plt.ylabel('Enumeration time (ms)', weight='bold')
plt.legend(framealpha=0.85, labelspacing=0.3, prop=bold_font)

# Add a horizontal line at 3600e3 with a label 'Timeout'
plt.axhline(y=3600e3, color='gray', linestyle=':')
plt.text(x=meta_num_rels.min(), y=3600e3, s='Timeout (1 hour)', color='gray', va='top', ha='left')

# Show the plot
plt.tight_layout(pad=0)
plt.savefig(os.path.join(results_path, 'enum-time.pdf'), format='pdf')
plt.show()
