package com.coffeepeek.feature.contributions.domain.model

data class CatalogOption(val id: String, val name: String)

/** What the forms choose from. */
data class Catalogs(
    val cities: List<CatalogOption> = emptyList(),
    val beans: List<CatalogOption> = emptyList(),
    val equipment: List<CatalogOption> = emptyList(),
    val roasters: List<CatalogOption> = emptyList(),
    val brewMethods: List<CatalogOption> = emptyList(),
    val tags: List<CatalogOption> = emptyList(),
)

data class DrinkOption(val slug: String, val name: String)

data class ShopPhotoRef(val id: String, val url: String)

data class MenuRow(val slug: String, val availability: Availability, val price: Double?)

/** The shop as it is now, the starting point of a change request. */
data class ShopSnapshot(
    val slug: String,
    val title: String,
    val description: String? = null,
    val contacts: Contacts = Contacts(),
    val photos: List<ShopPhotoRef> = emptyList(),
    val tagIds: List<String> = emptyList(),
    val roasterIds: List<String> = emptyList(),
    val equipmentIds: List<String> = emptyList(),
    val brewMethodIds: List<String> = emptyList(),
    val menu: List<MenuRow> = emptyList(),
    val menuPhotos: List<ShopPhotoRef> = emptyList(),
)
