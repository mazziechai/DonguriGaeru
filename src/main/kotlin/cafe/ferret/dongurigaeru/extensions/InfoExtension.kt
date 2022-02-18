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

package cafe.ferret.dongurigaeru.extensions

import cafe.ferret.dongurigaeru.database.collections.MatchCollection
import cafe.ferret.dongurigaeru.database.collections.PlayerCollection
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.publicSubCommand
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.common.DiscordTimestampStyle
import dev.kord.common.toMessageFormat
import org.koin.core.component.inject

class InfoExtension : Extension() {
    override val name = "info"

    private val playerCollection: PlayerCollection by inject()
    private val matchCollection: MatchCollection by inject()

    override suspend fun setup() {
        publicSlashCommand {
            name = "info"
            description = "Information commands."

            publicSubCommand(::MatchInfoCommandArguments) {
                name = "match"
                description = "Get information for a match."

                action {
                    // Get the match from the database
                    val match = matchCollection.get(arguments.id.toInt(16))

                    if (match != null) {
                        // Both players are most definitely in the collection since the match exists
                        // so there's no chance of it being null.
                        val player1 = playerCollection.get(match.player1)
                        val player2 = playerCollection.get(match.player2)

                        respond {
                            content = "Match `${match._id.toString(16)}`\n" +
                                    "**${player1?.name}** ${match.score1} - ${match.score2} **${player2?.name}**\n" +
                                    "at ${match.created.toMessageFormat(DiscordTimestampStyle.ShortDateTime)}"
                        }
                    } else {
                        respond {
                            content = "**Error:** That match could not be found."
                        }
                    }
                }
            }
        }
    }

    inner class MatchInfoCommandArguments : Arguments() {
        val id: String by string {
            name = "value"
            description = "The value to search with."

            validate {
                failIf("That is not a valid ID.") {
                    id.toIntOrNull(16) == null
                }
            }
        }
    }
}