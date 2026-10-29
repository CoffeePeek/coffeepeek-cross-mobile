package com.coffeepeek.feature.shelf.impl.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Hands the bag just added by a scan over to the shelf, which flies it in once and marks it as new for a moment. */
internal class ShelfArrivals {
    private val latest = MutableStateFlow<String?>(null)
    val arriving: StateFlow<String?> = latest.asStateFlow()

    fun arrived(bagId: String) {
        latest.value = bagId
    }

    fun shown(bagId: String) {
        latest.compareAndSet(bagId, null)
    }
}
