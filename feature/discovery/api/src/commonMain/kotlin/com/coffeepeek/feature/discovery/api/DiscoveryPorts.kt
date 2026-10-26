package com.coffeepeek.feature.discovery.api

import com.coffeepeek.feature.discovery.domain.model.CityEntry
import com.coffeepeek.feature.discovery.domain.model.Coordinates
import com.coffeepeek.feature.discovery.domain.model.ShopSummary
import kotlinx.coroutines.flow.Flow

/** Who is signed in. The application backs it with its session. */
interface DiscoveryAccount {
    /** The stable key of the active account (its user id, else its token), null for a guest. A replaced account is a new key. */
    val accountKey: Flow<String?>

    /** The key of the account that is active right now; a late answer for a previous key must be dropped. */
    fun currentAccountKey(): String?
}

/** The favorite shops of the account; the favorites owner backs it. */
interface DiscoveryFavorites {
    /** The ids of the favorite shops, again each time they change (another screen, another device of the account). */
    val ids: Flow<Set<String>>

    /** The ids right now; search merges them with server favorite flags. */
    suspend fun favoriteIds(): Set<String>

    /** Adds or removes the shop; a failure leaves the favorite as it was. */
    suspend fun setFavorite(shop: ShopSummary, favorite: Boolean): Result<Unit>
}

/** The city the user chose, shared with the settings. */
interface DiscoveryCity {
    /** The saved city id; emits again when another screen changes it. */
    val selected: Flow<String?>

    /** The saved city when it is among [cities], else the first one; the result is saved. Null without cities. */
    suspend fun resolve(cities: List<CityEntry>): String?
}

/** Where the user is. It reads the position only while the permission is granted and never asks for it: the screen asks through its own effect. */
interface DiscoveryLocation {
    /** The last known position while the permission is granted, else null. */
    val current: Flow<Coordinates?>

    /** Asks the platform for a fresh position, e.g. after the permission was granted or the user switched «Рядом» on. */
    fun refresh()
}
