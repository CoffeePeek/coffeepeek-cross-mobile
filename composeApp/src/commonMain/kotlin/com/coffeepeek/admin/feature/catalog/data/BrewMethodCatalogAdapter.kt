package com.coffeepeek.admin.feature.catalog.data

import com.coffeepeek.admin.feature.catalog.api.BrewMethodCatalog
import com.coffeepeek.admin.feature.catalog.api.BrewMethodOption
import com.coffeepeek.domain.repository.ShopRepository

/** Reuses the shop catalog and its cache during the incremental feature migration. */
internal class BrewMethodCatalogAdapter(private val shops: ShopRepository) : BrewMethodCatalog {
    override suspend fun load(): Result<List<BrewMethodOption>> = shops.getCatalogs().map { catalogs ->
        catalogs.brewMethods.map { BrewMethodOption(it.slug, it.name) }
    }
}
