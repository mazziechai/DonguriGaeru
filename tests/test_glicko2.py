from numpy.testing import assert_almost_equal

from glicko2.glicko2 import LOSS, WIN, Rating, rate

# Note: Below test is not rigorous enough to validate glicko2.TAU.


def test_glicko2():
    rating1 = Rating(1500, 200)
    rating2 = Rating(1400, 30)
    rating3 = Rating(1550, 100)
    rating4 = Rating(1700, 300)

    scores = {rating2: [WIN], rating3: [LOSS], rating4: [LOSS]}

    new_rating = rate(rating1, scores)

    assert_almost_equal(new_rating.rating, 1464.06, decimal=2)
    assert_almost_equal(new_rating.rd, 151.52, decimal=2)
    assert_almost_equal(new_rating.volatility, 0.05999, decimal=5)
