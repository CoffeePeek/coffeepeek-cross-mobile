package com.coffeepeek.feature.collections.domain.model

enum class SharedCollectionMode { Snapshot, List }

data class SharedCollection(val id: String, val title: String, val url: String, val places: List<CollectionPlace>,
    val mode: SharedCollectionMode = SharedCollectionMode.Snapshot)

data class CollectionSummary(val id: String, val title: String, val url: String, val itemCount: Int)

object CollectionRules {
    const val maxTitleLength = 100

    fun orderedPlaces(places: List<CollectionPlace>): List<CollectionPlace> =
        places.sortedBy { if (it.kind == CollectionPlaceKind.CoffeeShop) 0 else 1 }

    fun validTitle(title: String): Boolean = title.trim().length in 1..maxTitleLength && title.none(Char::isISOControl)

    fun validSelection(places: List<CollectionPlace>): Boolean = places.isNotEmpty() &&
        places.all { it.slug.isNotBlank() } && places.map(CollectionPlace::key).distinct().size == places.size
}
