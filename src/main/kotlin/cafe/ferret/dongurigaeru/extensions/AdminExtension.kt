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
import com.kotlindiscord.kord.extensions.checks.hasPermission
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.publicSubCommand
import com.kotlindiscord.kord.extensions.commands.converters.impl.int
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.common.DiscordTimestampStyle
import dev.kord.common.entity.Permission
import dev.kord.common.toMessageFormat
import org.koin.core.component.inject

class AdminExtension : Extension() {
    override val name = "admin"

    private val playerCollection: PlayerCollection by inject()
    private val matchCollection: MatchCollection by inject()

    override suspend fun setup() {
        publicSlashCommand {
            name = "admin"
            description = "Administrator commands."

            publicSubCommand(::AdminSubmitCommandArguments) {
                name = "submit"
                description = "Submits a match between two players."

                check {
                    hasPermission(Permission.Administrator)
                }

                action {
                    // Get player 1 and player 2 from the database
                    var player1 = playerCollection.getByName(arguments.player1)
                    var player2 = playerCollection.getByName(arguments.player2)

                    // If either are null, create a new player and save it to the database
                    if (player1 == null) {
                        player1 = playerCollection.new(arguments.player1, null)
                    }
                    if (player2 == null) {
                        player2 = playerCollection.new(arguments.player2, null)
                    }

                    val match = matchCollection.new(player1, arguments.score1, arguments.score2, player2)

                    respond {
                        content = "Reported match `${match._id.toString(16)}`\n" +
                                "**${player1.name}** ${arguments.score1} - ${arguments.score2} **${player2.name}**\n" +
                                "at ${match.created.toMessageFormat(DiscordTimestampStyle.ShortDateTime)}"

                    }
                }
            }

            publicSubCommand(::EnableDisableMatchCommandArguments) {
                name = "enable"
                description = "Enable a match if it was disabled."

                check {
                    hasPermission(Permission.Administrator)
                }

                action {
                    val match = matchCollection.get(arguments.id.toInt(16))

                    if (match == null) {
                        respond {
                            content = "**Error:** That match could not be found."
                        }
                    } else if (match.active) {
                        respond {
                            content = "**Error:** That match is already enabled."
                        }
                    } else {
                        match.active = true
                        matchCollection.set(match)
                        respond {
                            content = "Enabled match `${match._id.toString(16)}`"
                        }
                    }
                }
            }

            publicSubCommand(::EnableDisableMatchCommandArguments) {
                name = "disable"
                description = "Disable a match, effectively deleting it."

                check {
                    hasPermission(Permission.Administrator)
                }

                action {
                    val match = matchCollection.get(arguments.id.toInt(16))

                    if (match == null) {
                        respond {
                            content = "**Error:** That match could not be found."
                        }
                    } else if (!match.active) {
                        respond {
                            content = "**Error:** That match is already disabled."
                        }
                    } else {
                        match.active = false
                        matchCollection.set(match)
                        respond {
                            content = "Disabled match `${match._id.toString(16)}`"
                        }
                    }
                }
            }

        }
    }

    inner class AdminSubmitCommandArguments : Arguments() {
        val player1 by string {
            name = "player1"
            description = "Player 1's name."
        }
        val score1 by int {
            name = "score1"
            description = "The score of player 1."
        }
        val score2 by int {
            name = "score2"
            description = "The score of player 2."
        }
        val player2 by string {
            name = "player2"
            description = "Player 2's name."
        }
    }

    inner class EnableDisableMatchCommandArguments : Arguments() {
        val id by string {
            name = "id"
            description = "The ID of the match."

            validate {
                failIf("That is not a valid ID.") {
                    value.toIntOrNull(16) == null
                }
            }
        }
    }
}