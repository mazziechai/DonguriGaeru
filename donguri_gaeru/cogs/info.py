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
from sqlalchemy.sql.expression import Select
from utils import helpers

from donguri_gaeru import Session


class InfoCog(commands.Cog):
    def __init__(self, bot: DonguriGaeruBot):
        self.bot = bot
        self.log = logging.getLogger("donguri_gaeru")

    @commands.command()
    async def recent(self, ctx: commands.Context):
        stmt = select(Match).order_by(Match.created.desc())
        with Session() as session:
            await self.interactive_match_listing(ctx, session, stmt)

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
                    session.execute(stmt.where(Player.discord == user.id))
                    .scalars()
                    .first()
                )

            if player is None:
                await ctx.send("That player isn't registered!")
                return

            if type is None:
                await ctx.send(
                    f"__{player.name}__\nMatches played: {len(player.matches)}\n"
                    f"Registered {helpers.time(player.created)}\n"
                    f"Add `desc` to the end of this command to get {player.name}'s "
                    "most recent matches or `asc` to get matches from the beginning."
                )
                return

            stmt = select(Match).where(
                or_(player.id == Match.playerA_id, player.id == Match.playerB_id)
            )

            if type == "desc":
                stmt = stmt.order_by(Match.created.desc())

            if type == "asc":
                stmt = stmt.order_by(Match.created.asc())

            await self.interactive_match_listing(ctx, session, stmt)

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
        try:
            start_date = datetime.strptime(start, "%Y-%m-%d")
            start_date = UTC.localize(start_date)
        except ValueError:
            await ctx.send("Dates must be in `YYYY-MM-DD` format.")
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

            stmt = stmt.where(Match.created <= end_date + timedelta(days=1))

        else:
            stmt = stmt.where(Match.created <= start_date + timedelta(days=1))

        with Session() as session:
            await self.interactive_match_listing(ctx, session, stmt)

    @match.command()
    async def players(
        self, ctx, player1: Union[discord.User, str], player2: Union[discord.User, str]
    ):
        with Session() as session:
            stmt = select(Player)
            if isinstance(player1, str):
                playerA: Player = (
                    session.execute(stmt.where(Player.name.ilike(player1)))
                    .scalars()
                    .first()
                )
            else:
                playerA: Player = (
                    session.execute(stmt.where(Player.discord == player1.id))
                    .scalars()
                    .first()
                )

            if isinstance(player2, str):
                playerB: Player = (
                    session.execute(stmt.where(Player.name.ilike(player2)))
                    .scalars()
                    .first()
                )
            else:
                playerB: Player = (
                    session.execute(stmt.where(Player.discord == player2.id))
                    .scalars()
                    .first()
                )

            if playerA is None:
                await ctx.send("Player 1 is not registered!")
                return

            if playerB is None:
                await ctx.send("Player 2 is not registered!")
                return

            stmt = (
                select(Match)
                .where(
                    or_(playerA.id == Match.playerA_id, playerA.id == Match.playerA_id)
                )
                .where(
                    or_(playerB.id == Match.playerB_id, playerB.id == Match.playerB_id)
                )
                .order_by(Match.created.desc())
            )

            await self.interactive_match_listing(ctx, session, stmt)

    async def interactive_match_listing(
        self, ctx: commands.Context, session, stmt: Select
    ):
        iterations = 0
        while True:
            matches: list[Match] = (
                session.execute(
                    stmt.where(Match.active).limit(5).offset(iterations * 5)
                )
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
                    "Type `more` to get more matches or `exit` to cancel.\n\n" + text
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
                    # more = back to beginning of loop
                    if msg.content == "exit":
                        await ctx.send("Exiting!")
                        return
                except asyncio.TimeoutError:
                    await ctx.send("Exiting!")
                    return

            else:
                await ctx.send(
                    f"Showing {len(matches)} "
                    f"{'match' if len(matches) == 1 else 'matches'}.\n\n" + text
                )
                return

            iterations += 1


def setup(bot: DonguriGaeruBot):
    bot.add_cog(InfoCog(bot))
