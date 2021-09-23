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

import asyncio
from datetime import datetime

import discord
from database import Match
from discord.ext import commands
from utils import checks


async def confirmation(ctx: commands.Context, msg: discord.Message):
    await msg.add_reaction("✅")
    await msg.add_reaction("❌")

    try:
        reaction, user = await ctx.bot.wait_for(
            "reaction_add", check=checks.is_confirmation(ctx, msg), timeout=30.0
        )  # A tuple of a Reaction and a Member/User.
        if str(reaction.emoji) == "✅":
            return True
    except asyncio.TimeoutError:
        await ctx.send("There was no response.")

    await ctx.send("Cancelling!")
    return False


def time(time: datetime):
    return time.strftime("on %Y-%m-%d at %H:%M %Z")


def format_match(match: Match):
    return (
        f"{'**INACTIVE** ' if not match.active else ''}`{match.id}`: "
        f"({match.playerA.name}) {match.scoreA} - "
        f"{match.scoreB} ({match.playerB.name}) "
        f"{time(match.created)}\n"
    )
