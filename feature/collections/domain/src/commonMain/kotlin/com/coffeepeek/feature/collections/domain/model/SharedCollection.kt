package com.coffeepeek.feature.collections.domain.model

data class SharedCollection(val id: String, val title: String, val url: String, val places: List<CollectionPlace>)

data class CollectionSummary(val id: String, val title: String, val url: String, val itemCount: Int)

object CollectionRules {
    const val maxPlaces = 100
    const val maxTitleLength = 100

    fun validTitle(title: String): Boolean = title.trim().length in 1..maxTitleLength && title.none(Char::isISOControl)

    fun validSelection(places: List<CollectionPlace>): Boolean = places.size in 1..maxPlaces &&
        places.all { it.slug.isNotBlank() } && places.map(CollectionPlace::key).distinct().size == places.size
}
