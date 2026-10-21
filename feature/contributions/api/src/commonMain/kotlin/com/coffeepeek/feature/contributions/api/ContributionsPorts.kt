package com.coffeepeek.feature.contributions.api

import com.coffeepeek.feature.contributions.domain.model.Catalogs
import com.coffeepeek.feature.contributions.domain.model.DrinkOption
import com.coffeepeek.feature.contributions.domain.model.ShopSnapshot
import kotlinx.coroutines.flow.Flow

/** Who is signed in. The application backs it with its session. */
interface ContributionsAccount {
    /** The stable key of the active account (its user id, else its token), null for a guest. A replaced account is a new key. */
    val accountKey: Flow<String?>

    /** The key of the account that is active right now; a late answer for a previous key must be dropped. */
    fun currentAccountKey(): String?
}

/** The catalogs and the shop data the forms read; the application owns them (they are shared with Discovery, Shop and Settings). */
interface ContributionCatalogs {
    suspend fun catalogs(): Result<Catalogs>

    /** The shop as it is now, for prefilling a change request. */
    suspend fun shop(slug: String): Result<ShopSnapshot>

    /** The drinks a menu change lists. */
    suspend fun drinks(): Result<List<DrinkOption>>
}
