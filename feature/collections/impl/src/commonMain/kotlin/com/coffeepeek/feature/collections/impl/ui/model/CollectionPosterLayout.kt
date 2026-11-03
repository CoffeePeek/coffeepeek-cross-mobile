package com.coffeepeek.feature.collections.impl.ui.model

import com.coffeepeek.feature.collections.domain.model.CollectionPlace
import com.coffeepeek.feature.collections.domain.model.CollectionPlaceKind
import com.coffeepeek.feature.collections.domain.model.CollectionRules
import kotlin.math.floor

internal data class CollectionPosterCard(val place: CollectionPlace, val left: Float, val top: Float,
    val width: Float, val height: Float)
internal data class CollectionPosterLayout(val contentTop: Float, val pages: List<List<CollectionPosterCard>>)
private data class PosterRow(val places: List<CollectionPlace>, val widths: List<Float>, val height: Float)

internal fun collectionPosterLayout(places: List<CollectionPlace>, format: CollectionPosterFormat,
    contentTop: Float = 178f, roasterWidth: (CollectionPlace) -> Float = { 400f }): CollectionPosterLayout {
    val ordered = CollectionRules.orderedPlaces(places)
    val shops = ordered.filter { it.kind == CollectionPlaceKind.CoffeeShop }
    val roasters = roasterRows(ordered.filter { it.kind == CollectionPlaceKind.Roaster }, roasterWidth)
    val bottom = format.height - 108f
    val height = fittedShopHeight(shops, roasters, bottom, contentTop)
    return CollectionPosterLayout(contentTop, paginateRows(shopRows(shops, height) + roasters, bottom, contentTop))
}

private fun shopRows(shops: List<CollectionPlace>, height: Float): List<PosterRow> =
    shops.map { PosterRow(listOf(it), listOf(1016f), height) }

/** Use the largest height that preserves the packing achievable with at most 15% compression. */
private fun fittedShopHeight(shops: List<CollectionPlace>, roasters: List<PosterRow>, bottom: Float, top: Float): Float {
    if (shops.size <= 4) return 216f
    val compact = paginateRows(shopRows(shops, 183.6f) + roasters, bottom, top)
    val height = compact.mapNotNull { page ->
        val rows = page.distinctBy(CollectionPosterCard::top)
        val count = rows.count { it.place.kind == CollectionPlaceKind.CoffeeShop }
        if (count == 0) null else {
            val fixed = rows.filter { it.place.kind == CollectionPlaceKind.Roaster }.sumOf { it.height.toDouble() }.toFloat()
            (bottom - top - fixed - (rows.size - 1) * 16f) / count
        }
    }.minOrNull() ?: 216f
    return (floor(height * 10f) / 10f).coerceIn(183.6f, 216f)
}

private fun roasterRows(places: List<CollectionPlace>, width: (CollectionPlace) -> Float): List<PosterRow> {
    val rows = mutableListOf(mutableListOf<Pair<CollectionPlace, Float>>())
    places.forEach { place ->
        val preferred = width(place).coerceIn(400f, 1016f)
        val current = rows.last()
        val occupied = current.sumOf { it.second.toDouble() }.toFloat() + current.size * 16f
        if (current.isNotEmpty() && occupied + preferred > 1016f) rows.add(mutableListOf())
        rows.last().add(place to preferred)
    }
    return rows.filter { it.isNotEmpty() }.map(::roasterRow)
}

private fun roasterRow(items: List<Pair<CollectionPlace, Float>>): PosterRow {
    val spare = 1016f - items.sumOf { it.second.toDouble() }.toFloat() - (items.size - 1) * 16f
    val expansion = if (items.size > 1) spare / items.size else 0f
    return PosterRow(items.map { it.first }, items.map { it.second + expansion }, 152f)
}

private fun paginateRows(rows: List<PosterRow>, bottom: Float, contentTop: Float): List<List<CollectionPosterCard>> {
    val pages = mutableListOf<List<CollectionPosterCard>>()
    var cards = mutableListOf<CollectionPosterCard>()
    var top = contentTop
    rows.forEach { row ->
        if (cards.isNotEmpty() && top + row.height > bottom) {
            pages.add(cards)
            cards = mutableListOf()
            top = contentTop
        }
        cards.addAll(rowCards(row, top))
        top += row.height + 16f
    }
    if (cards.isNotEmpty()) pages.add(cards)
    return pages
}

private fun rowCards(row: PosterRow, top: Float): List<CollectionPosterCard> {
    var left = 32f
    return row.places.zip(row.widths).map { (place, width) ->
        CollectionPosterCard(place, left, top, width, row.height).also { left += width + 16f }
    }
}
