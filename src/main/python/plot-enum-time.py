import pandas as pd
import matplotlib.pyplot as plt
from matplotlib import font_manager as fm
import os
import numpy as np
import matplotlib.patches as mpatches

from config import results_path, figures_path, colors, dark_orange

plt.rcParams['font.size'] = 16

bold_font = fm.FontProperties(size=12, weight='semibold')

# Load the CSV file
meta_dsb = pd.read_csv(os.path.join(results_path, 'metadecomp-enum-dsb.csv'))
meta_original = pd.read_csv(os.path.join(results_path, 'metadecomp-enum-job-original.csv'))
meta_musicbrainz = pd.read_csv(os.path.join(results_path, 'metadecomp-enum-musicbrainz.csv'))
meta_large = pd.read_csv(os.path.join(results_path, 'metadecomp-enum-job-large.csv'))

meta_all = pd.concat([meta_dsb, meta_original, meta_musicbrainz, meta_large])

# Ensure num_rels is numeric, dropping non-numeric values
meta_all['num_rels'] = pd.to_numeric(meta_all['num_rels'], errors='coerce')
meta_all = meta_all.dropna(subset=['num_rels'])
print(meta_all['num_rels'])
meta_all['num_rels'] = meta_all['num_rels'].astype(int)

# Load the CSV file
ssp_dsb = pd.read_csv(os.path.join(results_path, 'sparksqlplus-enum-dsb.csv'))
ssp_original = pd.read_csv(os.path.join(results_path, 'sparksqlplus-enum-job-original.csv'))
ssp_musicbrainz = pd.read_csv(os.path.join(results_path, 'sparksqlplus-enum-musicbrainz.csv'))
ssp_large = pd.read_csv(os.path.join(results_path, 'sparksqlplus-enum-job-large.csv'))

ssp_all = pd.concat([ssp_original, ssp_dsb, ssp_musicbrainz, ssp_large])

# Filter meta_data to keep only rows with 'query' values that exist in ssp_data
# meta_data = meta_data[meta_data['query'].isin(ssp_data['query'])]

# Create a mapping from query to num_rels in meta_data
query_to_num_rels = meta_all.set_index('query')['num_rels'].to_dict()

# Map the num_rels to ssp_data using the query attribute
ssp_all['num_rels'] = ssp_all['query'].map(query_to_num_rels)

# Drop the 'query' column if it exists
meta_all = meta_all.drop(columns=['query'], errors='ignore')

# Group data by num_rels and calculate median and IQR
meta_all['time_ms'] = meta_all['time_us'] / 1000  # Convert to ms
meta_grouped = meta_all.groupby('num_rels')
meta_num_rels = meta_grouped.median().index
meta_median = meta_grouped['time_ms'].median()
meta_q25 = meta_grouped['time_ms'].quantile(0.25)
meta_q75 = meta_grouped['time_ms'].quantile(0.75)

# Asymmetric error bars around the median (clip to avoid negative extents)
meta_err_lower = (meta_median - meta_q25).clip(lower=0)
meta_err_upper = (meta_q75 - meta_median).clip(lower=0)
meta_error = [meta_err_lower, meta_err_upper]



# Drop the 'query' column if it exists
ssp_all = ssp_all.drop(columns=['query'], errors='ignore')

# Ensure num_rels is numeric, dropping non-numeric values
ssp_all['num_rels'] = pd.to_numeric(ssp_all['num_rels'], errors='coerce')
ssp_all = ssp_all.dropna(subset=['num_rels'])

# Group data by num_rels and calculate median and IQR for ssp
ssp_grouped = ssp_all.groupby('num_rels')
ssp_num_rels = ssp_grouped.median().index
ssp_median = ssp_grouped['time_ms'].median()
ssp_q25 = ssp_grouped['time_ms'].quantile(0.25)
ssp_q75 = ssp_grouped['time_ms'].quantile(0.75)

