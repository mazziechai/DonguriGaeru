import discord
from discord.ext import commands

from config import ADMINISTRATORS


def is_administrator():
    def predicate(ctx: commands.Context):
        return ctx.author.id in ADMINISTRATORS

    return commands.check(predicate)


def is_confirmation(ctx: commands.Context):
    def check(reaction: discord.Reaction, user: discord.User):
        if ctx.author == user:
            return str(reaction.emoji) == "✅" or str(reaction.emoji) == "❌"

        return False

    return check
