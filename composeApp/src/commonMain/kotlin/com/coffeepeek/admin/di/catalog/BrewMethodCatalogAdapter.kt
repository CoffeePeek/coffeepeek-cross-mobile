package com.coffeepeek.admin.di.catalog

import com.coffeepeek.feature.discovery.api.PublicDiscoveryCatalogs

/** The public brew-method list of the discovery catalogs, read once and kept. */
internal class BrewMethodCatalogAdapter(private val catalogs: PublicDiscoveryCatalogs) : BrewMethodCatalog {
    override suspend fun load(): Result<List<BrewMethodOption>> = catalogs.brewMethods().map { methods ->
        methods.map { BrewMethodOption(it.id, it.name) }
    }
}
