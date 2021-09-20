# Copyright (C) 2021 mazziechai, amosborne, and contributors
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU Affero General Public License as
# published by the Free Software Foundation, either version 3 of the
# License, or (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Affero General Public License for more details.
#
# You should have received a copy of the GNU Affero General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.

import logging
from logging.handlers import RotatingFileHandler

import discord
from bot import DonguriGaeruBot
from database import Base
from sqlalchemy.engine import create_engine

from config import (
    DB_HOSTNAME,
    DB_NAME,
    DB_PASSWORD,
    DB_USERNAME,
    LOG_LEVEL,
    PREFIX,
    TOKEN,
)
from donguri_gaeru import Session

# Setting up logging
logging.getLogger("discord").setLevel(logging.INFO)

root_logger = logging.getLogger()
root_logger.setLevel(LOG_LEVEL)

file_handler = RotatingFileHandler(
    filename="bot.log",
    encoding="utf-8",
    mode="w",
    maxBytes=32 * 1024 * 1024,
    backupCount=5,
)
datefmt = "%Y-%m-%d %H:%M:%S"
fmt = logging.Formatter(
    "[{asctime}] [{levelname}] {name}: {message}", datefmt, style="{"
)

file_handler.setFormatter(fmt)

stream_handler = logging.StreamHandler()
stream_handler.setFormatter(fmt)

root_logger.addHandler(file_handler)
root_logger.addHandler(stream_handler)

# Setting up the database
log = logging.getLogger("donguri_gaeru")

username = DB_USERNAME
password = DB_PASSWORD
hostname = DB_HOSTNAME
db_name = DB_NAME

if password:
    sql = "postgresql://{username}:{password}@{hostname}/{db_name}"
else:
    sql = "postgresql://{username}@{hostname}/{db_name}"
url = sql.format(
    username=DB_USERNAME, password=DB_PASSWORD, hostname=DB_HOSTNAME, db_name=DB_NAME
)

engine = create_engine(url, future=True)
Base.metadata.create_all(engine)

Session.configure(bind=engine)

log.info("Loaded database.")

# Starting the bot now
bot = DonguriGaeruBot(
    command_prefix=PREFIX,
    activity=discord.Activity(type=discord.ActivityType.listening, name="/help"),
)

log.info("Logging in...")

if not TOKEN:
    raise Exception("A token was not specified in the config!")

bot.run(TOKEN, reconnect=True)
