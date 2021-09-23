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
from typing import Union

import discord
from bot import DonguriGaeruBot
from database import Match, Player
from discord.ext import commands
from sqlalchemy import select, update
from sqlalchemy.exc import IntegrityError, NoResultFound
from utils import helpers

from donguri_gaeru import Session


class PlayerMatchCog(commands.Cog):
    """
    Commands for submitting, fixing, and deleting matches from the database.
    """

    def __init__(self, bot: DonguriGaeruBot):
        self.bot = bot
        self.log = logging.getLogger("donguri_gaeru")

    @commands.command()
    async def submit(
        self,
        ctx: commands.Context,
        score1: int,
        score2: int,
        player2: Union[discord.User, str],
    ):
        with Session() as session:
            stmt = select(Player)
            playerA = (
                session.execute(stmt.where(Player.discord == ctx.author.id))
                .scalars()
                .first()
            )

            if playerA is None:
                await ctx.send(
                    "You're not registered! "
                    f"Run `{ctx.prefix}register <name>` to register."
                )
                return

            if isinstance(player2, str):
                playerB: Player = (
                    session.execute(stmt.where(Player.name.ilike(player2)))
                    .scalars()
                    .first()
                )

                if playerB is None:
                    playerB = Player(name=player2)
                    session.add(playerB)

            else:
                playerB: Player = (
                    session.execute(stmt.where(Player.discord == player2.id))
                    .scalars()
                    .first()
                )

                if playerB is None:
                    await ctx.send(
                        "Player 2 isn't registered! "
                        f"Run `{ctx.prefix}register <name>` to register."
                    )
                    return

            msg: discord.Message = await ctx.send(
                "Is this information correct?\n\n"
                f"({playerA.name}) {score1} - {score2} ({playerB.name})"
            )

            if await helpers.confirmation(ctx, msg):
                session.commit()

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

                usr = f"{ctx.author.id} ({ctx.author.name}#{ctx.author.discriminator})"
                self.log.info(f"{usr} submitted match:\n{helpers.format_match(match)}")
                await ctx.send(f"Submitted match!\n\n{helpers.format_match(match)}")
            else:
                session.rollback()


class RegistrationCog(commands.Cog):
    """Commands for linking a Discord account to a Player."""

    def __init__(self, bot: DonguriGaeruBot):
        self.bot = bot
        self.log = logging.getLogger("donguri_gaeru")

    @commands.command()
    async def register(self, ctx: commands.Context, name: str):
        with Session() as session:
            stmt = select(Player).where(Player.discord == ctx.author.id)
            result = session.execute(stmt).scalars().first()

            if result is not None:
                await ctx.send(f"You're already registered as `{result.name}`!")
                return

            stmt = select(Player).where(Player.name.ilike(name))
            try:
                player: Player = session.execute(stmt).scalars().one()

            except NoResultFound:
                player = Player(name=name)

            msg = await ctx.send(
                "Is this information correct?\n\n" + f"Registering with name `{name}`"
            )

            if await helpers.confirmation(ctx, msg):
                player.discord = ctx.author.id
                session.add(player)
                session.commit()

                usr = f"{ctx.author.id} ({ctx.author.name}#{ctx.author.discriminator})"
                self.log.info(f"{usr} registered as {name}!")
                await ctx.send(f"Successfully registered as `{name}`!")


class PlayerSettingsCog(commands.Cog):
    """Commands for changing player information and settings."""

    def __init__(self, bot: DonguriGaeruBot):
        self.bot = bot
        self.log = logging.getLogger("donguri_gaeru")

    @commands.command()
    async def setname(self, ctx: commands.Context, name: str):
        with Session() as session:
            stmt = select(Player).where(Player.discord == ctx.author.id)
            try:
                player: Player = session.execute(stmt).scalars().one()
            except NoResultFound:
                await ctx.send(
                    "You're not registered!\nRegister with "
                    + f"`{ctx.prefix}register <name>`"
                )
                return

            if name == player.name:
                await ctx.send("That's already your name!")
                return

            old_name = player.name

            stmt = update(Player).where(Player.discord == ctx.author.id)

            try:
                session.execute(stmt.values(name=name))
            except IntegrityError:
                session.rollback()
                await ctx.send("This name is already taken!")
                return
            else:
                session.commit()

            self.log.info(f"{old_name} changed their name to {name}!")
            await ctx.send(f"Your name was changed to {name}! Hello, {name}!")


def setup(bot: DonguriGaeruBot):
    bot.add_cog(PlayerMatchCog(bot))
    bot.add_cog(RegistrationCog(bot))
    bot.add_cog(PlayerSettingsCog(bot))
