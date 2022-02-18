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
import cafe.ferret.dongurigaeru.database.entities.Match
import cafe.ferret.dongurigaeru.database.entities.Player
import kotlinx.datetime.Clock
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.litote.kmongo.eq
import org.litote.kmongo.or
import kotlin.random.Random

class MatchCollection : KoinComponent {
    private val database: Database by inject()
    private val col = database.mongo.getCollection<Match>(name)

    suspend fun new(player1: Player, score1: Int, score2: Int, player2: Player): Match {
        // Generating a new random ID, making sure it doesn't conflict before putting it in
        var random: Int

        do {
            random = Random.nextInt(0x100000, 0xFFFFFF)
        } while (col.countDocuments(Match::_id eq random) != 0L)

        val match = Match(random, player1._id, player2._id, score1, score2, true, Clock.System.now())
        set(match)
        return match
    }

    suspend fun get(id: Int) = col.findOne(Match::_id eq id)

    suspend fun getByPlayer(player: Player) =
        col.find(or(Match::player1 eq player._id, Match::player2 eq player._id)).toList()

    suspend fun set(match: Match) = col.save(match)

    companion object : Collection("matches")
}