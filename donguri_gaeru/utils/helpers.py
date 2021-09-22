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
from datetime import datetime, timezone

import discord
from discord.ext import commands
from utils import checks


async def confirmation(ctx: commands.Context, msg: discord.Message):
    await msg.add_reaction("✅")
    await msg.add_reaction("❌")

    try:
        event = await ctx.bot.wait_for(
            "reaction_add", check=checks.is_confirmation(ctx), timeout=30.0
        )  # A tuple of a Reaction and a Member/User.
        if str(event[0].emoji) == "✅":
            return True
    except asyncio.TimeoutError:
        await ctx.send("There was no response.")

    await ctx.send("Cancelling!")
    return False


class NameOrUser(commands.UserConverter):
    async def convert(self, ctx, argument):
        try:
            return await super().convert(ctx, argument)
        except commands.errors.UserNotFound:
            return argument


def time(time: datetime):
    return time.astimezone(tz=timezone.utc).strftime("on %Y-%m-%d at %H:%M %Z")
