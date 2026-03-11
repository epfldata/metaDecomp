import pandas as pd
import matplotlib.pyplot as plt
from matplotlib import font_manager as fm
import os
import numpy as np
import matplotlib.patches as mpatches

from config import results_path, figures_path, colors, dark_orange

plt.rcParams['font.size'] = 19
legend_font = fm.FontProperties(size=19, weight='semibold')
label_font = fm.FontProperties(size=19)

bold_font = fm.FontProperties(size=12, weight='semibold')

save_path = os.path.join(figures_path, f'enum-time')

if not os.path.exists(save_path):
    os.makedirs(save_path)

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
    plt.figure(figsize=(3.5, 3.5))

    plt.scatter(meta['num_join_trees'], meta['time_ms'], label = 'metaDecomp', alpha = 0.70, s = 40, marker='o', color = colors[0])
    plt.scatter(ssp['num_join_trees'], ssp['time_ms'], label = 'GYO reduction', alpha = 0.70, s = 60, marker='^', color = colors[1])

    # plt.legend(framealpha=0.85, labelspacing=0.3, prop=legend_font)

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
    plt.savefig(os.path.join(save_path, f'enum-time-{benchmark}.pdf'), format='pdf')
    # plt.show()

# Save the legends
import matplotlib.lines as mlines

handles = [
    mlines.Line2D([], [], color=colors[0], marker='o', linestyle='None', markersize=12, label='metaDecomp'),
    mlines.Line2D([], [], color=colors[1], marker='^', linestyle='None', markersize=10, label='GYO reduction')
]
legends = ['metaDecomp', 'GYO reduction']

fig_legend = plt.figure(figsize=(12, 1))
ax = fig_legend.add_subplot(111)
ax.axis('off')

legend_font = fm.FontProperties(size=24, weight='semibold')
legend = ax.legend(handles, legends, loc='center', ncol=len(legends), frameon=True, prop=legend_font, 
                   handletextpad=0.2, columnspacing=2.0, borderpad=0.4)
legend.get_frame().set_edgecolor('gray')
legend.get_frame().set_linewidth(1)
legend_path = os.path.join(save_path, 'enum-time-legend.pdf')
fig_legend.canvas.draw()

from matplotlib.transforms import Bbox
bbox = legend.get_window_extent().transformed(fig_legend.dpi_scale_trans.inverted())
bbox_expanded = Bbox.from_extents(bbox.x0 - 0.05, bbox.y0 - 0.05, bbox.x1 + 0.05, bbox.y1 + 0.05)
fig_legend.savefig(legend_path, format='pdf', bbox_inches=bbox_expanded, pad_inches=0)
print(f"Saved legend to {legend_path}")
plt.close(fig_legend)
