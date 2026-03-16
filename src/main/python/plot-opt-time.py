from matplotlib import ticker
import pandas as pd
import matplotlib.pyplot as plt
from matplotlib import font_manager as fm
from matplotlib.ticker import MaxNLocator
import os
import numpy as np
from matplotlib.offsetbox import HPacker, VPacker, AnnotationBbox, DrawingArea, TextArea

from config import colors, dark_orange, results_path, figures_path, benchmarks

plt.rcParams['font.size'] = 18

bold_font = fm.FontProperties(size=12, weight='semibold')
legend_font = fm.FontProperties(size=18, weight='semibold')

save_path = os.path.join(figures_path, 'opt-time')

if not os.path.exists(save_path):
    os.makedirs(save_path)

# Define methods and their styles
methods = {
    'metaDecomp': {'file': 'metadecomp-opt-{}.csv', 'col': 'total_opt_time', 'color': 'black', 'marker': 'o', 'label': 'metaDecomp'},
    'DPconv': {'file': 'dpconv-opt-{}.csv', 'col': 'opt_time', 'color': colors[0], 'marker': 's', 'label': 'DPconv'},
    'DuckDB': {'file': 'duckdb-{}.csv', 'col': 'opt_time', 'color': colors[1], 'marker': '^', 'label': 'DuckDB'},
    'UnionDP': {'file': 'uniondp-opt-{}.csv', 'col': 'opt_time', 'color': colors[2], 'marker': 'd', 'label': 'UnionDP'},
    'Yannakakis+': {'file': 'yanplus-opt-{}.csv', 'col': 'opt_time', 'color': colors[3], 'marker': 'P', 'label': 'Yannakakis$^+$'},
    'LLM-R2': {'file': 'llm-r2-opt-{}.csv', 'col': 'opt_time', 'color': colors[4], 'marker': 'v', 'label': 'LLM-R$^2$'},
    'LearnedRewrite': {'file': 'learned-rewrite-opt-{}.csv', 'col': 'opt_time', 'color': colors[5], 'marker': 'x', 'label': 'LearnedRewrite'}
}

# Benchmarks to plot
benchmarks_to_plot = benchmarks # ['dsb', 'job-original', 'musicbrainz', 'job-large']

# Container for overall data
overall_data = {method: [] for method in methods}

# Function to plot a single benchmark
def plot_benchmark(series, df_map, output_name):
    plt.figure(figsize=(7, 4))
    
    # Iterate through methods to plot
    for method_name, config in methods.items():
        if method_name not in df_map or df_map[method_name] is None:
            continue
            
        df = df_map[method_name]
        
        # Group by num_rels
        grouped = df.groupby('num_rels')['time_ms'].agg(['mean', 'min', 'max']).sort_index()
        
        # Determine x offset based on method index to avoid overlap
        idx = list(methods.keys()).index(method_name)
        offset = (idx - 2) * 0.1 # Center around 0
        
        x = grouped.index + offset
        y = grouped['mean']
        yerr = [grouped['mean'] - grouped['min'], grouped['max'] - grouped['mean']]
        
        # Plot with connecting lines
        plt.errorbar(x, y, yerr=yerr, fmt=config['marker'], label=config['label'], 
                     color=config['color'], capsize=3, alpha=0.8, markersize=8, linestyle=':')

    plt.yscale('log')
    plt.xlabel('Number of relations', weight='bold')
    plt.ylabel('Optimization\ntime (ms)', weight='bold')
    # Use integer ticks for x-axis
    plt.gca().xaxis.set_major_locator(MaxNLocator(integer=True))
    
    # Add timeout line if needed
    max_times = [df['time_ms'].max() for df in df_map.values() if df is not None]
    if not max_times:
        return
    overall_max = max(max_times)
    
    # Hide the 10^5 tick on the y-axis if there is no point greater than 10^5
    if overall_max < 1e5:
        ax = plt.gca()
        locs = ax.get_yticks()
        ax.set_yticks([loc for loc in locs if loc < 1e5])

    if overall_max >= 3e5:
        plt.axhline(y=3e5, color='dimgray', linestyle=':')
        # if series == 'overall':
        plt.text(x=df['num_rels'].min(), y=3e5, s='Timeout (5 min)', color='dimgray', va='top', ha='left')
        # else:
            # plt.text(x=groups_included[0], y=3e5, s='Timeout (5 min)', color='dimgray', va='top', ha='left')

    plt.tight_layout()
    output_file = os.path.join(save_path, output_name)
    # plt.tight_layout(pad=0) # Left, bottom, right, top
    plt.savefig(output_file, format='pdf', bbox_inches='tight', pad_inches=0.01)
    print(f"  Saved plot to {output_file}")
    plt.close()

