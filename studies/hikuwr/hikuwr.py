import argparse
import os
import re
import subprocess
from datetime import datetime, timezone

from progress.bar import Bar
from sqlalchemy import create_engine, select
from sqlalchemy.orm import Session
from sqlalchemy_utils import create_database, database_exists

from config import DB_HOSTNAME, DB_PASSWORD, DB_USERNAME
from donguri_gaeru.database import Base, Match, Player

parser = argparse.ArgumentParser(
    description=(
        "Create a test database from Hiku's World Ranking dataset. "
        'Default is "--local hikuwr".'
    ),
    formatter_class=argparse.RawTextHelpFormatter,
)
group = parser.add_mutually_exclusive_group()
group.add_argument(
    "-l",
    "--local",
    nargs="?",
    const="hikuwr",
    default="hikuwr",
    metavar="DBNAME",
    help="create database using the local server credentials",
)
group.add_argument(
    "-k",
    "--heroku",
    metavar="APPNAME",
    help="create database using a heroku application",
)


def create_new_player(session, name, date):
    # If the player isn't already in the database, add them.
    player = (
        session.execute(select(Player).where(Player.name == name)).scalars().first()
    )
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

    # Create a progress bar.
    lim = len(lines) if row_limit is None else row_limit
    row_count, prev_row_count, player_row_count, match_row_count = 0, 0, 0, 0
    bar = Bar(
        message="Populating database ...",
        check_tty=False,
        suffix="%(percent)d%%",
        max=lim,
    )

    # Parse the dataset and populate the database.
    for line in lines:
        datematch = re.match(r"^(\d\d/\d\d/\d\d)$", line)
        if datematch:
            date = datetime.strptime(datematch.group(), "%d/%m/%y")
            date = date.replace(tzinfo=timezone.utc)

        gamematch = re.match(r"^(\S+) (\d+) (\d+) (\S+)$", line)
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

                prev_row_count = row_count
                player_row_count = max(playerA.id, playerB.id, player_row_count)
                match_row_count += 1
                row_count = match_row_count + player_row_count

                if row_limit is not None and row_count - prev_row_count > 0:
                    bar.next(row_count - prev_row_count)
                    if row_count >= row_limit:
                        break

            except AssertionError:
                pass

        if row_limit is None:
            bar.next()

    session.commit()
    bar.finish()


def start():
    # Parse the command line arguments.
    args = parser.parse_args()

    if args.heroku is not None:
        # Get the Heroku database access credentials via the CLI.
        cmd = "heroku config:get DATABASE_URL -a " + args.heroku
        url = (
            subprocess.check_output(cmd, shell=True)
            .decode("utf-8")
            .strip()
            .replace("postgres", "postgresql")
        )
        row_limit = 9990  # 10 rows margin against 10000 limit

    else:
        # Get the local database access credentials via the private config.
        sql = "postgresql://{username}:{password}@{hostname}/{database}"
        url = sql.format(
            username=DB_USERNAME,
            password=DB_PASSWORD,
            hostname=DB_HOSTNAME,
            database=args.local,
        )
        row_limit = None

        if not database_exists(url):
            create_database(url)

    # Populate the database.
    engine = create_engine(url, future=True)
    Base.metadata.drop_all(engine)
    Base.metadata.create_all(engine)
    connection = engine.connect()
    session = Session(bind=connection, future=True)
    populate_database(session, row_limit)

    # Close the database connection.
    session.close()
    connection.close()
    engine.dispose()
