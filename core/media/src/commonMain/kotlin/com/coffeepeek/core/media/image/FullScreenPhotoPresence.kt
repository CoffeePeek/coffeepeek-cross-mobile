package com.coffeepeek.core.media.image

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

/** The covered screen keeps rendering, but speculative image work yields to the viewer. */
object FullScreenPhotoPresence {
    private var viewers by mutableIntStateOf(0)
    val active: Boolean get() = viewers > 0
    fun enter() { viewers++ }
    fun leave() { viewers = (viewers - 1).coerceAtLeast(0) }
}
