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

package cafe.ferret.dongurigaeru.database.entities

import cafe.ferret.dongurigaeru.database.Entity
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Match(
    override val _id: Int,
    val player1: Int,
    val player2: Int,
    var score1: Int,
    var score2: Int,
    var active: Boolean,
    val created: Instant
) : Entity<Int>
