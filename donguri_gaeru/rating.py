from collections import defaultdict, namedtuple

from glicko2 import Player as Glicko
from sqlalchemy import create_engine
from sqlalchemy.orm import Session

from config import LOCAL_SQL_HOSTNAME, LOCAL_SQL_PASSWORD, LOCAL_SQL_USERNAME
from donguri_gaeru.database import Match, Player, Rating

HikuRating = namedtuple("Rating", "min med max")


def db2rating(dbname, start_date, end_date, rating_period):
    # Open the connection to the database.
    sql = "postgresql://{username}:{password}@{hostname}/{database}"
    url = sql.format(
        username=LOCAL_SQL_USERNAME,
        password=LOCAL_SQL_PASSWORD,
        hostname=LOCAL_SQL_HOSTNAME,
        database=dbname,
    )

    engine = create_engine(url)

    with engine.connect() as connection:
        with Session(bind=connection) as session:
            # Clear the rating table of any entries.
            session.query(Rating).delete()

            # Loop through the date range, compute ratings at the specified period.
            date = start_date
            while date <= end_date:
                compute_ratings(session, date, rating_period)
                date += rating_period

            # Create the output rating dictionary to be returned.
            ratings = dict()
            for player in session.query(Player).all():
                rating = player.rating(end_date)
                if rating is not None:
                    ratings[player.name] = HikuRating(
                        min=rating.rating - rating.rd,
                        med=rating.rating,
                        max=rating.rating + rating.rd,
                    )

    engine.dispose()
    return ratings


def compute_ratings(dbsession, asof_date, rating_period):
    # Query all matches which occurred during the rating period.
    matches = (
        dbsession.query(Match)
        .filter(Match.created <= asof_date)
        .filter(Match.created > asof_date - rating_period)
    )

    # Loop through matches to construct Glicko2 player update inputs.
    glicko_inputs = defaultdict(list)
    default_rating, default_rd, default_vol, default_tau = 1500, 350, 0.06, 0.5
    for match in matches:
        glickoA = (match.playerA, match.playerB, match.scoreA / match.games)
        glickoB = (match.playerB, match.playerA, match.scoreB / match.games)

        for player, opponent, outcome in [glickoA, glickoB]:
            rating = opponent.rating(asof_date)
            if rating is None:
                glicko_inputs[player].append((default_rating, default_rd, outcome))
            else:
                glicko_inputs[player].append((rating.rating, rating.rd, outcome))

    # For each player, update the Glicko2 rating.
    new_ratings = []
    for player, glicko in glicko_inputs.items():
        rating = player.rating(asof_date)

        if rating is None:
            glicko_player = Glicko(default_rating, default_rd, default_vol)
        else:
            glicko_player = Glicko(rating.rating, rating.rd, rating.vol)

        glicko_player._tau = default_tau
        glicko_player.update_player(*zip(*glicko))

        new_ratings.append(
            Rating(
                player_id=player.id,
                asof_date=asof_date,
                rating=glicko_player.rating,
                rd=glicko_player.rd,
                vol=glicko_player.vol,
            )
        )

    dbsession.add_all(new_ratings)
    dbsession.commit()

    # TODO: Each player with a previous rating who did not compete in this rating period
    # should have their volatility increased according to method: did_not_compete()
