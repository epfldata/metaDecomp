import pandas as pd
import matplotlib.pyplot as plt
from matplotlib import font_manager as fm
import numpy as np
import os
import matplotlib.lines as mlines

from config import results_path, figures_path, colors

plt.rcParams['font.size'] = 24

legend_font = fm.FontProperties(size=18, weight='semibold')
label_font = fm.FontProperties(size=22)

save_path = os.path.join(figures_path, 'scatter-all-benchmarks')

if not os.path.exists(save_path):
    os.makedirs(save_path)

duckdb_df = {}
dpconv_df = {}
meta_df = {}
base_df = {}
duckdb = {}
dpconv = {}
meta = {}
base = {}

for (baseline, baseline_label, subfigure) in [('dpconv', 'DPconv', 'a'), ('duckdb', 'DuckDB', 'b'), ('uniondp', 'UnionDP', 'c'), ('yanplus', 'Yannakakis$^+$', 'd'), ('learned-rewrite', 'LearnedRewrite', 'e'), ('llm-r2', 'LLM-R$^2$', 'f')]:
    for benchmark in ['dsb', 'job-original', 'musicbrainz', 'job-large']:

        # Load the CSV files
        duckdb_df[benchmark] = pd.read_csv(os.path.join(results_path, f'duckdb-{benchmark}.csv'))
        dpconv_df[benchmark] = pd.read_csv(os.path.join(results_path, f'dpconv-opt-{benchmark}.csv'))
        meta_df[benchmark] = pd.read_csv(os.path.join(results_path, f'metadecomp-opt-{benchmark}.csv'))
        base_df[benchmark] = pd.read_csv(os.path.join(results_path, f'{baseline}{f"" if baseline == "duckdb" else "-opt"}-{benchmark}.csv'))

        diff = np.abs(base_df[benchmark]['total_time'] - meta_df[benchmark]['total_time']) / base_df[benchmark]['total_time']
        duckdb_df[benchmark] = duckdb_df[benchmark][diff > 0.1]
        dpconv_df[benchmark] = dpconv_df[benchmark][diff > 0.1]
        meta_df[benchmark] = meta_df[benchmark][diff > 0.1]
        base_df[benchmark] = base_df[benchmark][diff > 0.1]

        # Cap the values at 3e8
        meta[benchmark] = np.clip(meta_df[benchmark]['total_time'], None, 3e8)
        base[benchmark] = np.clip(base_df[benchmark]['total_time'], None, 3e8)
        duckdb[benchmark] = np.clip(duckdb_df[benchmark]['total_time'], None, 3e8)
        dpconv[benchmark] = np.clip(dpconv_df[benchmark]['total_time'], None, 3e8)

        if len(meta[benchmark]) != len(base[benchmark]):
            print(f"Warning: Different sizes for {benchmark}: meta {len(meta[benchmark])}, baseline {len(base[benchmark])}")

        size = min(len(meta[benchmark]), len(base[benchmark]))
        meta[benchmark] = meta[benchmark][:size]
        base[benchmark] = base[benchmark][:size]
        duckdb[benchmark] = duckdb[benchmark][:size]
        dpconv[benchmark] = dpconv[benchmark][:size]

        # Drop entries where both are timeout
        both_timeout = (meta[benchmark] >= 3e8 - 1e-5) & (duckdb[benchmark] >= 3e8 - 1e-5) & (dpconv[benchmark] >= 3e8 - 1e-5)
        meta[benchmark] = meta[benchmark][~both_timeout]
        base[benchmark] = base[benchmark][~both_timeout]
        duckdb[benchmark] = duckdb[benchmark][~both_timeout]
        dpconv[benchmark] = dpconv[benchmark][~both_timeout]

        meta[benchmark] = meta[benchmark] / 1000
        base[benchmark] = base[benchmark] / 1000
        duckdb[benchmark] = duckdb[benchmark] / 1000
        dpconv[benchmark] = dpconv[benchmark] / 1000

    # Create the scatter plot
    plt.figure(figsize=(5, 5))

    plt.scatter(base['dsb'], meta['dsb'], label = 'DSB', alpha = 0.70, s = 90, marker='+', color = colors[0])
    plt.scatter(base['job-original'], meta['job-original'], label = 'JOB', alpha = 0.70, s = 70, marker='x', color = colors[1])
    plt.scatter(base['musicbrainz'], meta['musicbrainz'], label = 'Musicbrainz', alpha = 0.70, s = 20, marker='o', color = colors[2])
    plt.scatter(base['job-large'], meta['job-large'], label = 'JOBLarge', alpha = 0.70, s = 70, marker='^', color = colors[3])

    # if baseline == 'dpconv':
    #     plt.legend(framealpha=0.85, labelspacing=0.3, prop=legend_font)

    # Add the reference line y = x
    base_min, base_max = base[min(base, key=lambda x: base[x].min())].min(), base[max(base, key=lambda x: base[x].max())].max()
    meta_min, meta_max = meta[min(meta, key=lambda x: meta[x].min())].min(), meta[max(meta, key=lambda x: meta[x].max())].max()
    plt.plot([min(base_min, meta_min), max(base_max, meta_max)], [min(base_min, meta_min), max(base_max, meta_max)], linestyle=':', color='darkorange')
    

    # Set log scale for both axes
    plt.xscale('log')
    plt.yscale('log')

    # Add labels, legend, and title
    plt.xlabel(f'({subfigure}) {baseline_label} (ms)', weight='bold')
    plt.ylabel('metaDecomp (ms)', weight='bold')
    # plt.legend(framealpha=0.85, labelspacing=0, prop=bold_font)

    meta_max = meta[max(meta, key = lambda x: meta[x].max())].max()

    if meta_max >= 3e5:
        plt.axhline(y=3e5, color='dimgray', linestyle=':')
        plt.text(y=3e5, x=10**((np.log10(base_min) + np.log10(base_max)) / 2), s='metaDecomp timeout (5 min)', color='dimgray', va='top', ha='center', fontproperties=label_font)
    if base_max >= 3e5:
        plt.axvline(x=3e5, color='dimgray', linestyle=':')
        plt.text(x=3e5, y=min(meta_min, base_min), s=f'{"Learned-\nRewrite" if baseline == "learned-rewrite" else baseline_label}\ntimeout\n(5 min)', color='dimgray', va='bottom', ha='right', rotation=0, fontproperties=label_font)

    # Show the plot
    plt.tight_layout(pad=0)
    plt.savefig(os.path.join(save_path, f'scatter-meta-{baseline}.pdf'), format='pdf')
    # plt.show()
    plt.close()

