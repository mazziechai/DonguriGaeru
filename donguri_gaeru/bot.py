# Copyright (C) 2021 mazziechai
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
import re
import traceback
from pathlib import Path

import discord
from database import Base
from discord.ext import commands
from sqlalchemy import create_engine
from sqlalchemy.engine.base import Engine
from sqlalchemy_utils import create_database, database_exists

from config import (
    LOCAL_SQL_DATABASE,
    LOCAL_SQL_HOSTNAME,
    LOCAL_SQL_PASSWORD,
    LOCAL_SQL_USERNAME,
)


class DonguriGaeruBot(commands.Bot):
    # type hints
    engine: Engine

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)

        self.log = logging.getLogger("donguri_gaeru")

        self._setup_database()

        # Loading every cog in the cogs folder
        file_directory = Path(__file__).parent.resolve()

        for cog in (file_directory / "cogs").resolve().glob(r"**/*"):
            relative_path = re.sub(r"(.+)(dongurigaeru.)", "", str(cog))
            self.load_extension(
                re.sub(r"[\\\/]", ".", relative_path).replace(".py", "")
            )

    async def on_ready(self):
        self.log.info(f"Logged in as {self.user.name}#{self.user.discriminator}!")

    async def on_command_error(self, ctx: commands.Context, error):
        if isinstance(error, commands.DisabledCommand):
            await ctx.send("This command is disabled.")

        elif isinstance(error, commands.CommandInvokeError):
            original = error.original

            if not isinstance(original, discord.HTTPException):
                self.log.error(
                    f"""Exception from {ctx.command.qualified_name}:\n
                        {traceback.format_exc()}\n
                        {original.__class__.__name__}: {original}"""
                )

                await ctx.send(
                    f"""Exception from {ctx.command.qualified_name}:\n
                        {traceback.format_exc()}\n
                        {original.__class__.__name__}: {original}"""
                )

    def _setup_database(self):
        username = LOCAL_SQL_USERNAME
        password = LOCAL_SQL_PASSWORD
        hostname = LOCAL_SQL_HOSTNAME
        db_name = LOCAL_SQL_DATABASE

        if password:
            url = f"postgresql://{username}:{password}@{hostname}/{db_name}"
        else:
            url = f"postgresql://{username}@{hostname}/{db_name}"

        if not database_exists(url):
            self.log.info("Database not found, creating it...")
            create_database(url)
            self.engine = create_engine(url, future=True)
            Base.metadata.create_all(self.engine)

        else:
            self.engine = create_engine(url)

        self.log.info("Loaded database.")
