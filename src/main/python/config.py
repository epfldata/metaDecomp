import matplotlib.pyplot as plt
import os

results_path = os.path.abspath(os.path.join(os.path.dirname(__file__), "../../../experiment-results"))

plt.rcParams['font.sans-serif'] = ['Linux Libertine O']
plt.rcParams['font.family'] = 'sans-serif'

epfl_leman_color = (0, 167/255, 159/255)
epfl_canard_color = (0, 116/255, 128/255)
orange_color = (241/255, 89/255, 42/255)
yellow_color = (224/255, 195/255, 0/255)
epfl_groseille_color = (181/255, 31/255, 31/255)
