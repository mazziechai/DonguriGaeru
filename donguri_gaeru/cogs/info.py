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
import logging
from datetime import datetime, timedelta
from typing import Union

import discord
from bot import DonguriGaeruBot
from database import Match, Player
from discord.ext import commands
from pytz import UTC
from sqlalchemy import or_, select
from utils import helpers

from donguri_gaeru import Session


class InfoCog(commands.Cog):
    def __init__(self, bot: DonguriGaeruBot):
        self.bot = bot
        self.log = logging.getLogger("donguri_gaeru")

    @commands.command()
    async def player(
        self, ctx: commands.Context, user: Union[discord.User, str], type: str = None
    ):
        with Session() as session:
            stmt = select(Player)
            if isinstance(user, str):
                player: Player = (
                    session.execute(stmt.where(Player.name.ilike(user)))
                    .scalars()
                    .first()
                )
            else:
                player: Player = (
                    session.execute(stmt.where(Player.discord == user))
                    .scalars()
                    .first()
                )

            if player is None:
                await ctx.send("That player isn't valid!")
                return

            if type is None:
                await ctx.send(
                    f"__{player.name}__\nMatches played: {len(player.matches)}\n"
                    f"Registered {helpers.time(player.created)}\n"
                    f"Add `recent` to the end of this command to get {player.name}'s "
                    f"recent matches or `all` to get all matches."
                )
            elif type == "recent":
                stmt = (
                    select(Match)
                    .where(
                        or_(
                            player.id == Match.playerA_id, player.id == Match.playerB_id
                        )
                    )
                    .order_by(Match.created.desc())
                    .limit(5)
                )
                matches = session.execute(stmt).scalars().all()
                text = ""
                for match in matches:
                    text += helpers.format_match(match)

                await ctx.send(f"__{player.name}__\nRecent matches:{text}")
            elif type == "all":
                await ctx.send("Not implemented yet!")

    @commands.group()
    async def match(self, ctx: commands.Context):
        pass

    @match.command()
    async def id(self, ctx: commands.Context, match_id: int):
        with Session() as session:
            result = session.execute(select(Match).where(Match.id == match_id))
            match: Match = result.scalars().first()

            if match is None:
                await ctx.send("That is not a valid match!")
                return

            await ctx.send(helpers.format_match(match))

    @match.command()
    async def date(self, ctx: commands.Context, start: str, end: str = None):
        with Session() as session:
            try:
                start_date = datetime.strptime(start, "%Y-%m-%d")
                start_date = UTC.localize(start_date)
            except ValueError:
                await ctx.send("Dates must be in YYYY-MM-DD format.")
                return

            stmt = select(Match).where(Match.created >= start_date)

            if end is not None:
                try:
                    end_date = datetime.strptime(end, "%Y-%m-%d")
                    end_date = UTC.localize(end_date)
                except ValueError:
                    await ctx.send("Dates must be in YYYY-MM-DD format!")
                    return

                if start_date > end_date:
                    await ctx.send("Start date cannot come after the end!")
                    return
                if start_date == end_date:
                    await ctx.send("Start and end date cannot be the same!")

                stmt = stmt.where(Match.created <= end_date)

            else:
                stmt = stmt.where(Match.created <= start_date + timedelta(days=1))

            iterations = 0

            while True:
                matches = (
                    session.execute(stmt.limit(5).offset(iterations * 5))
                    .scalars()
                    .fetchmany(5)
                )
                if matches == []:
                    await ctx.send("No matches found!")
                    break

                text = ""
                for match in matches:
                    text += helpers.format_match(match)

                if len(matches) == 5:
                    await ctx.send(
                        "Showing 5 matches.\n"
                        "Type `more` to get more matches. Type `exit` to cancel.\n\n"
                        + text
                    )

                    try:
                        msg: discord.Message = await self.bot.wait_for(
                            "message",
                            check=lambda msg: (
                                (msg.content == "more" or msg.content == "exit")
                                and msg.author == ctx.author
                            ),
                            timeout=15,
                        )

                        if msg.content == "exit":
                            await ctx.send("Exiting!")
                            return
                    except asyncio.TimeoutError:
                        await ctx.send("Exiting!")
                        return

                    iterations += 1

                else:
                    await ctx.send(f"Showing {len(matches)} matches.\n\n" + text)
                    break


def setup(bot: DonguriGaeruBot):
    bot.add_cog(InfoCog(bot))
