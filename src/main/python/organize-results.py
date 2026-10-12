"""Copy timestamped CSVs to the filenames expected by the plotting scripts."""
import os
import re
import shutil
from pathlib import Path

results = Path(os.environ.get("RESULTS_DIR", "experiment-results"))
for source in sorted(results.glob("*.csv")):
    name = re.sub(r"-\d{4}-\d{2}-\d{2}T\d{2}-\d{2}-\d{2}(?=\.csv$)", "", source.name)
    if name != source.name:
        shutil.copy2(source, results / name)
