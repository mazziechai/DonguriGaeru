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

import discord
from bot import DonguriGaeruBot
from database import Match, Player
from discord.ext import commands
from sqlalchemy import select
from sqlalchemy.exc import NoResultFound
from utils import checks

from donguri_gaeru import Session


class MatchCog(commands.Cog):
    """
    Commands for submitting, fixing, and deleting matches from the database.
    """

    def __init__(self, bot: DonguriGaeruBot):
        self.bot = bot
        self.log = logging.getLogger("donguri_gaeru")

    @commands.command()
    async def ping(self, ctx: commands.Context):
        await ctx.reply("Pong!")

    @commands.command()
    async def submit(
        self,
        ctx: commands.Context,
        player1: str,
        score1: int,
        score2: int,
        player2: str,
    ):
        with Session() as session:
            stmt = select(Player)
            playerA = (
                session.execute(stmt.where(Player.name.ilike(player1)))
                .scalars()
                .first()
            )
            playerB = (
                session.execute(stmt.where(Player.name.ilike(player2)))
                .scalars()
                .first()
            )

            if playerA is None:
                playerA = Player(name=player1)
                session.add(playerA)
            if playerB is None:
                playerB = Player(name=player2)
                session.add(playerB)

            session.commit()

            session.refresh(playerA)
            session.refresh(playerB)

            match = Match(
                playerA_id=playerA.id,
                playerB_id=playerB.id,
                scoreA=score1,
                scoreB=score2,
            )
            session.add(match)
            session.commit()

            session.refresh(match)
            await ctx.reply(
                "Submitted match!\n"
                + f"`{match.id}`\n"
                + f"({match.playerA.name}) {match.scoreA} - "
                f"{match.scoreB} ({match.playerB.name})\n" + f"at {match.created}"
            )

    @commands.command()
    async def delete(self, ctx: commands.Context, match_id: int):
        with Session() as session:
            stmt = select(Match).where(Match.id == match_id)
            try:
                match: Match = session.execute(stmt).scalars().one()
            except NoResultFound:
                await ctx.reply(f"Match {match_id} doesn't exist!")
                return

            if not match.active:
                await ctx.reply("This match has already been deleted!")
                return

            msg: discord.Message = await ctx.reply(
                "Are you sure you want to delete this match?\n"
                + f"`{match_id}`\n"
                + f"({match.playerA.name}) {match.scoreA} - "
                f"{match.scoreB} ({match.playerB.name})\n" + f"at {match.created}"
            )
            await msg.add_reaction("✅")
            await msg.add_reaction("❌")

            try:
                event = await self.bot.wait_for(
                    "reaction_add", check=checks.is_confirmation(ctx), timeout=30.0
                )  # A tuple of a Reaction and a Member/User.
            except asyncio.TimeoutError:
                await ctx.reply("No response, cancelling!")
                return

            if str(event[0].emoji) == "✅":
                match.active = False
                session.commit()
                await ctx.reply("Match has been deleted!")
            else:
                await ctx.reply("Cancelling!")

    @commands.command()
    async def fix(self, ctx: commands.Context, match_id: int, score1: int, score2: int):
        with Session() as session:
            stmt = select(Match).where(Match.id == match_id)
            try:
                match: Match = session.execute(stmt).scalars().one()
            except NoResultFound:
                await ctx.reply(f"Match {match_id} doesn't exist!")
                return

            msg: discord.Message = await ctx.reply(
                "Is this information correct?\n"
                + f"`{match_id}`\n"
                + f"({match.playerA.name}) {match.scoreA} - "
                f"{match.scoreB} ({match.playerB.name})\n" + f"at {match.created}"
            )

            await msg.add_reaction("✅")
            await msg.add_reaction("❌")

            try:
                event = await self.bot.wait_for(
                    "reaction_add", check=checks.is_confirmation(ctx), timeout=30.0
                )  # A tuple of a Reaction and a Member/User.
            except asyncio.TimeoutError:
                await ctx.reply("No response, cancelling!")
                return

            if str(event[0].emoji) == "✅":
                match.scoreA = score1
                match.scoreB = score2
                session.commit()
                await ctx.reply("Match has been fixed!")
            else:
                await ctx.reply("Cancelling!")


def setup(bot):
    bot.add_cog(MatchCog(bot))
