import math
import random
import time
from collections import namedtuple

from progress.spinner import PixelSpinner

Rating = namedtuple("Rating", "min med max")


def hikuwr_rating(matches, asof_date, iterations):
    # Create an initial dictionary of player ratings to the default.
    med_ratings = dict()
    for match in matches:
        med_ratings[match.playerA] = 1
        med_ratings[match.playerB] = 1

    # Iteratively converge on the median player ratings, shuffle matches by week.
    start = time.time()
    for _ in PixelSpinner("hikuwr algorithm converging... ").iter(range(iterations)):
        random.shuffle(matches)
        med_ratings = hikuwr_med_rating(matches, asof_date, med_ratings)

    fstring = "... success ({:0.1f}s elapsed, {:d} iterations)."
    print(fstring.format(time.time() - start, iterations))

    # Construct the final rating with uncertainty bounds.
    return hikuwr_certainty(matches, med_ratings)


def hikuwr_med_rating(matches, asof_date, ratings):
    # Loop through matches applying the Hiku World Ranking Algorithm.
    for match in matches:
        coefficient = hikuwr_coefficient(match, asof_date, ratings)

        # Compute the match delta score.
        deltaA = math.sqrt(ratings[match.playerB] / ratings[match.playerA])
        deltaB = math.sqrt(ratings[match.playerA] / ratings[match.playerB])
        delta = (deltaA * match.scoreA - deltaB * match.scoreB) * coefficient

        # Update the ratings.
        alpha = 1 + delta if delta > 0 else 1 / (1 - delta)
        ratings[match.playerA] = ratings[match.playerA] * alpha
        ratings[match.playerB] = ratings[match.playerB] / alpha

    return ratings


def hikuwr_certainty(matches, med_ratings):
    # TODO: Compute twisted certainty value.
    ratings = dict()
    for player, med_rating in med_ratings.items():
        ratings[player] = Rating(min=med_rating, med=med_rating, max=med_rating)

    return ratings


def hikuwr_coefficient(match, asof_date, ratings):
    # Compute the match time coefficent.
    time_coefficient = (asof_date - match.created).days / 365.25
    time_coefficient = 1 / (1.5 * (time_coefficient ** 2) + 1)  # magic function

    # Compute the match level coefficient.
    level_coefficientA = ratings[match.playerA] / ratings[match.playerB]
    level_coefficientB = ratings[match.playerB] / ratings[match.playerA]
    level_coefficient = min(level_coefficientA, level_coefficientB)

    # Compute the overall match coefficient.
    return time_coefficient * level_coefficient / 1000  # magic function
