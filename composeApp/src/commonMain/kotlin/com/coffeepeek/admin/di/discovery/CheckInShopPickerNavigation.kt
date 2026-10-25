package com.coffeepeek.admin.di.discovery

import com.coffeepeek.admin.feature.checkin.api.CheckInFlowHandoff

/** One selection/close per live destination; disposed or replaced callbacks cannot change navigation. */
internal class CheckInShopPickerNavigation(
    private val isCurrent: () -> Boolean,
    private val open: (String) -> Unit,
    private val dismiss: () -> Unit,
) {
    private var ended = false

    fun select(slug: String) {
        if (ended || !isCurrent() || slug.isBlank()) return
        ended = true
        CheckInFlowHandoff.continueToShop()
        open(slug)
    }

    fun close(back: Boolean) {
        if (ended || !isCurrent()) return
        ended = true
        if (back) CheckInFlowHandoff.cancelFromBack() else CheckInFlowHandoff.take()?.cancel("close")
        dismiss()
    }

    fun dispose() { ended = true }
}
