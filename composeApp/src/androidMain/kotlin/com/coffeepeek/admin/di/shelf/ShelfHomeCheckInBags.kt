package com.coffeepeek.admin.di.shelf

import com.coffeepeek.feature.homecheckin.impl.ui.data.HomeCheckInShelfBag
import com.coffeepeek.feature.shelf.domain.repository.ShelfRepository
import kotlinx.coroutines.flow.first

/** The bean picker's "С моей полки": the bags standing on the shelf now, from its snapshot when it has one. */
internal class ShelfHomeCheckInBags(private val shelf: ShelfRepository) {
    suspend fun load(): Result<List<HomeCheckInShelfBag>> {
        val bags = shelf.shelf.first()?.let { Result.success(it) } ?: shelf.refresh()
        return bags.map { list -> list.map { HomeCheckInShelfBag(it.id, it.name.text(), it.roasterName.text()) } }
    }
}

private fun String?.text() = this?.trim()?.takeIf(String::isNotEmpty)
