package com.coffeepeek.feature.discovery.domain.model

/** One choice of a filter list: a roaster, a brew method, a tag, equipment or beans. [photoUrl] is the roaster's logo. */
data class CatalogEntry(val id: String, val name: String, val photoUrl: String? = null)

data class CityEntry(val id: String, val name: String)

/** What the filters choose from. */
data class DiscoveryCatalogs(
    val cities: List<CityEntry> = emptyList(),
    val beans: List<CatalogEntry> = emptyList(),
    val equipment: List<CatalogEntry> = emptyList(),
    val roasters: List<CatalogEntry> = emptyList(),
    val brewMethods: List<CatalogEntry> = emptyList(),
    val tags: List<CatalogEntry> = emptyList(),
)
