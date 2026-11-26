package com.coffeepeek.core.media.image

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The covered screen keeps rendering, but speculative image work yields to the viewer. */
object FullScreenPhotoPresence {
    private var viewers by mutableIntStateOf(0)
    private val mutableActive = MutableStateFlow(false)
    val activeFlow = mutableActive.asStateFlow()
    val active: Boolean get() = viewers > 0
    fun enter() {
        viewers++
        mutableActive.value = active
    }
    fun leave() {
        viewers = (viewers - 1).coerceAtLeast(0)
        mutableActive.value = active
    }
}
