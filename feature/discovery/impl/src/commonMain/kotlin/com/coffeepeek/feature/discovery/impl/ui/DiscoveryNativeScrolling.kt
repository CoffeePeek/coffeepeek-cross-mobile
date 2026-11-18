package com.coffeepeek.feature.discovery.impl.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.coffeepeek.feature.discovery.impl.search.SearchState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Native drag and geometry changes include deceleration; prompt eligibility stays in Kotlin. */
internal class DiscoveryNativeScrolling(private val scope: CoroutineScope) {
    var active by mutableStateOf(false)
        private set
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
        active = true
        settling?.cancel()
        if (!dragging) settling = scope.launch {
            delay(350)
            active = false
        }
    }

    fun reset() {
        settling?.cancel()
        dragging = false
        active = false
    }
}

internal fun nativeDiscoveryCanPromptReview(state: SearchState, picker: Boolean, active: Boolean,
    mapExpanded: Boolean, roastersExpanded: Boolean, scrolling: Boolean): Boolean =
    !picker && active && state.isIdleDiscoveryContent() && !mapExpanded && !roastersExpanded && !scrolling
