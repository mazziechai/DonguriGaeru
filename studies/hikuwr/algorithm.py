import math
from collections import namedtuple
from functools import partial

Rating = namedtuple("Rating", "min med max")
MatchTuple = namedtuple("MatchTuple", "playerA playerB delta")


def hikuwr_rating(matches, asof_date, iterations):

    # Define subfunction for computing algorithm iterative convergence.
    def delta(ratingA, ratingB, scoreA, scoreB):
        lvlcoefA = ratingA / ratingB
        deltaA = math.sqrt(lvlcoefA)
        beta = scoreA / deltaA - scoreB * deltaA
        return beta * lvlcoefA if lvlcoefA < 1 else beta / lvlcoefA

    # Initialize algorithm data structures for efficiency.
    match_tuples = []
    med_ratings, players = dict(), dict()
    for match in matches:
        years = (asof_date - match.created).days / 365.25
        tcoef = 1 / (1.5 * (years ** 2) + 1) / 1000  # magic function
        match_tuples.append(
            MatchTuple(
                playerA=match.playerA.name,
                playerB=match.playerB.name,
                delta=partial(
                    delta, scoreA=match.scoreA * tcoef, scoreB=match.scoreB * tcoef
                ),
            )
        )
        med_ratings[match.playerA.name] = 1
        med_ratings[match.playerB.name] = 1
        players[match.playerA.name] = match.playerA
        players[match.playerB.name] = match.playerB

    # Compute the median ratings.
    for _ in range(iterations):
        for match in match_tuples:
            ratingA = med_ratings[match.playerA]
            ratingB = med_ratings[match.playerB]
            delta = match.delta(ratingA=ratingA, ratingB=ratingB)
            alpha = 1 + delta if delta > 0 else 1 / (1 - delta)
            med_ratings[match.playerA] = ratingA * alpha
            med_ratings[match.playerB] = ratingB / alpha

    # TODO: Compute the rating uncertainty boundaries.
    ratings = {
        players[name]: Rating(min=rating, med=rating, max=rating)
        for name, rating in med_ratings.items()
    }
    return ratings
