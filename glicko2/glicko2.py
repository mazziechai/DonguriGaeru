from math import exp, log, pi, sqrt

TAU = 0.75

WIN = 1.0
LOSS = 0.0
DRAW = 0.5


class Rating:
    def __init__(self, rating: float = 1500, rd: float = 350, volatility: float = 0.06):
        self.rating = rating
        self.rd = rd
        self.volatility = volatility


def rate(
    player: Rating,
    results: dict[Rating, list[float]],
):
    μ: float = (player.rating - 1500) / 173.7178
    φ: float = player.rd / 173.7178
    σ: float = player.volatility

    # Initializing variables
    v = 0
    s = 0

    if len(results) > 0:
        for opponent in results:
            # Getting the opponent

            μⱼ = (opponent.rating - 1500) / 173.7178
            φⱼ = opponent.rd / 173.7178

            for score in results[opponent]:
                # Calculating inverse of the variance
                v += _v(μ, μⱼ, φⱼ)
                # Calculating Glicko-2 score
                s += _s(μ, μⱼ, φⱼ, score)

        v = pow(v, -1)
        Δ = v * s

        # New volatility
        σʹ = _determine_volatility(φ, σ, Δ, v)

        # New phi
        φ1 = sqrt(pow(φ, 2) + pow(σʹ, 2))
        φʹ = 1 / sqrt(1 / pow(φ1, 2) + 1 / v)

        # New rating
        μʹ = μ + pow(φʹ, 2) * s

        # Return new rating
        new_rating = μʹ * 173.7178 + 1500
        new_rd = φʹ * 173.7178
        new_volatility = σʹ

        return Rating(new_rating, new_rd, new_volatility)

    else:
        # Just updating the RD
        φʹ = sqrt(pow(φ, 2) + pow(σ, 2))
        new_rd = φʹ * 173.7178

        return Rating(player.rating, new_rd, player.volatility)


def _determine_volatility(φ: float, σ: float, Δ: float, v: float):
    # Initializing variables
    Δsq = pow(Δ, 2)
    φsq = pow(φ, 2)
    τ = TAU

    ε = 0.000001  # convergence tolerance

    # Let A = a = ln(σ^2)
    A = log(pow(σ, 2))

    # and define
    def f(x: float):
        n = exp(x) * (Δsq - φsq - v - exp(x))
        d = 2 * pow(φsq + v + exp(x), 2)
        return n / d - (x - A) / pow(τ, 2)

    # Set the inital values of the iterative algorithm
    B = 0

    if Δsq > φsq + v:
        B = log(Δsq - φsq - v)
    else:
        k = 1
        while f(A - k * τ) < 0:
            k += 1

        B = A - k * τ

    # Let fA = f(A) and fB = f(B).
    fA = f(A)
    fB = f(B)

    # While |B−A| > ε, carry out the following steps.
    while abs(B - A) > ε:
        C = A + (A - B) * fA / (fB - fA)
        fC = f(C)

        if fC * fB < 0:
            A = B
            fA = fB

        else:
            fA /= 2

        B = C
        fB = fC

    return exp(A / 2)


def _g(φ: float):
    return 1 / sqrt(1 + 3 * pow(φ, 2) / pow(pi, 2))


def _e(μ: float, μⱼ: float, φⱼ: float):
    return 1 / (1 + exp(-_g(φⱼ) * (μ - μⱼ)))


def _s(μ: float, μⱼ: float, φⱼ: float, score: float):
    return _g(φⱼ) * (score - _e(μ, μⱼ, φⱼ))


def _v(μ: float, μⱼ: float, φⱼ: float):
    return pow(_g(φⱼ), 2) * _e(μ, μⱼ, φⱼ) * (1 - _e(μ, μⱼ, φⱼ))
