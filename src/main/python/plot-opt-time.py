from matplotlib import ticker
import pandas as pd
import matplotlib.pyplot as plt
from matplotlib import font_manager as fm
from matplotlib.ticker import MaxNLocator
import os

from config import colors, dark_orange, results_path, figures_path, benchmarks

plt.rcParams['font.size'] = 14

bold_font = fm.FontProperties(size=12, weight='semibold')
legend_font = fm.FontProperties(size=14, weight='semibold')

for series in benchmarks + ['overall']:
    # Load the CSV files
    if series == 'overall':
        for s in benchmarks:
            meta_df_part = pd.read_csv(os.path.join(results_path, f'metadecomp-opt-{s}.csv'))
            dpconv_df_part = pd.read_csv(os.path.join(results_path, f'dpconv-opt-{s}.csv'))
            if 'meta_df' in locals():
                meta_df = pd.concat([meta_df, meta_df_part], ignore_index=True)
                dpconv_df = pd.concat([dpconv_df, dpconv_df_part], ignore_index=True)
            else:
                meta_df = meta_df_part
                dpconv_df = dpconv_df_part
    else:
        meta_df = pd.read_csv(os.path.join(results_path, f'metadecomp-opt-{series}.csv'))
        dpconv_df = pd.read_csv(os.path.join(results_path, f'dpconv-opt-{series}.csv'))

    meta_df['num_rels'] = pd.to_numeric(meta_df['num_rels'], errors='coerce')
    dpconv_df['num_rels'] = pd.to_numeric(dpconv_df['num_rels'], errors='coerce')

    meta_df = meta_df[['num_rels', 'total_opt_time']]
    dpconv_df = dpconv_df[['num_rels', 'opt_time']]

    # Group data by num_rels and calculate median
    meta_grouped = meta_df.groupby('num_rels')
    num_rels = meta_grouped.median().index
    meta_median = meta_grouped['total_opt_time'].median() / 1000

    dpconv_grouped = dpconv_df.groupby('num_rels')
    dpconv_median = dpconv_grouped['opt_time'].median() / 1000

    # Group data by num_rels and calculate mean, min, and max
    meta_min = meta_grouped['total_opt_time'].min() / 1000
    meta_max = meta_grouped['total_opt_time'].max() / 1000
    dpconv_min = dpconv_grouped['opt_time'].min() / 1000
    dpconv_max = dpconv_grouped['opt_time'].max() / 1000


    # Compute 25th and 75th percentiles (Q1/Q3) instead of min/max
    meta_q25 = meta_grouped['total_opt_time'].quantile(0.25) / 1000
    meta_q75 = meta_grouped['total_opt_time'].quantile(0.75) / 1000
    dpconv_q25 = dpconv_grouped['opt_time'].quantile(0.25) / 1000
    dpconv_q75 = dpconv_grouped['opt_time'].quantile(0.75) / 1000

    # Asymmetric error bars around the median (25th/75th percentiles)
    meta_err_lower = (meta_median - meta_q25)
    meta_err_upper = (meta_q75 - meta_median)
    dpconv_err_lower = (dpconv_median - dpconv_q25)
    dpconv_err_upper = (dpconv_q75 - dpconv_median)
    meta_error = [meta_err_lower, meta_err_upper]
    dpconv_error = [dpconv_err_lower, dpconv_err_upper]

    # Create the plot -> convert to boxplot per num_rels
    # prepare grouped data for boxplots
    import numpy as np
    import matplotlib.patches as mpatches

    # union of groups from both datasets (sorted)
    meta_groups = sorted(meta_df['num_rels'])
    dpconv_groups = sorted(dpconv_df['num_rels'])
    all_groups = sorted(set(meta_groups).union(dpconv_groups))

    # collect values per group (in ms)
    meta_values = []
    dpconv_values = []
    groups_included = []
    for g in all_groups:
        mv = (meta_df.loc[meta_df['num_rels'] == g, 'total_opt_time'] / 1000).dropna().tolist()
        dv = (dpconv_df.loc[dpconv_df['num_rels'] == g, 'opt_time'] / 1000).dropna().tolist()
        # skip groups with no data at all
        print(g, mv, dv)
        # if len(mv) == 0 and len(dv) == 0:
            # continue
        groups_included.append(g)
        meta_values.append(mv)
        dpconv_values.append(dv)

    if series == 'overall':
        plt.rcParams['font.size'] = 18
        fig = plt.figure(figsize=(7.5, 2.5))
    else:
        plt.rcParams['font.size'] = 17
        # if series == 'dsb-exact':
            # plt.figure(figsize=(3.5, 4))
        # else:
        fig = plt.figure(figsize=(6, 4))

    # categorical positions and offsets
    n = len(groups_included)
    positions = np.arange(n)
    width = 0.6
    pos_meta = positions
    pos_dpconv = positions

    # draw boxplots for each series at the offset positions
    bp_meta = plt.boxplot(meta_values, positions=groups_included, widths=width,
                          patch_artist=True, manage_ticks=False, showfliers=False)
    bp_dpconv = plt.boxplot(dpconv_values, positions=groups_included, widths=width,
                             patch_artist=True, manage_ticks=False, showfliers=False)

    # style boxes and all related lines (whiskers, caps, medians, fliers)
    # meta: box face = colors[0], box edge = colors[1]
    for box in bp_meta['boxes']:
        box.set(facecolor=colors[0], alpha=0.8, edgecolor='C0')
    for whisker in bp_meta.get('whiskers', []):
        whisker.set(color='C0', linewidth=1.0)
    for cap in bp_meta.get('caps', []):
        cap.set(color='C0', linewidth=1.0)
    for median in bp_meta.get('medians', []):
        median.set(color='C0', linewidth=1.2)
    for flier in bp_meta.get('fliers', []):
        flier.set(markeredgecolor='C0')

    # dpconv: box face = colors[1], box edge = colors[0]
    for box in bp_dpconv['boxes']:
        box.set(facecolor=colors[1], alpha=0.8, edgecolor=dark_orange, hatch='ooo')
    for whisker in bp_dpconv.get('whiskers', []):
        whisker.set(color=dark_orange, linewidth=1.0)
    for cap in bp_dpconv.get('caps', []):
        cap.set(color=dark_orange, linewidth=1.0)
    for median in bp_dpconv.get('medians', []):
        median.set(color=dark_orange, linewidth=1.2)
    for flier in bp_dpconv.get('fliers', []):
        flier.set(markeredgecolor=dark_orange)

    # log scale for y axis (times must be positive)
    plt.yscale('log')

    # labels and legend (use patches)
    plt.xlabel('Number of relations', weight='bold')
    if series == 'overall':
        plt.ylabel('Optimization\ntime (ms)', weight='bold')
    else:
        plt.ylabel('Optimization time (ms)', weight='bold')
    p_meta = mpatches.Patch(facecolor=colors[0], edgecolor='C0', label='metaDecomp', alpha=0.8)
    p_dpconv = mpatches.Patch(facecolor=colors[1], edgecolor=dark_orange, label='DPconv', alpha=0.8, hatch='oooo')
    plt.legend(handles=[p_meta, p_dpconv], framealpha=0.85, labelspacing=0.3, prop=legend_font)

    # optional timeout line — compute from raw data to avoid index misalignment
    overall_max = max(
        dpconv_df['opt_time'].max() if not dpconv_df['opt_time'].empty else 0,
        meta_df['total_opt_time'].max() if not meta_df['total_opt_time'].empty else 0
    )
    if overall_max >= 3e8:
        plt.axhline(y=3e5, color='dimgray', linestyle=':')
        if series == 'overall':
            plt.text(x=groups_included[-1], y=3e5, s='Timeout (5 min)', color='dimgray', va='top', ha='right')
        else:
            plt.text(x=groups_included[0], y=3e5, s='Timeout (5 min)', color='dimgray', va='top', ha='left')

    fig.gca().xaxis.set_major_locator(MaxNLocator(integer=True))

    plt.tight_layout(pad=0)
    plt.savefig(os.path.join(figures_path, f'opt-time-{series}.pdf'), format='pdf')
    plt.show()
