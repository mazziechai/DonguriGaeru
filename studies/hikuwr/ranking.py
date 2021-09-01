import os
import re
from datetime import datetime, timedelta, timezone

from matplotlib import pyplot as plt
from scipy.stats.stats import pearsonr

from donguri_gaeru.rating import HikuRating, db2rating


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
            ranking[match.group(1)] = HikuRating(
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

rating_period = timedelta(weeks=2)
start_date = datetime(2019, 12, 1, tzinfo=timezone.utc)
end_date = datetime(2020, 2, 1, tzinfo=timezone.utc)

# Compare the ranking algorithm to hikuwr and save plot to file.
dbnames = ["hikuwr", "hikuwr_puyolobbyA", "hikuwr_puyolobbyB"]
fig, axes = plt.subplots(nrows=1, ncols=len(dbnames), figsize=(16, 9))
for dbname, ax in zip(dbnames, axes):
    ranking = db2rating(dbname, start_date, end_date, rating_period)
    corr = compare_rankings(ax, truth=RANKING_FEB_2020, actual=ranking)
    ax.title.set_text(dbname + " (Pearson: {:0.2f})".format(corr))
    ax.set_xlabel("Hiku World Ranking (Feb 2020)")
    ax.set_ylabel("Donguri Gaeru Ranking")
    ax.set(adjustable="box", aspect="equal")
    ax.grid()

plt.savefig(os.path.join(os.path.dirname(__file__), "compare_rankings.png"))
