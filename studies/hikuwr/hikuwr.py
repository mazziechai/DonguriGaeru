import os
import re
import subprocess
from datetime import datetime, timezone

from sqlalchemy import create_engine
from sqlalchemy.orm import Session
from sqlalchemy_utils import create_database, database_exists

from donguri_gaeru.database import Base, Match, Player

# Get the Heroku database access credentials via the CLI.
HIKUWR_SQL_URL = (
    subprocess.check_output(
        "heroku config:get DATABASE_URL -a hiku-worldranking", shell=True
    )
    .decode("utf-8")
    .strip()
    .replace("postgres", "postgresql")
)

# Create a clean PostgreSQL database and connect.
dbengine = create_engine(HIKUWR_SQL_URL)
if not database_exists(dbengine.url):
    create_database(dbengine.url)
else:
    Base.metadata.drop_all(dbengine)

Base.metadata.create_all(dbengine)
connection = dbengine.connect()
session = Session(bind=connection)

# Load the dateset from the text file.
filename = os.path.join(os.path.dirname(__file__), "hikuwr.txt")

with open(filename, "r", encoding="utf-8") as file:
    lines = file.readlines()


def create_new_player(name):
    # If the player isn't already in the database, add them.
    player = session.query(Player).filter_by(name=name).first()
    if player is None:
        player = Player(name=name, created=date)
        session.add(player)
        session.commit()
    return player


# Parse the dataset and populate the database.
DATE_REGEX = r"^(\d\d/\d\d/\d\d)$"
GAME_REGEX = r"^(\S+) (\d+) (\d+) (\S+)$"
DATABASE_ROW_LIMIT = 9950  # 50 rows margin against 10000 limit

for line in lines:
    player_rows = session.query(Player).count()
    match_rows = session.query(Match).count()
    if player_rows + match_rows > DATABASE_ROW_LIMIT:
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
            playerA = create_new_player(playerA)
            playerB = create_new_player(playerB)

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

# Close the database connection.
session.close()
connection.close()
dbengine.dispose()
