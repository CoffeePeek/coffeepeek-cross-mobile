package com.coffeepeek.admin.feature.catalog.api

/** Public brewing methods shared by shop and home preparation forms. */
internal data class BrewMethodOption(val slug: String, val name: String)

internal fun interface BrewMethodCatalog {
    suspend fun load(): Result<List<BrewMethodOption>>
}
