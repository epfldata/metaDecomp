import pandas as pd
import matplotlib.pyplot as plt
from matplotlib import font_manager as fm
import os
import numpy as np
import matplotlib.patches as mpatches

from config import results_path, figures_path, colors, dark_orange

plt.rcParams['font.size'] = 24
legend_font = fm.FontProperties(size=18, weight='semibold')
label_font = fm.FontProperties(size=22)

bold_font = fm.FontProperties(size=12, weight='semibold')

# Load the CSV file
meta_dsb = pd.read_csv(os.path.join(results_path, 'metadecomp-enum-dsb.csv'))
meta_original = pd.read_csv(os.path.join(results_path, 'metadecomp-enum-job-original.csv'))
meta_musicbrainz = pd.read_csv(os.path.join(results_path, 'metadecomp-enum-musicbrainz.csv'))
meta_large = pd.read_csv(os.path.join(results_path, 'metadecomp-enum-job-large.csv'))

meta_all = pd.concat([meta_dsb, meta_original, meta_musicbrainz, meta_large])

meta_all = meta_all[meta_all['num_join_trees'] > 0]

# Ensure num_join_trees is numeric, dropping non-numeric values
meta_all['num_join_trees'] = pd.to_numeric(meta_all['num_join_trees'], errors='coerce')
meta_all = meta_all.dropna(subset=['num_join_trees'])
print(meta_all['num_join_trees'])
meta_all['num_join_trees'] = meta_all['num_join_trees'].astype(int)

# Load the CSV file
ssp_dsb = pd.read_csv(os.path.join(results_path, 'sparksqlplus-enum-dsb.csv'))
ssp_original = pd.read_csv(os.path.join(results_path, 'sparksqlplus-enum-job-original.csv'))
ssp_musicbrainz = pd.read_csv(os.path.join(results_path, 'sparksqlplus-enum-musicbrainz.csv'))
ssp_large = pd.read_csv(os.path.join(results_path, 'sparksqlplus-enum-job-large.csv'))

ssp_all = pd.concat([ssp_original, ssp_dsb, ssp_musicbrainz, ssp_large])

# Filter meta_data to keep only rows with 'query' values that exist in ssp_data
# meta_data = meta_data[meta_data['query'].isin(ssp_data['query'])]

# Create a mapping from query to num_join_trees in meta_data
query_to_num_join_trees = meta_all.set_index('query')['num_join_trees'].to_dict()

# Map the num_join_trees to ssp_data using the query attribute
ssp_all['num_join_trees'] = ssp_all['query'].map(query_to_num_join_trees)

# Drop the 'query' column if it exists
meta_all = meta_all.drop(columns=['query'], errors='ignore')



meta_all['time_ms'] = meta_all['time_us'] / 1000  # Convert to ms




# Create the scatter plot
plt.figure(figsize=(10, 3))

plt.scatter(meta_all['num_join_trees'], meta_all['time_ms'], label = 'metaDecomp', alpha = 0.70, s = 30, marker='o', color = colors[0])
plt.scatter(ssp_all['num_join_trees'], ssp_all['time_ms'], label = 'GYO reduction', alpha = 0.70, s = 60, marker='^', color = colors[1])

plt.legend(framealpha=0.85, labelspacing=0.3, prop=legend_font)

# Set log scale for both axes
plt.xscale('log')
plt.yscale('log')

# Add labels, legend, and title
plt.xlabel('Number of join trees', weight='bold')
plt.ylabel('Enumeration\ntime (ms)', weight='bold')
# plt.legend(framealpha=0.85, labelspacing=0, prop=bold_font)

plt.axhline(y=3600e3, color='dimgray', linestyle=':')
plt.text(y=3600e3, x=(meta_all['num_join_trees'].max())**0.5, s='Timeout (1 hour)', color='dimgray', va='top', ha='center', fontproperties=label_font)

# Show the plot
plt.tight_layout(pad=0)
plt.savefig(os.path.join(figures_path, f'enum-time-vs-num-jts.pdf'), format='pdf')
plt.show()
