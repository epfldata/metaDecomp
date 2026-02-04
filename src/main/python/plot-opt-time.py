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

# Define methods and their styles
methods = {
    'metaDecomp': {'file': 'metadecomp-opt-{}.csv', 'col': 'total_opt_time', 'color': 'black', 'marker': 'o', 'label': 'metaDecomp'},
    'DPconv': {'file': 'dpconv-opt-{}.csv', 'col': 'opt_time', 'color': colors[0], 'marker': 's', 'label': 'DPconv'},
    'DuckDB': {'file': 'duckdb-{}.csv', 'col': 'opt_time', 'color': colors[1], 'marker': '^', 'label': 'DuckDB'},
    'UnionDP': {'file': 'uniondp-opt-{}.csv', 'col': 'opt_time', 'color': colors[2], 'marker': 'd', 'label': 'UnionDP'},
    'Yannakakis+': {'file': 'yanplus-opt-{}.csv', 'col': 'opt_time', 'color': colors[3], 'marker': 'P', 'label': 'Yannakakis$^+$'},
    'LLM-R2': {'file': 'llm-r2-opt-{}.csv', 'col': 'opt_time', 'color': colors[4], 'marker': 'v', 'label': 'LLM-R$^2$'},
    'LearnedRewrite': {'file': 'learned-rewrite-opt-{}.csv', 'col': 'opt_time', 'color': colors[5], 'marker': 'x', 'label': 'Learned Rewrite'}
}

# Benchmarks to plot
benchmarks_to_plot = benchmarks # ['dsb', 'job-original', 'musicbrainz', 'job-large']

# Container for overall data
overall_data = {method: [] for method in methods}

# Function to plot a single benchmark
def plot_benchmark(series, df_map, output_name):
    plt.figure(figsize=(7.5, 3.5))
    
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
    plt.ylabel('Optimization time (ms)', weight='bold')
    # Use integer ticks for x-axis
    plt.gca().xaxis.set_major_locator(MaxNLocator(integer=True))
    
    # Add timeout line if needed
    max_times = [df['time_ms'].max() for df in df_map.values() if df is not None]
    if not max_times:
        return
    overall_max = max(max_times)
    if overall_max >= 3e5:
        plt.axhline(y=3e5, color='dimgray', linestyle=':')
        # if series == 'overall':
        plt.text(x=df['num_rels'].min(), y=3e5, s='Timeout (5 min)', color='dimgray', va='top', ha='left')
        # else:
            # plt.text(x=groups_included[0], y=3e5, s='Timeout (5 min)', color='dimgray', va='top', ha='left')

    plt.tight_layout()
    output_file = os.path.join(figures_path, output_name)
    plt.tight_layout(pad=0)
    plt.savefig(output_file, format='pdf')
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
legend_path = os.path.join(figures_path, 'opt-time-legend.pdf')
fig_legend.canvas.draw()
bbox = legend.get_window_extent().transformed(fig_legend.dpi_scale_trans.inverted())
# Manual padding of 0.05 inches to ensure border is visible but crop is tight
from matplotlib.transforms import Bbox
bbox_expanded = Bbox.from_extents(bbox.x0 - 0.05, bbox.y0 - 0.05, bbox.x1 + 0.05, bbox.y1 + 0.05)
fig_legend.savefig(legend_path, format='pdf', bbox_inches=bbox_expanded, pad_inches=0)
print(f"  Saved legend (1 row) to {legend_path}")
plt.close(fig_legend)

# Version 2: Two rows (5+2 centered)
fig_legend_2 = plt.figure(figsize=(8, 2))
ax2 = fig_legend_2.add_subplot(111)
ax2.axis('off')

def create_row_packer(h_list, l_list):
    row_items = []
    for h, l in zip(h_list, l_list):
        # Slightly reduced height for tighter vertical fit
        da = DrawingArea(32, 24, 0, 0)
        # Draw line without markers
        leg_line = plt.Line2D([2, 30], [12, 12], 
                              color=h.get_color(), 
                              linestyle=h.get_linestyle(), 
                              linewidth=h.get_linewidth(),
                              alpha=0.8)
        da.add_artist(leg_line)
        # Draw single marker in the exact center of the line
        leg_marker = plt.Line2D([16], [12],
                                color=h.get_color(),
                                marker=h.get_marker(),
                                markersize=h.get_markersize(),
                                linestyle='None', # Explicitly no line for the marker artist
                                alpha=1.0)
        da.add_artist(leg_marker)
        te = TextArea(l, textprops=dict(fontproperties=legend_font))
        # Group handle and label
        item = HPacker(children=[da, te], align="center", pad=0, sep=4)
        row_items.append(item)
    return HPacker(children=row_items, align="center", pad=0, sep=16)

row1 = create_row_packer(handles[:5], labels[:5])
row2 = create_row_packer(handles[5:], labels[5:])
# Reduced sep between rows
vbox = VPacker(children=[row1, row2], align="center", pad=0, sep=2)

ab = AnnotationBbox(vbox, (0.5, 0.5), xycoords='axes fraction',
                    box_alignment=(0.5, 0.5),
                    pad=0.1, # Minimized padding
                    # Reduced boxstyle pad to 0.1 for very tight fit
                    bboxprops=dict(boxstyle="round,pad=0.1", edgecolor='gray', linewidth=1, facecolor='white'))
ax2.add_artist(ab)

legend_path_2 = os.path.join(figures_path, 'opt-time-legend-2-rows.pdf')
fig_legend_2.canvas.draw()
bbox2 = ab.get_window_extent().transformed(fig_legend_2.dpi_scale_trans.inverted())
# Manual padding of 0.05 inches
bbox_expanded_2 = Bbox.from_extents(bbox2.x0 - 0.05, bbox2.y0 - 0.05, bbox2.x1 + 0.05, bbox2.y1 + 0.05)
fig_legend_2.savefig(legend_path_2, format='pdf', bbox_inches=bbox_expanded_2, pad_inches=0)
print(f"  Saved legend (2 rows centered) to {legend_path_2}")
plt.close(fig_legend_2)
