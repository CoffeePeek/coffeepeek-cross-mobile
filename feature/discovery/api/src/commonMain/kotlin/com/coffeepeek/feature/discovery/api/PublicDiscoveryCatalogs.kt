package com.coffeepeek.feature.discovery.api

/** Read-only public city and brew-method lists for other owners' forms; the list is read once and a success is kept. */
interface PublicDiscoveryCatalogs {
    suspend fun cities(): Result<List<PublicCatalogOption>>
    suspend fun brewMethods(): Result<List<PublicCatalogOption>>
}

data class PublicCatalogOption(val id: String, val name: String)
