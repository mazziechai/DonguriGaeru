import os
import re

from matplotlib import pyplot as plt
from scipy.stats.stats import pearsonr

from donguri_gaeru.rating import Rating, db2rating


def load_rankings(filename):
    # Load the dateset from the text file.
    filename = os.path.join(os.path.dirname(__file__), filename)
    with open(filename, "r", encoding="utf-8") as file:
        lines = file.readlines()

    # Parse the dataset and return a dictionary.
    decimal = r"\s(\d+.\d\d)"
    pattern = r"^\d+\s(\S+)\s\S+" + decimal * 3 + "$"
    ranking = dict()
    for line in lines:
        if match := re.match(pattern, line):
            ranking[match.group(1)] = Rating(
                min=float(match.group(2)),
                med=float(match.group(3)),
                max=float(match.group(4)),
            )

    return ranking


def compare_rankings(ax, truth, actual):
    # Delete any ratings in truth that are absent from actual.
    truth = {p: r for p, r in truth.items() if p in actual}

    # Sort the two dictionary in order of the median rating.
    truth_sorted = sorted(truth, key=lambda k: truth[k].med, reverse=True)
    actual_sorted = sorted(actual, key=lambda k: actual[k].med, reverse=True)

    # Scatterplot the players actual ranking versus truth ranking.
    arank = [actual_sorted.index(p) for p in truth_sorted]
    trank = range(len(truth_sorted))
    ax.scatter(trank, arank, s=5)
    return pearsonr(trank, arank)[0]


RANKING_FEB_2020 = load_rankings("ranking_feb_2020.txt")

# Compare the ranking algorithm to hikuwr and save plot to file.
dbnames = ["hikuwr_puyolobbyA", "hikuwr_puyolobbyB"]
fig, axes = plt.subplots(nrows=1, ncols=len(dbnames), figsize=(12, 8))
for dbname, ax in zip(dbnames, axes):
    corr = compare_rankings(ax, RANKING_FEB_2020, db2rating(dbname))
    ax.title.set_text(dbname + " (Pearson: {:0.2f})".format(corr))
    ax.set_xlabel("Hiku World Ranking (Feb 2020)")
    ax.set_ylabel("Donguri Gaeru")
    ax.set(adjustable="box", aspect="equal")
    ax.grid()

plt.savefig(os.path.join(os.path.dirname(__file__), "compare_rankings.png"))
