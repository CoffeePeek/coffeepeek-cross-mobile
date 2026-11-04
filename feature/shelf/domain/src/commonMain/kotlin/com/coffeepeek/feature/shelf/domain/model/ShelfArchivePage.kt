package com.coffeepeek.feature.shelf.domain.model

/** Finished bags, newest first. */
data class ShelfArchivePage(val items: List<ShelfBag>, val totalCount: Int, val page: Int, val pageSize: Int) {
    val hasMore: Boolean get() = page.toLong() * pageSize < totalCount
}
