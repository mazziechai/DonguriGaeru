import argparse
import os
import re
import sys
import time
from collections import Counter
from contextlib import contextmanager
from datetime import datetime, timezone

import jpype
import numpy as np
from matplotlib import pyplot as plt
from sqlalchemy import asc, create_engine
from sqlalchemy.orm import Session

from config import DB_HOSTNAME, DB_PASSWORD, DB_USERNAME
from donguri_gaeru.algorithm import Rating, hikuwr_rating
from donguri_gaeru.database import Match, Player

# Define constants for validation routines.
ASOF_DATE = datetime(2020, 2, 1, tzinfo=timezone.utc)


def result_filepath(*path):
    # Utility function to determine complete filepath for result outputs.

    return os.path.join(os.path.dirname(__file__), "results", *path)


@contextmanager
def dbsession(dbname):
    # Return a local database session as a context manager.

    sql = "postgresql://{username}:{password}@{hostname}/{database}"
    url = sql.format(
        username=DB_USERNAME,
        password=DB_PASSWORD,
        hostname=DB_HOSTNAME,
        database=dbname,
    )
    engine = create_engine(url)
    with engine.connect() as connection:
        with Session(bind=connection) as session:
            yield session

    engine.dispose()


class TimedMessage:
    # Utility class for printing timed messages to the command line.

    def __init__(self):
        self.start = time.time()

    def __call__(self, msg):
        end = time.time()
        print("   - {} ({:0.1f}s elapsed)".format(msg, end - self.start))
        self.start = end


def get_matches(session, routine):
    # Utility function for loading all database matches prior to the specified
    # date and printing a timed message to the command line.

    print(routine, "as of", ASOF_DATE.strftime("%d/%m/%y"))

    matches = (
        session.query(Match)
        .filter(Match.created <= ASOF_DATE)
        .order_by(asc(Match.created))
        .all()
    )

    msg = TimedMessage()
    msg("Loaded {:d} matches from the database.".format(len(matches)))

    return matches, msg


def validate_algorithm(threshold):
    # This routine will validate the Python algorithm implementation.
    # See results/validate_algorithm.txt for the output which compares:
    #   1) The ratings posted on the bayoen.fr world ranking page as of February 2020.
    #   2) The ratings generated by the original Java code (with minor adjustments).
    #   3) The ratings generated by the Python code adaptation.
    #
    # A successful result will show strong correspondence between all 3 results.

    routine = "validate_algorithm (threshold={:0.2g})".format(threshold)

    with dbsession(dbname="hikuwr") as session:
        matches, msg = get_matches(session, routine)

        wiki = load_wiki_results(session)
        msg("Load {:d} ratings from wiki.".format(len(wiki)))

        times = 50000
        java = load_java_results(session, matches, ASOF_DATE, times)
        msg("Compute ratings with Java algorithm, {:d} iterations.".format(times))

        python, info = hikuwr_rating(matches, ASOF_DATE, threshold, info=True)
        msg("Compute ratings with Python algorithm, {:d} iterations.".format(info.n))

    # Get the set of players common to all three results, sorted by wiki minimum rating.
    players = set(wiki) & set(java) & set(python)
    players = sorted(players, key=lambda player: wiki[player].min, reverse=True)
    results = [("wiki", wiki), ("java", java), ("python", python)]

    # Write the results to file.
    filepath = result_filepath("validate_algorithm.txt")
    fstring = "{}\t{}\t{:0.2f}\t{:0.2f}\t{:0.2f}\n"
    with open(filepath, "w", encoding="utf-8") as file:
        file.write("Ratings as of " + ASOF_DATE.strftime("%d/%m/%y") + ".\n")
        file.write("Java Iterations={:d}\n".format(times))
        file.write("Python Threshold={:0.2g}\n\n".format(threshold))
        for player in players:
            for src, result in results:
                file.write(fstring.format(src, player.name, *result[player]))
            file.write("\n")


