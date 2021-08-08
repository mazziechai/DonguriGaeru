# Copyright (C) 2021 mazziechai
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

import json
import sys

config = None

try:
    with open("config.json", "r") as file:
        config = json.load(file)

except FileNotFoundError:
    with open("config.json", "w") as file:
        config = {
            "log_level": 20,  # information
            "discord": {
                "token": input("Enter a Discord bot token: "),
                "default_prefix": ";",
                "administrators": [712104395747098656],
            },
            "database": {"path": "database.db3", "backup_interval": 30},  # in minutes
            "glicko2": {
                "standard_rating": 1500,
                "standard_rd": 140,
                "standard_volatility": 0.06,
                "volatility_constraint": 0.75,
                "convergence_tolerance": 0.000001,
                "rating_period": {
                    "length": 72,  # in hours
                    "start": "2021-08-02 0:00:00",  # %Y-%m-%d %H:%M:%S
                },
            },
            "matchmaking": {
                "pending_match_lifetime": 5,  # in minutes
                "in_progress_match_lifetime": 60,  # in minutes
                "cancel_match_lifetime": 5,  # in minutes
                "first_to": 7,
            },
        }

        if (
            input("Would you like to stop start up to configure the settings? (y/N) ")
            == "y"
        ):
            print("Quitting...")
            sys.exit(0)
        else:
            print("Continuing...")

        json.dump(config, file, indent="    ")
