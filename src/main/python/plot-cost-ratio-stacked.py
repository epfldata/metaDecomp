from duckdb import df
from matplotlib.ticker import PercentFormatter
from matplotlib import font_manager as fm
import pandas as pd
import matplotlib.pyplot as plt
import numpy as np
import os

from config import results_path, figures_path, colors

plt.rcParams['font.size'] = 24

bold_font = fm.FontProperties(size=24, weight='semibold')

metadecomp_path = {}
dpconv_path = {}
meta_df = {}
dpconv_df = {}
duckdb_df = {}
merged = {}
ratios = {}

benchmarks = ['dsb', 'job-original', 'musicbrainz', 'job-large']

for benchmark in benchmarks:

    # Paths to your CSV files
    metadecomp_path[benchmark] = os.path.join(results_path, f'metadecomp-opt-{benchmark}.csv')
    dpconv_path[benchmark] = os.path.join(results_path, f'dpconv-opt-{benchmark}.csv')

    # Read the CSV files
    meta_df[benchmark] = pd.read_csv(metadecomp_path[benchmark])
    dpconv_df[benchmark] = pd.read_csv(dpconv_path[benchmark])

    # Load the CSV files
    duckdb_df[benchmark] = pd.read_csv(os.path.join(results_path, f'duckdb-{benchmark}.csv'))

    # Merge on 'query'
    merged[benchmark] = pd.merge(meta_df[benchmark], dpconv_df[benchmark], on='query', suffixes=('_meta', '_dpconv'))

    merged[benchmark] = merged[benchmark][merged[benchmark]['opt_exec_time'] > 0]

    # Compute the ratio
    ratios[benchmark] = (merged[benchmark]['cout_opt_intm'] + merged[benchmark]['cost_in']) / merged[benchmark]['cout_cost']

    # Cap the data at 0.99 - so that the ones with 1.0 are shown in the range 0.9-1.0
    ratios[benchmark] = np.clip(ratios[benchmark], None, 0.99)

# Create the histogram: compute per-series normalized histograms and plot grouped bars
# plt.figure(figsize=(9, 2))

series_keys = benchmarks
legends = ['DSB', 'JOB', 'Musicbrainz', 'JOBLarge']
series = [ratios[k] for k in series_keys]
hatches = ['..', '--', 'xx', '||']

# bin edges and centers
bins = np.arange(0.0, 1.1, 0.05)
bin_centers = (bins[:-1] + bins[1:]) / 2.0
bin_width = bins[1] - bins[0]

# compute raw counts per bin for each series
counts_list = []
for s in series:
    if len(s) == 0:
        counts = np.zeros(len(bin_centers), dtype=int)
    else:
        counts, _ = np.histogram(s, bins=bins)
    counts_list.append(counts)

# normalize to fractions of the total (so stacked bars sum to 1 across all bins & series)
total_all = sum(counts_list).sum()
if total_all > 0:
    stacked_props = [counts / total_all for counts in counts_list]
else:
    stacked_props = [np.zeros(len(bin_centers)) for _ in counts_list]

# plot stacked bars (one stacked bar per bin)
plt.figure(figsize=(10, 2))
width = bin_width  # visual width of the stacked bar
bottom = np.zeros(len(bin_centers))
for i, (props, hatch) in enumerate(zip(stacked_props, hatches)):
    plt.bar(bin_centers, props, bottom=bottom, width=width, align='center',
            label=legends[i], color=colors[i], hatch=hatch, edgecolor='black', linewidth=0.4)
    bottom += props

ax = plt.gca()
ax.yaxis.set_major_formatter(PercentFormatter(1, 0))
# ax.set_xticks(np.arange(0.0, 1.1, 0.1))
# ax.set_xlim(bins[0], bins[-1])

# Add labels and title
# plt.xlabel('(a) Cost ratio', weight='bold')
plt.ylabel('Frequency', weight='bold')
legend_font = fm.FontProperties(size=20, weight='semibold')
plt.legend(framealpha=0.5, labelspacing=0.1, prop=legend_font)

# Show the plot
plt.tight_layout(pad=0)
plt.savefig(os.path.join(figures_path, f'cost-ratios-stacked.pdf'), format='pdf')
plt.show()
