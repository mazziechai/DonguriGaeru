import argparse
import os
import re
import subprocess
from datetime import datetime, timezone

from sqlalchemy import create_engine
from sqlalchemy.orm import Session
from sqlalchemy_utils import create_database, database_exists

from config import LOCAL_SQL_PASSWORD, LOCAL_SQL_USERNAME
from donguri_gaeru.database import Base, Match, Player

DATE_REGEX = r"^(\d\d/\d\d/\d\d)$"
GAME_REGEX = r"^(\S+) (\d+) (\d+) (\S+)$"
HEROKU_ROW_LIMIT = 9950  # 50 rows margin against 10000 limit

parser = argparse.ArgumentParser(
    description="Create a test database from Hiku's World Ranking dataset."
)
group = parser.add_mutually_exclusive_group()
group.add_argument(
    "-l", "--local", action="store_true", help="create database on local machine"
)
group.add_argument(
    "-k", "--heroku", action="store_true", help="create database on heroku cloud"
)


def create_new_player(session, name, date):
    # If the player isn't already in the database, add them.
    player = session.query(Player).filter_by(name=name).first()
    if player is None:
        player = Player(name=name, created=date)
        session.add(player)
        session.commit()
    return player


def populate_database(session, row_limit):
    # Load the dateset from the text file.
    filename = os.path.join(os.path.dirname(__file__), "hikuwr.txt")
    with open(filename, "r", encoding="utf-8") as file:
        lines = file.readlines()

    # Parse the dataset and populate the database.
    for line in lines:
        player_rows = session.query(Player).count()
        match_rows = session.query(Match).count()
        if row_limit is not None and player_rows + match_rows > row_limit:
            break

        datematch = re.match(DATE_REGEX, line)
        if datematch:
            date = datetime.strptime(datematch.group(), "%d/%m/%y")
            date = date.replace(tzinfo=timezone.utc)

        gamematch = re.match(GAME_REGEX, line)
        if gamematch:
            playerA = gamematch.group(1)
            scoreA = int(gamematch.group(2))
            scoreB = int(gamematch.group(3))
            playerB = gamematch.group(4)

            try:
                playerA = create_new_player(session, playerA, date)
                playerB = create_new_player(session, playerB, date)

                match = Match(
                    playerA_id=playerA.id,
                    playerB_id=playerB.id,
                    scoreA=scoreA,
                    scoreB=scoreB,
                    created=date,
                )
                session.add(match)
                session.commit()

            except AssertionError:
                pass


def start():
    # Parse the command line arguments.
    args = parser.parse_args()
    if args.local:
        # Get the local database access credentials via the private config.
        SQL_URL = "postgresql://{}:{}@localhost/hikuwr_db".format(
            LOCAL_SQL_USERNAME, LOCAL_SQL_PASSWORD
        )
        row_limit = None
    elif args.heroku:
        # Get the Heroku database access credentials via the CLI.
        SQL_URL = (
            subprocess.check_output(
                "heroku config:get DATABASE_URL -a hiku-worldranking", shell=True
            )
            .decode("utf-8")
            .strip()
            .replace("postgres", "postgresql")
        )
        row_limit = HEROKU_ROW_LIMIT
    else:
        parser.print_help()
        return

    # Create a clean PostgreSQL database and connect.
    dbengine = create_engine(SQL_URL)
    if not database_exists(dbengine.url):
        create_database(dbengine.url)
    else:
        Base.metadata.drop_all(dbengine)

    Base.metadata.create_all(dbengine)
    connection = dbengine.connect()
    session = Session(bind=connection)

    # Populate the database.
    populate_database(session, row_limit)

    # Close the database connection.
    session.close()
    connection.close()
    dbengine.dispose()
