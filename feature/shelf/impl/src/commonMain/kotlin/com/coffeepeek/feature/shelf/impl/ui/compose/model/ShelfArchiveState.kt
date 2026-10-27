package com.coffeepeek.feature.shelf.impl.ui.compose.model

import com.coffeepeek.feature.shelf.domain.model.ShelfIssue
import com.coffeepeek.feature.shelf.impl.ui.data.ShelfDay

/** Finished bags newest first; [page] is the last loaded page (0 before the first answer). */
internal data class ShelfArchiveState(
    val today: ShelfDay,
    val query: String = "",
    val items: List<ShelfBagItem> = emptyList(),
    val page: Int = 0,
    val hasMore: Boolean = false,
    val loading: Boolean = true,
    val loadingMore: Boolean = false,
    val loadFailed: Boolean = false,
    val loadMoreFailed: Boolean = false,
    val openBagId: String? = null,
    val deletingId: String? = null,
    val message: ShelfArchiveMessage? = null,
) {
    val openBag: ShelfBagItem? get() = items.firstOrNull { it.id == openBagId }

    /** Month groups in loading order; a bag without a finish date joins the group before it. */
    val sections: List<ShelfArchiveSection>
        get() = items.fold(mutableListOf<ShelfArchiveSection>()) { sections, item ->
            val month = item.finishedOn?.let { it.year to it.month } ?: sections.lastOrNull()?.let { it.year to it.month }
            val last = sections.lastOrNull()
            if (last != null && month == last.year to last.month) sections[sections.lastIndex] = last.copy(items = last.items + item)
            else sections += ShelfArchiveSection(month?.first ?: today.year, month?.second ?: today.month, listOf(item))
            sections
        }
}

internal data class ShelfArchiveSection(val year: Int, val month: Int, val items: List<ShelfBagItem>)

internal sealed interface ShelfArchiveMessage {
    data object Restored : ShelfArchiveMessage
    data object Deleted : ShelfArchiveMessage
    data class Failed(val issue: ShelfIssue) : ShelfArchiveMessage
}
