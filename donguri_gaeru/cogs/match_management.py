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

import discord
from bot import DonguriGaeruBot
from database import Match, Player
from discord.ext import commands
from sqlalchemy import select
from sqlalchemy.exc import NoResultFound
from utils import checks
from utils.command_utils import confirmation

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
        await ctx.send("Pong!")

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

            msg: discord.Message = await ctx.send(
                "Is this information correct?\n"
                + f"({playerA.name}) {score1} - {score2} ({playerB.name})"
            )

            if await confirmation(ctx, msg):
                match = Match(
                    playerA_id=playerA.id,
                    playerB_id=playerB.id,
                    scoreA=score1,
                    scoreB=score2,
                )
                session.add(match)
                session.commit()

                session.refresh(match)
                await ctx.send(
                    "Submitted match!\n"
                    + f"`{match.id}`\n"
                    + f"({match.playerA.name}) {match.scoreA} - "
                    f"{match.scoreB} ({match.playerB.name})\n" + f"at {match.created}"
                )

    @commands.command()
    @checks.is_administrator()
    async def delete(self, ctx: commands.Context, match_id: int):
        with Session() as session:
            stmt = select(Match).where(Match.id == match_id)
            try:
                match: Match = session.execute(stmt).scalars().one()
            except NoResultFound:
                await ctx.send(f"Match {match_id} doesn't exist!")
                return

            if not match.active:
                await ctx.send("This match has already been deleted!")
                return

            msg: discord.Message = await ctx.send(
                "Are you sure you want to delete this match?\n"
                + f"`{match_id}`\n"
                + f"({match.playerA.name}) {match.scoreA} - "
                f"{match.scoreB} ({match.playerB.name})\n" + f"at {match.created}"
            )

            if await confirmation(ctx, msg):
                match.active = False
                session.commit()
                await ctx.send("Match has been deleted!")

    @commands.command()
    @checks.is_administrator()
    async def fix(self, ctx: commands.Context, match_id: int, score1: int, score2: int):
        with Session() as session:
            stmt = select(Match).where(Match.id == match_id)
            try:
                match: Match = session.execute(stmt).scalars().one()
            except NoResultFound:
                await ctx.send(f"Match {match_id} doesn't exist!")
                return

            if score1 == match.scoreA:
                part1 = f"{score1}"
            else:
                part1 = f"~~{match.scoreA}~~ {score1}"

            if score2 == match.scoreB:
                part2 = f"{score2}"
            else:
                part2 = f"{score2} ~~{match.scoreB}~~"

            if score1 == match.scoreA and score2 == match.scoreB:
                await ctx.send("The new scores are the same as the old scores.")
                return

            msg: discord.Message = await ctx.send(
                "Is this information correct?\n"
                + f"`{match_id}`\n"
                + f"({match.playerA.name}) {part1} - "
                + f"{part2} ({match.playerB.name})\n"
                + f"at {match.created}"
            )

            if await confirmation(ctx, msg):
                match.scoreA = score1
                match.scoreB = score2
                session.commit()
                await ctx.send("Match has been fixed!")


def setup(bot):
    bot.add_cog(MatchCog(bot))