def load_wiki_results(session):

    # Load the dateset from the text file.
    filepath = os.path.join(os.path.dirname(__file__), "source", "ratings_feb_2020.txt")
    with open(filepath, "r", encoding="utf-8") as file:
        lines = file.readlines()

    # Parse the dataset and return a dictionary. Query the database by player name.
    decimal = r"\s(\d+.\d\d)"
    pattern = r"^\d+\s(\S+)\s\S+" + decimal * 3 + "$"
    ratings = dict()
    for match in [re.match(pattern, line) for line in lines]:
        if match is None:
            continue

        player = session.query(Player).filter(Player.name == match.group(1)).first()

        if player is not None:
            ratings[player] = Rating(
                min=float(match.group(2)),
                med=float(match.group(3)),
                max=float(match.group(4)),
            )

    return ratings


def load_java_results(session, matches, asof_date, iterations):

    jpype.startJVM(classpath=[os.path.dirname(__file__)])
    WorldRanking = jpype.JClass("source/WorldRanking")()

    for match in matches:
        WorldRanking.pythonAddMatch(
            match.playerA.name,
            match.playerB.name,
            match.scoreA,
            match.scoreB,
            match.created.strftime("%d/%m/%y"),
        )

    WorldRanking.pythonExecuteAlgorithm(iterations, asof_date.strftime("%d/%m/%y"))
    output = zip(
        WorldRanking.playerNames,
        WorldRanking.playerRatings,
        WorldRanking.playerLowerBounds,
        WorldRanking.playerUpperBounds,
    )
    results = {}
    for name, med_rating, lb_rating, ub_rating in output:
        player = session.query(Player).filter(Player.name == str(name)).one()
        rating = Rating(min=lb_rating, med=med_rating, max=ub_rating)
        results[player] = rating

    jpype.shutdownJVM()
    return results


def validate_convergence(threshold):
    # This routine will plot the algorithm convergence metric over iterations.
    # See results/validate_convergence.png for a plot.

    routine = "validate_convergence (threshold={:0.2g})".format(threshold)

    with dbsession(dbname="hikuwr") as session:
        matches, msg = get_matches(session, routine)

        _, info = hikuwr_rating(matches, ASOF_DATE, threshold, info=True)
        msg("Compute ratings with Python algorithm, {:d} iterations.".format(info.n))

    plt.semilogy(info.maxdeltas, label="largest rating change")
    plt.semilogy(info.avedeltas, label="average rating change")
    plt.title("Rating Change Over Iterations")
    plt.grid()
    plt.xlabel("Iterations")
    plt.ylabel("Rating Change")
    plt.legend()
    plt.savefig(result_filepath("validate_convergence.png"))


def matchups(n):
    # See results/matchups.txt for a list.

    routine = "matchups (number={:d})".format(n)

    with dbsession(dbname="hikuwr") as session:
        matches, msg = get_matches(session, routine)

        matchup_counter = Counter()
        match_counter = Counter()
        for match in matches:
            matchup_counter[frozenset([match.playerA.name, match.playerB.name])] += 1
            match_counter[match.playerA.name] += 1
            match_counter[match.playerB.name] += 1

    filepath = result_filepath("matchups.txt")
    fstring = "{:d} matches between:\t{} ({:d} matches)\t{} ({:d} matches)\n"
    with open(filepath, "w", encoding="utf-8") as file:
        for matchup, count in matchup_counter.most_common(n):
            playerA, playerB = tuple(matchup)
            params = (playerA, match_counter[playerA], playerB, match_counter[playerB])
            file.write(fstring.format(count, *params))

    msg("Most common matchups written to file.")