# Asymmetric error bars around the median (clip to avoid negative extents)
ssp_err_lower = (ssp_median - ssp_q25).clip(lower=0)
ssp_err_upper = (ssp_q75 - ssp_median).clip(lower=0)
ssp_error = [ssp_err_lower, ssp_err_upper]


# Create the plot -> make boxplots per num_rels for meta and ssp

# build union of groups (sorted)
meta_groups = sorted(meta_all['num_rels'].unique())
ssp_groups = sorted(ssp_all['num_rels'].unique())
groups_included = sorted(set(meta_groups).union(set(ssp_groups)))

# collect values per group (in ms)
meta_values = []
ssp_values = []
labels = []
for g in groups_included:
    mv = meta_all.loc[meta_all['num_rels'] == g, 'time_ms'].dropna().tolist()
    sv = ssp_all.loc[ssp_all['num_rels'] == g, 'time_ms'].dropna().tolist()
    # include group if at least one series has data
    if len(mv) == 0 and len(sv) == 0:
        continue
    meta_values.append(mv)
    ssp_values.append(sv)
    labels.append(str(int(g)))

if len(labels) == 0:
    raise SystemExit("No data to plot")

plt.figure(figsize=(6, 2.25))

n = len(labels)
positions = np.arange(n)
width = 0.6
pos_meta = meta_groups # - width / 2.0
pos_ssp = ssp_groups # + width / 2.0

# draw boxplots at offset positions
bp_meta = plt.boxplot(meta_values, positions=groups_included, widths=width,
                      patch_artist=True, showfliers=False, manage_ticks=False)
bp_ssp = plt.boxplot(ssp_values, positions=groups_included, widths=width,
                     patch_artist=True, showfliers=False, manage_ticks=False)

# style boxes and related artists using shared palette from config.colors
col_meta = colors[0]
col_ssp = colors[1]
for box in bp_meta['boxes']:
    box.set(facecolor=col_meta, edgecolor='C0', alpha=0.8)
for whisker in bp_meta.get('whiskers', []):
    whisker.set(color='C0', linewidth=1.0)
for cap in bp_meta.get('caps', []):
    cap.set(color='C0', linewidth=1.0)
for median in bp_meta.get('medians', []):
    median.set(color='C0', linewidth=1.2)
for flier in bp_meta.get('fliers', []):
    flier.set(markeredgecolor=col_meta)

for box in bp_ssp['boxes']:
    box.set(facecolor=col_ssp, edgecolor=dark_orange, alpha=0.8, hatch='ooo')
for whisker in bp_ssp.get('whiskers', []):
    whisker.set(color=dark_orange, linewidth=1.0)
for cap in bp_ssp.get('caps', []):
    cap.set(color=dark_orange, linewidth=1.0)
for median in bp_ssp.get('medians', []):
    median.set(color=dark_orange, linewidth=1.2)
for flier in bp_ssp.get('fliers', []):
    flier.set(markeredgecolor=col_ssp)

# log scale for y axis
plt.yscale('log')

# labels, legend, timeout line
plt.xlabel('Number of relations', weight='bold')
plt.ylabel('Enumeration\ntime (ms)', weight='bold')
p_meta = mpatches.Patch(facecolor=col_meta, edgecolor='C0', label='metaDecomp', alpha=0.8)
p_ssp = mpatches.Patch(facecolor=col_ssp, edgecolor=dark_orange, label='GYO reduction', alpha=0.8, hatch='oooo')
plt.legend(handles=[p_meta, p_ssp], framealpha=0.85, labelspacing=0.3, prop=bold_font)

plt.axhline(y=3600e3, color='gray', linestyle=':')
plt.text(x=(groups_included[0] + groups_included[-1])/2, y=3600e3, s='Timeout (1 hour)', color='gray', va='top', ha='center')

plt.tight_layout(pad=0)
plt.savefig(os.path.join(figures_path, 'enum-time.pdf'), format='pdf')
plt.show()
