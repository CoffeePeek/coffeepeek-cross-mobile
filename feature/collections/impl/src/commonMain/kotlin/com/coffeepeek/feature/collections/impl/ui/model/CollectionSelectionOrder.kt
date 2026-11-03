package com.coffeepeek.feature.collections.impl.ui.model

import com.coffeepeek.feature.collections.domain.model.CollectionPlace

/** Move within a section without modifying favorite membership or selected keys. */
internal fun moveCollectionPlace(places: List<CollectionPlace>, key: String, targetKey: String, after: Boolean): List<CollectionPlace> {
    val source = places.firstOrNull { it.key == key } ?: return places
    val target = places.firstOrNull { it.key == targetKey } ?: return places
    if (key == targetKey || source.kind != target.kind) return places
    val reordered = places.filterNot { it.key == key }.toMutableList()
    val index = reordered.indexOfFirst { it.key == targetKey } + if (after) 1 else 0
    reordered.add(index, source)
    return reordered
}