def validate_predicts(playerA, playerB, threshold):
    # Remove from the matchup list any matches between the two players.
    # At the time of each match, compute player ratings and compare the
    # predicted score range against the actual result.
    # See results/validate_predicts/playerA_vs_playerB.png for a plot.

    routine = "validate_predicts ({} vs {}, threshold={:0.2g})"
    routine = routine.format(playerA, playerB, threshold)

    def separate_matches(matches):
        base, predict = [], []
        matchup = {playerA, playerB}
        for match in matches:
            if matchup != {match.playerA.name, match.playerB.name}:
                base.append(match)
            else:
                predict.append(match)

        return base, predict

    with dbsession(dbname="hikuwr") as session:
        matches, msg = get_matches(session, routine)

        base, predict = separate_matches(matches)
        msg("{:d} matches identified to predict.".format(len(predict)))

        ratings = []
        for i, match in enumerate(predict):
            datefilt = lambda m: m.created <= match.created  # noqa: E731
            matches_to_rate = list(filter(datefilt, base))
            rating = hikuwr_rating(matches_to_rate, match.created, threshold)
            ratings.append((match, rating))
            msg("Ratings computed for match {:d} of {:d}.".format(i + 1, len(predict)))

        dates, ratingA, ratingB, ratingAerr, ratingBerr = ([] for i in range(5))
        score_predict, score_predict_err, score_actual = ([] for i in range(3))

        for match, rating in ratings:
            dates.append(match.created)
            if match.playerA.name == playerA:
                rA, rB, sA = rating[match.playerA], rating[match.playerB], match.scoreA
            else:
                rA, rB, sA = rating[match.playerB], rating[match.playerA], match.scoreB

            # Get the player ratings over time.
            ratingA.append(rA.med)
            ratingAerr.append([rA.med - rA.min, rA.max - rA.med])
            ratingB.append(rB.med)
            ratingBerr.append([rB.med - rB.min, rB.max - rB.med])

            # Predict the matchup results over time.
            score_actual.append(sA / match.games)
            predict = 1 / (rB.med / rA.med + 1)
            lo_predict = 1 / (rB.max / rA.min + 1)
            hi_predict = 1 / (rB.min / rA.max + 1)
            score_predict.append(predict)
            score_predict_err.append([predict - lo_predict, hi_predict - predict])

    fig, axs = plt.subplots(2, 1, figsize=(6, 8), sharex=True)

    def ebar(ax, y, err, label):
        ax.errorbar(dates, y, np.transpose(err), fmt=".", capsize=2, label=label)

    ebar(axs[0], ratingA, ratingAerr, "PlayerA: " + playerA)
    ebar(axs[0], ratingB, ratingBerr, "PlayerB: " + playerB)
    axs[0].grid(which="both")
    axs[0].legend()
    axs[0].set_ylabel("Player Ratings")

    ebar(axs[1], score_predict, score_predict_err, "predict")
    axs[1].plot(dates, score_actual, "rx", label="actual")
    axs[1].grid(which="both")
    axs[1].legend()
    axs[1].set_ylabel("PlayerA Fractional Win Total")

    [tick.set_rotation(30) for tick in axs[1].get_xticklabels()]

    filename = "{}_vs_{}.png".format(playerA, playerB)
    plt.savefig(result_filepath("validate_predicts", filename))


def argparser():
    # Return the argument parser for the command line interface.

    parser = argparse.ArgumentParser(
        description=(
            "Run World Ranking rating system validation routines as of 01/02/20.\n"
            "Default threshold for Python algorithm is 1e-6 (maximum rating delta)."
        ),
        formatter_class=argparse.RawTextHelpFormatter,
    )

    parser.add_argument(
        "-a",
        "--algorithm",
        nargs="?",
        const="1e-6",
        type=float,
        metavar="THRESHOLD",
        help="compare algorithm results between the Wiki, Java, and Python",
    )

    parser.add_argument(
        "-c",
        "--convergence",
        nargs="?",
        const="1e-6",
        type=float,
        metavar="THRESHOLD",
        help="plot the algorithm convergence metrics",
    )

    parser.add_argument(
        "-m",
        "--matchups",
        nargs="?",
        const="250",
        type=int,
        metavar="N",
        help="list the most common player matchups (default 250)",
    )

    def predicts_args(arg):
        try:
            return float(arg)
        except ValueError:
            return arg

    parser.add_argument(
        "-p",
        "--predicts",
        nargs=3,
        type=predicts_args,
        metavar=("PLAYERA", "PLAYERB", "THRESHOLD"),
        help="compute algorithm predicts for a specific matchup",
    )

    return parser


def main():
    # Run the command line interface.

    parser = argparser()

    if not len(sys.argv) > 1:
        parser.print_help()
        return

    args = parser.parse_args()
    if args.algorithm is not None:
        print()
        validate_algorithm(args.algorithm)
    if args.convergence is not None:
        print()
        validate_convergence(args.convergence)
    if args.matchups is not None:
        print()
        matchups(int(args.matchups))
    if args.predicts is not None:
        print()
        validate_predicts(*args.predicts)
