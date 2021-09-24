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

import discord
from discord.ext import commands

from config import ADMINISTRATORS


def is_administrator():
    """
    Returns true if the author is in the ADMINISTRATORS list in the config.
    """

    def predicate(ctx: commands.Context):
        return ctx.author.id in ADMINISTRATORS

    return commands.check(predicate)


def is_confirmation(ctx: commands.Context, msg: discord.Message):
    """Checks if the reaction being added is on the passed message and is
    one of the correct emojis.

    Parameters
    ----------
    ctx: commands.Context
        The context from the command.
    msg: discord.Message
        The message to check.

    Returns
    ----------
    Callable[[discord.Reaction, discord.User], bool]
        A function that returns true if the reaction was a confirmation.
    """

    def check(reaction: discord.Reaction, user: discord.User):
        if ctx.author == user:
            if reaction.message == msg:
                return str(reaction.emoji) == "✅" or str(reaction.emoji) == "❌"
            return False
        return False

    return check
