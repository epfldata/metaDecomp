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

for benchmark in ['dsb', 'job-original', 'musicbrainz', 'job-large']:

    # Load the CSV file
    meta = pd.read_csv(os.path.join(results_path, f'metadecomp-enum-{benchmark}.csv'))
    meta = meta[meta['num_join_trees'] > 0]
    # Ensure num_join_trees is numeric, dropping non-numeric values
    meta['num_join_trees'] = pd.to_numeric(meta['num_join_trees'], errors='coerce')
    meta = meta.dropna(subset=['num_join_trees'])
    print(meta['num_join_trees'])
    meta['num_join_trees'] = meta['num_join_trees'].astype(int)

    # Load the CSV file
    ssp = pd.read_csv(os.path.join(results_path, f'sparksqlplus-enum-{benchmark}.csv'))

    # Filter meta_data to keep only rows with 'query' values that exist in ssp_data
    # meta_data = meta_data[meta_data['query'].isin(ssp_data['query'])]

    # Create a mapping from query to num_join_trees in meta_data
    query_to_num_join_trees = meta.set_index('query')['num_join_trees'].to_dict()

    # Map the num_join_trees to ssp_data using the query attribute
    ssp['num_join_trees'] = ssp['query'].map(query_to_num_join_trees)

    # Drop the 'query' column if it exists
    meta = meta.drop(columns=['query'], errors='ignore')



    meta['time_ms'] = meta['time_us'] / 1000  # Convert to ms



    min_num_jts = meta['num_join_trees'].min()
    max_time = max(meta['time_ms'].max(), ssp['time_ms'].max())

    # Create the scatter plot
    plt.figure(figsize=(10, 4.5))

    plt.scatter(meta['num_join_trees'], meta['time_ms'], label = 'metaDecomp', alpha = 0.70, s = 40, marker='o', color = colors[0])
    plt.scatter(ssp['num_join_trees'], ssp['time_ms'], label = 'GYO reduction', alpha = 0.70, s = 60, marker='^', color = colors[1])

    plt.legend(framealpha=0.85, labelspacing=0.3, prop=legend_font)

    # Set log scale for both axes
    plt.xscale('log')
    plt.yscale('log')

    # Add labels, legend, and title
    plt.xlabel('Number of join trees', weight='bold')
    plt.ylabel('Enumeration time (ms)', weight='bold')
    # plt.legend(framealpha=0.85, labelspacing=0, prop=bold_font)

    if max_time >= 3600e3 - 1e-5:
        plt.axhline(y=3600e3, color='dimgray', linestyle=':')
        plt.text(y=3600e3, x=min_num_jts, s='Timeout (1 hour)', color='dimgray', va='top', ha='left', fontproperties=label_font)

    # Show the plot
    plt.tight_layout(pad=0)
    plt.savefig(os.path.join(figures_path, f'enum-time-vs-num-jts-{benchmark}.pdf'), format='pdf')
    plt.show()
