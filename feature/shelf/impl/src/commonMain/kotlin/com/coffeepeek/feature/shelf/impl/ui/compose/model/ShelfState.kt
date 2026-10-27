package com.coffeepeek.feature.shelf.impl.ui.compose.model

import com.coffeepeek.feature.shelf.domain.model.ShelfIssue
import com.coffeepeek.feature.shelf.impl.ui.data.ShelfDay

/**
 * [loaded] is false until the current account's shelf arrives. [openBagId] is the bag sheet, [finishingId] the
 * "Закончилась" sheet and [deletingId] the delete confirmation.
 */
internal data class ShelfState(
    val today: ShelfDay,
    val bags: List<ShelfBagItem> = emptyList(),
    val loaded: Boolean = false,
    val loading: Boolean = false,
    val loadFailed: Boolean = false,
    val openBagId: String? = null,
    val finishingId: String? = null,
    val deletingId: String? = null,
    val message: ShelfMessage? = null,
) {
    val openBag: ShelfBagItem? get() = bags.firstOrNull { it.id == openBagId }
    val finishing: ShelfBagItem? get() = bags.firstOrNull { it.id == finishingId }
    val summary: ShelfSummary get() = bags.summary()
}

/** One snackbar at a time; [Finished] offers to put the bag back at [position]. */
internal sealed interface ShelfMessage {
    data class Finished(val bagId: String, val position: Int) : ShelfMessage
    data object Restored : ShelfMessage
    data object Deleted : ShelfMessage
    data class Failed(val issue: ShelfIssue) : ShelfMessage
}
