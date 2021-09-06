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

from bot import DonguriGaeruBot
from database import Match, Player
from discord.ext import commands
from sqlalchemy import select

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
                session.execute(stmt.where(Player.name == player1)).scalars().first()
            )
            playerB = (
                session.execute(stmt.where(Player.name == player2)).scalars().first()
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

            await ctx.send(f"Submitted match to database.\n`{match}`")


def setup(bot):
    bot.add_cog(MatchCog(bot))
