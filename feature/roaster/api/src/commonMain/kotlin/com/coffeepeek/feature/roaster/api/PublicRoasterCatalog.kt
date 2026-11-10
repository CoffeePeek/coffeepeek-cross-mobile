package com.coffeepeek.feature.roaster.api

/** Filter choices from the same public catalog/cache used by the roaster screens. */
fun interface PublicRoasterCatalog {
    suspend fun load(): Result<List<PublicRoasterCatalogEntry>>
}

data class PublicRoasterCatalogEntry(val slug: String, val name: String, val photoUrl: String?)
