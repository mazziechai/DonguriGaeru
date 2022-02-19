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
import cafe.ferret.dongurigaeru.formatMatch
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.converters.ChoiceEnum
import com.kotlindiscord.kord.extensions.commands.application.slash.converters.impl.enumChoice
import com.kotlindiscord.kord.extensions.commands.application.slash.publicSubCommand
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
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
                            content = "Match ${formatMatch(match, player1!!, player2!!)}"
                        }
                    } else {
                        respond {
                            content = "**Error:** Could not find that match"
                        }
                    }
                }
            }

            publicSubCommand(::PlayerInfoCommandArguments) {
                name = "player"
                description = "Get information for a player."

                action {
                    val player = when (arguments.choice) {
                        SearchType.ID -> {
                            playerCollection.get(arguments.value.toInt(16))
                        }
                        SearchType.NAME -> {
                            playerCollection.getByName(arguments.value)
                        }
                    }

                    if (player != null) {
                        val matches = matchCollection.getByPlayer(player)

                        respond {
                            content = "`${player._id.toString(16)}`: **${player.name}**\n" +
                                    "Matches: ${matches.count()}\n" +
                                    "Match listing not implemented yet"
                        }
                    } else {
                        respond {
                            content = "**Error:** Could not find that player"
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

    inner class PlayerInfoCommandArguments : Arguments() {
        val choice: SearchType by enumChoice {
            name = "searchType"
            description = "Search by name or ID."

            typeName = "SearchType"

            choice("name", SearchType.NAME)
            choice("id", SearchType.ID)
        }
        val value: String by string {
            name = "value"
            description = "The ID or name, based on what you selected for the search type."

            validate() {
                failIf("That is not a valid ID.") {
                    choice == SearchType.ID && value.toIntOrNull(16) == null
                }
            }
        }
    }

    enum class SearchType : ChoiceEnum {
        ID {
            override val readableName = "id"
        },
        NAME {
            override val readableName = "name"
        }
    }
}