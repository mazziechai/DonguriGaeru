import os
import re
from datetime import datetime, timezone

import jpype
from algorithm import Rating, hikuwr_rating
from matplotlib import pyplot as plt
from sqlalchemy import asc, create_engine
from sqlalchemy.orm import Session

from config import LOCAL_SQL_HOSTNAME, LOCAL_SQL_PASSWORD, LOCAL_SQL_USERNAME
from donguri_gaeru.database import Match


def db2ratings(dbname, asof_date, times):
    # Open the connection to the database.
    sql = "postgresql://{username}:{password}@{hostname}/{database}"
    url = sql.format(
        username=LOCAL_SQL_USERNAME,
        password=LOCAL_SQL_PASSWORD,
        hostname=LOCAL_SQL_HOSTNAME,
        database=dbname,
    )

    engine = create_engine(url)

    # Compute the ratings.
    with engine.connect() as connection:
        with Session(bind=connection) as session:
            matches = (
                session.query(Match)
                .filter(Match.created <= asof_date)
                .order_by(asc(Match.created))
                .all()
            )

            # Using Hiku's original Java implementation.
            WorldRanking = jpype.JClass("fr/hiku/WorldRanking")()
            for match in matches:
                WorldRanking.pythonAddMatch(
                    match.playerA.name,
                    match.playerB.name,
                    match.scoreA,
                    match.scoreB,
                    match.created.strftime("%d/%m/%y"),
                )
            WorldRanking.pythonExecuteAlgorithm(times)
            truth_ratings = {
                str(name): float(rating)
                for name, rating in zip(
                    WorldRanking.playerNames, WorldRanking.playerRatings
                )
            }

            # Using terramyst's Python adaptation.
            actual_ratings = hikuwr_rating(matches, asof_date, times)
            actual_ratings = {p.name: r.med for p, r in actual_ratings.items()}

    engine.dispose()
    return truth_ratings, actual_ratings


def file2ratings(filename):
    # Load the dateset from the text file.
    filename = os.path.join(os.path.dirname(__file__), filename)
    with open(filename, "r", encoding="utf-8") as file:
        lines = file.readlines()

    # Parse the dataset and return a dictionary.
    decimal = r"\s(\d+.\d\d)"
    pattern = r"^\d+\s(\S+)\s\S+" + decimal * 3 + "$"
    ratings = dict()
    for line in lines:
        if match := re.match(pattern, line):
            ratings[match.group(1)] = Rating(
                min=float(match.group(2)),
                med=float(match.group(3)),
                max=float(match.group(4)),
            )

    return ratings


def ratings2file(dbname, times, truth, actual):
    filename = os.path.join(os.path.dirname(__file__), "results", dbname + ".txt")
    with open(filename, "w", encoding="utf-8") as file:
        file.write("Iterations: {:d} times\n".format(times))
        file.write("Median ratings: ( name, java (truth), python (actual) ):\n")
        ranking = sorted(truth, key=lambda name: truth[name], reverse=True)
        for name in ranking:
            file.write("{}, {:0.2f}, {:0.2f}\n".format(name, truth[name], actual[name]))


def compare_algorithms(ax, truth, actual):
    ratings = [(truth[p], actual[p] - truth[p]) for p in truth]
    ax.scatter(*zip(*ratings), s=5)


jpype.startJVM(classpath=[os.path.join(os.path.dirname(__file__))])

RATINGS_FEB_2020 = file2ratings("ratings_feb_2020.txt")
asof_date = datetime(2020, 2, 1, tzinfo=timezone.utc)

# Compare the rating algorithm to hikuwr and save plot to file.
dbnames = ["hikuwr", "hikuwr_puyolobbyA", "hikuwr_puyolobbyB"]
times = 300

fig, axes = plt.subplots(nrows=1, ncols=len(dbnames), figsize=(16, 9))
for dbname, ax in zip(dbnames, axes):
    truth_ratings, actual_ratings = db2ratings(dbname, asof_date, times)
    compare_algorithms(ax, truth=truth_ratings, actual=actual_ratings)

    ratings2file(dbname, times, truth_ratings, actual_ratings)

    ax.title.set_text(dbname)
    ax.set_xlabel("Original Java Implementation (Rating)")
    ax.set_ylabel("Reimplemented in Python (Rating Error)")
    ax.set(adjustable="box", aspect="equal")
    ax.grid()

fig.suptitle("Comparing Java and Python Hiku WorldRanking Algorithm Implementations")
plt.savefig(os.path.join(os.path.dirname(__file__), "compare.png"))

jpype.shutdownJVM()
