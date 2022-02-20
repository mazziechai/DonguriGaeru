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

package cafe.ferret.dongurigaeru.database.collections

import cafe.ferret.dongurigaeru.database.Collection
import cafe.ferret.dongurigaeru.database.Database
import cafe.ferret.dongurigaeru.database.entities.Player
import dev.kord.common.Locale
import dev.kord.common.entity.Snowflake
import kotlinx.datetime.Clock
import org.bson.conversions.Bson
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.litote.kmongo.eq
import org.litote.kmongo.regex
import kotlin.random.Random

class PlayerCollection : KoinComponent {
    private val database: Database by inject()
    private val col = database.mongo.getCollection<Player>(name)

    suspend fun new(name: String, discord: Snowflake?, locale: Locale?): Player {
        // Generating a new random ID, making sure it doesn't conflict before putting it in
        var random: Int

        do {
            random = Random.nextInt(0x1000, 0xFFFF)
        } while (count(Player::_id eq random) != 0L)

        val player = Player(random, name, discord, Clock.System.now(), locale)
        set(player)

        return player
    }

    suspend fun get(id: Int) = col.findOne(Player::_id eq id)
    suspend fun set(player: Player) = col.save(player)

    suspend fun getByName(name: String) = col.findOne(Player::name regex "(?i)$name")

    suspend fun getByDiscord(discord: Snowflake) = col.findOne(Player::discord eq discord)

    suspend fun count(filter: Bson) = col.countDocuments(filter)

    companion object : Collection("players")
}