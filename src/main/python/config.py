import matplotlib.pyplot as plt
import os

root_path = os.path.abspath(os.path.join(os.path.dirname(__file__), "../../.."))
results_path = os.path.abspath(os.path.join(root_path, "experiment-results"))
figures_path = os.path.abspath(os.path.join(results_path, "figures"))

benchmarks = ['dsb', 'job-original', 'musicbrainz', 'job-large']

plt.rcParams['font.sans-serif'] = ['Linux Libertine O']
plt.rcParams['font.family'] = 'sans-serif'
plt.rcParams['mathtext.fontset'] = 'dejavusans'
plt.rcParams['mathtext.default'] = 'regular'

green_color = (0, 167/255, 159/255)
dark_green_color = (0, 116/255, 128/255)
yellow_color = (224/255, 195/255, 0/255)

# https://dovydas.com/blog/colorblind-friendly-diagrams
colors = [(86/255, 180/255, 233/255), (230/255, 159/255, 0), (0, 158/255, 115/255), (204/255, 121/255, 167/255), (240/255, 228/255, 66/255), (0, 114/255, 178/255), (213/255, 94/255, 0/255)]
dark_orange = (213/255, 94/255, 0)