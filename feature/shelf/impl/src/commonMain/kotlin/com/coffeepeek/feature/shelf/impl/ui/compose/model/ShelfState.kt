package com.coffeepeek.feature.shelf.impl.ui.compose.model

import com.coffeepeek.feature.shelf.domain.model.ShelfIssue
import com.coffeepeek.feature.shelf.impl.ui.data.ShelfDay

/**
 * [loaded] is false until the current account's shelf arrives. [finishingId] is the "Закончилась" summary of a bag dropped on
 * the zone or finished with TalkBack; a tapped bag opens on its own screen.
 */
internal data class ShelfState(
    val today: ShelfDay,
    val bags: List<ShelfBagItem> = emptyList(),
    val loaded: Boolean = false,
    val loading: Boolean = false,
    val loadFailed: Boolean = false,
    val finishingId: String? = null,
    /** A bag just added by a scan: it flies in and is marked as new until [ShelfAction.ArrivalShown]. */
    val arrivingId: String? = null,
    val message: ShelfMessage? = null,
) {
    val finishing: ShelfBagItem? get() = bags.firstOrNull { it.id == finishingId }
    val summary: ShelfSummary get() = bags.summary()
}

/** One snackbar at a time; [Finished] offers to put the bag back at [position]. */
internal sealed interface ShelfMessage {
    data class Finished(val bagId: String, val position: Int) : ShelfMessage
    data object Added : ShelfMessage
    data object Restored : ShelfMessage
    data object Deleted : ShelfMessage
    data class Failed(val issue: ShelfIssue) : ShelfMessage
}
