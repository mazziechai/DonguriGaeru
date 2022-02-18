/*
 * Copyright (C) 2022 mazziechai
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package cafe.ferret.dongurigaeru

import cafe.ferret.dongurigaeru.extensions.AdminExtension
import cafe.ferret.dongurigaeru.extensions.InfoExtension
import cafe.ferret.dongurigaeru.extensions.PlayerExtension
import cafe.ferret.dongurigaeru.extensions.UtilityExtension
import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.utils.env
import dev.kord.common.entity.Snowflake

val TEST_SERVER_ID = Snowflake(
    env("TEST_SERVER").toLong()  // Get the test server ID from the env vars or a .env file
)

private val TOKEN = env("TOKEN")   // Get the bot' token from the env vars or a .env file

suspend fun main() {
    val bot = ExtensibleBot(TOKEN) {
        database()
        applicationCommands {
            defaultGuild(TEST_SERVER_ID)
        }
        extensions {
            add(::UtilityExtension)
            add(::AdminExtension)
            add(::PlayerExtension)
            add(::InfoExtension)
        }
    }

    bot.start()
}
