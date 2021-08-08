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

import asyncio
import re
from pathlib import Path

import discord
from config import config
from core import database, utils
from discord.ext import commands
from logger import logger

bot = commands.Bot(
    command_prefix=",",
    activity=discord.Activity(type=discord.ActivityType.listening, name=";help"),
)

utils.bot = bot

# Loading every cog in the cogs folder
file_directory = Path(__file__).parent.resolve()

for cog in (file_directory / "cogs").resolve().glob(r"**/*"):
    relative_path = re.sub(r"(.+)(dongurigaeru.)", "", str(cog))
    bot.load_extension(re.sub(r"[\\\/]", ".", relative_path).replace(".py", ""))


# Setting our events
@bot.event
async def on_ready():
    logger.info(f"Logged in as {bot.user.name}#{bot.user.discriminator}!")
    asyncio.create_task(database.backup())


@bot.event
async def on_command_error(ctx, error):
    pass  # stops command not found errors, handle errors in cogs instead


bot.run(config["discord"]["token"], bot=True, reconnect=True)
