package com.coffeepeek.feature.contributions.domain.repository

import com.coffeepeek.feature.contributions.domain.model.Catalogs
import com.coffeepeek.feature.contributions.domain.model.DrinkOption
import com.coffeepeek.feature.contributions.domain.model.ShopSnapshot

/** The public reference data the forms read: the catalogs, the shop as it is now, and the menu drinks. */
interface ContributionCatalogSource {
    suspend fun catalogs(): Result<Catalogs>

    /** The shop for prefilling a change request; read fresh every time. */
    suspend fun shop(slug: String): Result<ShopSnapshot>

    suspend fun drinks(): Result<List<DrinkOption>>
}
