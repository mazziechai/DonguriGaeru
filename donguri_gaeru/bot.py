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
import re
import traceback
from pathlib import Path

from discord.ext import commands
from discord.utils import escape_markdown


class DonguriGaeruBot(commands.Bot):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)

        self.log = logging.getLogger("donguri_gaeru")

        # Loading every cog in the cogs folder
        file_directory = Path(__file__).parent.resolve()

        for cog in (file_directory / "cogs").resolve().glob(r"**/*"):
            relative_path = re.sub(r"(.+)(donguri_gaeru.)", "", str(cog))
            if relative_path.endswith(".py"):
                self.load_extension(
                    re.sub(r"[\\\/]", ".", relative_path).replace(".py", "")
                )

    async def on_ready(self):
        self.log.info(f"Logged in as {self.user.name}#{self.user.discriminator}!")

    async def on_command_error(self, ctx: commands.Context, error):
        if isinstance(error, commands.DisabledCommand):
            await ctx.send("This command is disabled.")

        elif isinstance(error, commands.CommandInvokeError):
            self.log.exception(
                f"""Exception from {ctx.command.qualified_name}!\n
                {"".join(traceback.format_exception(
                    type(error), error, error.__traceback__
                ))}"""
            )

            await ctx.send(
                f"""Exception from {ctx.command.qualified_name}!\n
                ```{escape_markdown("".join(
                        traceback.format_exception(
                            type(error), error, error.__traceback__
                        )
                    )
                )}```"""
            )
