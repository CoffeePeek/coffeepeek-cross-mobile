package com.coffeepeek.feature.collections.impl.ui.model

import com.coffeepeek.feature.collections.domain.model.CollectionPlace
import com.coffeepeek.feature.collections.domain.model.CollectionPlaceKind
import com.coffeepeek.feature.collections.domain.model.CollectionRules

internal data class CollectionPosterCard(val place: CollectionPlace, val top: Float, val height: Float)

internal fun collectionPosterPage(places: List<CollectionPlace>, format: CollectionPosterFormat, page: Int): List<CollectionPlace> =
    CollectionRules.orderedPlaces(places).chunked(format.perPage).getOrElse(page) { emptyList() }

internal fun collectionPosterCards(places: List<CollectionPlace>, format: CollectionPosterFormat): List<CollectionPosterCard> {
    val heights = places.map { if (it.kind == CollectionPlaceKind.CoffeeShop) 232f else 164f }
    val gap = 16f
    val available = format.height - 260f - 80f - gap * (places.size - 1).coerceAtLeast(0)
    val scale = minOf(1f, available / heights.sum().coerceAtLeast(1f))
    var top = 260f
    return places.mapIndexed { index, place ->
        val height = heights[index] * scale
        CollectionPosterCard(place, top, height).also { top += height + gap }
    }
}
