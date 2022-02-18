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

import cafe.ferret.dongurigaeru.database.collections.PlayerCollection
import cafe.ferret.dongurigaeru.database.entities.Player
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.types.respondEphemeral
import org.koin.core.component.inject
import org.litote.kmongo.eq

class PlayerExtension : Extension() {
    override val name = "player"

    private val playerCollection: PlayerCollection by inject()

    override suspend fun setup() {
        publicSlashCommand(::PlayerRegistrationCommandArguments) {
            name = "register"
            description = "Register yourself into the Donguri Gaeru system."

            action {

                // if discord is already used
                if (playerCollection.count(Player::discord eq user.id) > 0) {
                    respondEphemeral { content = "You are already registered." }
                    return@action
                }

                // Checking if the name is already registered, so we can set the Discord account for that player instead
                val sameNamePlayer = playerCollection.getByName(arguments.name)
                if (sameNamePlayer != null) {
                    if (sameNamePlayer.discord == null) {
                        sameNamePlayer.discord = user.id
                        playerCollection.set(sameNamePlayer)

                        respond {
                            content =
                                "You have been registered as ${arguments.name} with ID ${sameNamePlayer._id.toString(16)}"
                        }
                        return@action
                    } else {
                        // Otherwise, this player is already registered and can't register again
                        respond {
                            content = "You are already registered under another Discord account"
                        }
                        return@action
                    }
                }

                val sameDiscordPlayer = playerCollection.getByDiscord(user.id)
                if (sameDiscordPlayer != null) {
                    respond {
                        content = "You are already registered as ${sameDiscordPlayer.name}"
                    }
                    return@action
                }

                val player = playerCollection.new(arguments.name, user.id)
                respond {
                    content = "You have been registered as ${arguments.name} with ID ${player._id.toString(16)}"
                }
            }
        }
    }

    inner class PlayerRegistrationCommandArguments : Arguments() {
        val name by string {
            name = "name"
            description = "The name you wish to register under. This can be changed in the future."
        }
    }
}