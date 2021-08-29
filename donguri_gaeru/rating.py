import random
from collections import namedtuple

from sqlalchemy import create_engine
from sqlalchemy.orm import Session

from config import LOCAL_SQL_HOSTNAME, LOCAL_SQL_PASSWORD, LOCAL_SQL_USERNAME
from donguri_gaeru.database import Player

Rating = namedtuple("Rating", "min med max")


def db2rating(dbname):
    # Return a dictionary of player ratings from the chosen database.
    sql = "postgresql://{username}:{password}@{hostname}/{database}"
    url = sql.format(
        username=LOCAL_SQL_USERNAME,
        password=LOCAL_SQL_PASSWORD,
        hostname=LOCAL_SQL_HOSTNAME,
        database=dbname,
    )
    engine = create_engine(url)
    ratings = dict()
    with engine.connect() as connection:
        with Session(bind=connection) as session:
            # For each player in the database, assign a random rating.
            # Median is a random number between 0 and 10 (uniform).
            # Min/Max is a random number from a normal distribution.
            players = session.query(Player).all()
            for player in players:
                med = random.uniform(0, 10)
                dev = abs(random.gauss(mu=0, sigma=0.2))
                ratings[player.name] = Rating(med - dev, med, med + dev)

    engine.dispose()
    return ratings
