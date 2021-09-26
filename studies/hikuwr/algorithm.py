import math
from collections import defaultdict, namedtuple
from functools import partial

Rating = namedtuple("Rating", "min med max")
MatchTuple = namedtuple("MatchTuple", "playerA playerB points delta")
RatingInfo = namedtuple("RatingInfo", "n maxdeltas avedeltas")


def hikuwr_rating(matches, asof_date, threshold=1e-6, nlimit=50000, info=False):
    # Convergence threshold is specified against the maximum rating delta.
    # A tuple is returned with algorithm infodata if info=True.

    # Define subfunction for computing algorithm iterative convergence.
    def delta(ratingA, ratingB, scoreA, scoreB):
        lvlcoefA = ratingA / ratingB
        deltaA = math.sqrt(lvlcoefA)
        beta = scoreA / deltaA - scoreB * deltaA
        return beta * lvlcoefA if lvlcoefA < 1 else beta / lvlcoefA

    # Initialize algorithm data structures for efficiency.
    match_tuples = []
    med_ratings = dict()
    players = dict()
    for match in matches:
        years = (asof_date - match.created).days / 365.25
        tcoef = 1 / (1.5 * (years ** 2) + 1)  # magic function
        match_tuples.append(
            MatchTuple(
                playerA=match.playerA.name,
                playerB=match.playerB.name,
                points=(match.scoreA + match.scoreB) * tcoef,
                delta=partial(
                    delta,
                    scoreA=match.scoreA * tcoef / 1000,
                    scoreB=match.scoreB * tcoef / 1000,
                ),
            )
        )
        med_ratings[match.playerA.name] = 1
        med_ratings[match.playerB.name] = 1
        players[match.playerA.name] = match.playerA
        players[match.playerB.name] = match.playerB

    # Compute the median ratings.
    maxdeltas = []
    avedeltas = []
    n = 0
    while True:
        n += 1
        old_ratings = dict(med_ratings)

        for match in match_tuples:
            ratingA = med_ratings[match.playerA]
            ratingB = med_ratings[match.playerB]
            delta = match.delta(ratingA=ratingA, ratingB=ratingB)
            alpha = 1 + delta if delta > 0 else 1 / (1 - delta)
            med_ratings[match.playerA] = ratingA * alpha
            med_ratings[match.playerB] = ratingB / alpha

        deltas = [abs(med_ratings[player] - old_ratings[player]) for player in players]
        maxdeltas.append(max(deltas))
        avedeltas.append(sum(deltas) / len(deltas))
        if maxdeltas[-1] < threshold or n == nlimit:
            break

    # Compute the matchup strengths.
    matchups = defaultdict(int)
    for match in match_tuples:
        lvlcoef = med_ratings[match.playerA] / med_ratings[match.playerB]
        partial_strength = match.points * min(lvlcoef, 1 / lvlcoef)
        matchups[frozenset([match.playerA, match.playerB])] += partial_strength

    # Compute the certainties.
    ratings = {}
    for player, playerobject in players.items():
        certainty = 0
        for matchup, strength in matchups.items():
            if player in matchup:
                certainty += min(1, math.sqrt(strength) / 10)

        twisted_certainty = 0.6 / (certainty ** 1.2)
        lbound = med_ratings[player] / (1 + twisted_certainty)
        ubound = med_ratings[player] * (1 + twisted_certainty)

        ratings[playerobject] = Rating(min=lbound, med=med_ratings[player], max=ubound)

    if info:
        return ratings, RatingInfo(n, maxdeltas, avedeltas)
    else:
        return ratings
