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
from utils import helpers

from donguri_gaeru import Session


class InfoCog(commands.Cog):
    def __init__(self, bot: DonguriGaeruBot):
        self.bot = bot
        self.log = logging.getLogger("donguri_gaeru")

    @commands.group()
    async def info(self, ctx: commands.Context):
        pass

    @info.command()
    async def player(
        self, ctx: commands.Context, user: helpers.NameOrUser, type: str = None
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
                await ctx.send("That player isn't registered!")
                return

            if type is None:
                await ctx.send(
                    f"__{player.name}__\nMatches played: {len(player.matches)}\n"
                    f"Registered: {helpers.time(player.created)}\n"
                    f"Type `{ctx.prefix}info player {player.name} recent` to get this "
                    "player's matches."
                )
            elif type == "recent":
                matches = sorted(
                    player.matches, reverse=True, key=lambda match: match.created
                )
                text = ""
                for match in matches:
                    text += (
                        f"\n`{match.id}`: ({match.playerA.name}) {match.scoreA} - "
                        f"{match.scoreB} ({match.playerB.name})"
                        f"{helpers.time(match.created)}"
                    )

                await ctx.send(f"__{player.name}__\nRecent matches:{text}")

    @info.command()
    async def match(self, ctx: commands.Context, match_id: int):
        with Session() as session:
            result = session.execute(select(Match).where(Match.id == match_id))
            match: Match = result.scalars().first()

            if match is None:
                await ctx.send("That is not a valid match!")
                return

            await ctx.send(
                f"\n`{match.id}`: \n{'**[DELETED]** ' if not match.active else ''}"
                f"\n`{match.id}`: ({match.playerA.name}) {match.scoreA} - "
                f"{match.scoreB} ({match.playerB.name})"
                f"{helpers.time(match.created)}"
            )


def setup(bot: DonguriGaeruBot):
    bot.add_cog(InfoCog(bot))
