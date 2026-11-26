package com.coffeepeek.feature.discovery.impl.ui

import com.coffeepeek.feature.discovery.impl.search.SearchState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Native drag and geometry changes include deceleration; prompt eligibility stays in Kotlin. */
internal class DiscoveryNativeScrolling(private val scope: CoroutineScope) {
    private val mutableActive = MutableStateFlow(false)
    val activity = mutableActive.asStateFlow()
    val active get() = mutableActive.value
    private var dragging = false
    private var settling: Job? = null

    fun record(name: String): Boolean = when (name) {
        "scroll-start" -> {
            dragging = true
            moved()
            true
        }
        "scroll-end" -> {
            dragging = false
            moved()
            true
        }
        "scroll" -> {
            moved()
            true
        }
        else -> false
    }

    private fun moved() {
        mutableActive.value = true
        settling?.cancel()
        if (!dragging) settling = scope.launch {
            delay(350)
            mutableActive.value = false
        }
    }

    fun reset() {
        settling?.cancel()
        dragging = false
        mutableActive.value = false
    }
}

internal fun nativeDiscoveryCanPromptReview(state: SearchState, picker: Boolean, active: Boolean,
    mapExpanded: Boolean, roastersExpanded: Boolean, scrolling: Boolean): Boolean =
    !picker && active && state.isIdleDiscoveryContent() && !mapExpanded && !roastersExpanded && !scrolling