# Process each benchmark
for series in benchmarks_to_plot:
    print(f"Processing benchmark: {series}")
    
    # Pre-load all dataframes for this benchmark
    df_map = {}
    
    # Load metaDecomp data first as reference for num_rels logic
    meta_path = os.path.join(results_path, methods['metaDecomp']['file'].format(series))
    if not os.path.exists(meta_path):
        print(f"Skipping {series}: metaDecomp file not found at {meta_path}")
        continue
    
    meta_df_raw = pd.read_csv(meta_path)
    if 'query' in meta_df_raw.columns:
        meta_df_raw['query'] = meta_df_raw['query'].astype(str)
    query_to_num_rels = meta_df_raw.set_index('query')['num_rels'].to_dict()
    
    for method_name, config in methods.items():
        file_path = os.path.join(results_path, config['file'].format(series))
        if not os.path.exists(file_path):
            df_map[method_name] = None
            continue
            
        df = pd.read_csv(file_path)
        if 'query' not in df.columns or config['col'] not in df.columns:
            df_map[method_name] = None
            continue
            
        df['query'] = df['query'].astype(str)
        if 'num_rels' not in df.columns:
            df['num_rels'] = df['query'].map(query_to_num_rels)
            
        df = df.dropna(subset=['num_rels'])
        df['num_rels'] = pd.to_numeric(df['num_rels'])
        
        df[config['col']] = pd.to_numeric(df[config['col']], errors='coerce')
        df = df.dropna(subset=[config['col']])
        df['time_ms'] = df[config['col']] / 1000.0
        df['time_ms'] = df['time_ms'].clip(upper=300000)
        
        df_map[method_name] = df
        overall_data[method_name].append(df[['num_rels', 'time_ms']])
        
    plot_benchmark(series, df_map, f'opt-time-{series}.pdf')

# Process Overall
print("Processing benchmark: overall")
overall_df_map = {}
for method_name in methods:
    if overall_data[method_name]:
        overall_df_map[method_name] = pd.concat(overall_data[method_name], ignore_index=True)
    else:
        overall_df_map[method_name] = None

plot_benchmark('overall', overall_df_map, 'opt-time-overall.pdf')

# Save Legend separately
print("Saving separate legend versions...")
handles = []
labels = []
# Create dummy handles
for method_name, config in methods.items():
    h = plt.Line2D([0], [0], color=config['color'], marker=config['marker'], linestyle=':', label=config['label'], markersize=12)
    handles.append(h)
    labels.append(config['label'])

# Version 1: Single row
fig_legend = plt.figure(figsize=(12, 1))
ax = fig_legend.add_subplot(111)
ax.axis('off')
legend = ax.legend(handles, labels, loc='center', ncol=len(methods), frameon=True, prop=legend_font, 
                   handletextpad=0.2, columnspacing=0.8, borderpad=0.3)
legend.get_frame().set_edgecolor('gray')
legend.get_frame().set_linewidth(1)
legend_path = os.path.join(save_path, 'opt-time-legend.pdf')
fig_legend.canvas.draw()
bbox = legend.get_window_extent().transformed(fig_legend.dpi_scale_trans.inverted())
# Manual padding of 0.05 inches to ensure border is visible but crop is tight
from matplotlib.transforms import Bbox
bbox_expanded = Bbox.from_extents(bbox.x0 - 0.05, bbox.y0 - 0.05, bbox.x1 + 0.05, bbox.y1 + 0.05)
fig_legend.savefig(legend_path, format='pdf', bbox_inches=bbox_expanded, pad_inches=0)
print(f"  Saved legend (1 row) to {legend_path}")
plt.close(fig_legend)
