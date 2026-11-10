package com.coffeepeek.admin.ui

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Transient tab-bar gestures: switching tabs or recreating a screen must not replay a scroll. */
internal class MainTabReselections {
    private val searchRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val search = searchRequests.asSharedFlow()

    fun onTabPressed(tab: Navigator.Screen, isSelected: Boolean) {
        if (isSelected && tab == Navigator.Screen.FeedTab) searchRequests.tryEmit(Unit)
    }
}
