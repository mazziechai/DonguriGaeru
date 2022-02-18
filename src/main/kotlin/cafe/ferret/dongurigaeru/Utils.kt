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

import cafe.ferret.dongurigaeru.database.Database
import cafe.ferret.dongurigaeru.database.collections.MatchCollection
import cafe.ferret.dongurigaeru.database.collections.PlayerCollection
import cafe.ferret.dongurigaeru.database.entities.Match
import cafe.ferret.dongurigaeru.database.entities.Player
import com.kotlindiscord.kord.extensions.builders.ExtensibleBotBuilder
import com.kotlindiscord.kord.extensions.utils.env
import com.kotlindiscord.kord.extensions.utils.loadModule
import dev.kord.common.DiscordTimestampStyle
import dev.kord.common.toMessageFormat
import org.koin.dsl.bind

suspend fun ExtensibleBotBuilder.database() {
    val uri = env("DB_URI")
    val db = Database(uri)

    hooks {
        beforeKoinSetup {
            loadModule {
                single { db } bind Database::class
            }

            loadModule {
                single { PlayerCollection() } bind PlayerCollection::class
                single { MatchCollection() } bind MatchCollection::class
            }
        }
    }
}

fun formatMatch(match: Match, player1: Player, player2: Player): String {
    return "`${match._id.toString(16)}`\n" +
            "**${player1.name}** ${match.score1} - ${match.score2} **${player2.name}**\n" +
            "at ${match.created.toMessageFormat(DiscordTimestampStyle.ShortDateTime)}\n" +
            "Active: ${match.active}"
}