# Save the legends

handles = [
    mlines.Line2D([], [], color=colors[0], marker='+', linestyle='None', markersize=12, label='DSB'),
    mlines.Line2D([], [], color=colors[1], marker='x', linestyle='None', markersize=10, label='JOB'),
    mlines.Line2D([], [], color=colors[2], marker='o', linestyle='None', markersize=8, label='Musicbrainz'),
    mlines.Line2D([], [], color=colors[3], marker='^', linestyle='None', markersize=10, label='JOBLarge')
]
legends = ['DSB', 'JOB', 'Musicbrainz', 'JOBLarge']

fig_legend = plt.figure(figsize=(12, 1))
ax = fig_legend.add_subplot(111)
ax.axis('off')

legend_font = fm.FontProperties(size=24, weight='semibold')
legend = ax.legend(handles, legends, loc='center', ncol=len(legends), frameon=True, prop=legend_font, 
                   handletextpad=0.2, columnspacing=2.0, borderpad=0.4)
legend.get_frame().set_edgecolor('gray')
legend.get_frame().set_linewidth(1)
legend_path = os.path.join(save_path, 'scatter-legend.pdf')
fig_legend.canvas.draw()

from matplotlib.transforms import Bbox
bbox = legend.get_window_extent().transformed(fig_legend.dpi_scale_trans.inverted())
bbox_expanded = Bbox.from_extents(bbox.x0 - 0.05, bbox.y0 - 0.05, bbox.x1 + 0.05, bbox.y1 + 0.05)
fig_legend.savefig(legend_path, format='pdf', bbox_inches=bbox_expanded, pad_inches=0)
print(f"Saved legend to {legend_path}")
plt.close(fig_legend)
