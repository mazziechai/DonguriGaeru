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
import com.kotlindiscord.kord.extensions.builders.ExtensibleBotBuilder
import com.kotlindiscord.kord.extensions.utils.env
import com.kotlindiscord.kord.extensions.utils.loadModule
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