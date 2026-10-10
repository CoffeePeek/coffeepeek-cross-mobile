package com.coffeepeek.core.designsystem.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import kotlin.math.min
import kotlin.time.Duration

/** Internal UI gesture policy, not request scheduling or business retry logic. */
internal class PullRefreshGestureState(
    val thresholdPx: Float,
    private val cooldown: Duration,
    private val elapsed: () -> Duration,
) {
    init {
        require(thresholdPx.isFinite() && thresholdPx > 0f)
        require(cooldown.isFinite() && cooldown >= Duration.ZERO)
    }

    var offset by mutableFloatStateOf(0f)
        private set
    private var lastRequest: Duration? = null

    fun pull(delta: Float, atTop: Boolean, allowed: Boolean): Float {
        if (!allowed || !atTop) {
            reset()
            return 0f
        }
        if (!delta.isFinite() || delta <= 0f) return 0f
        val next = (offset + delta * 0.5f).coerceAtMost(thresholdPx * 1.4f)
        val consumed = (next - offset) / 0.5f
        offset = next
        return consumed
    }

    fun retract(delta: Float): Float {
        if (!delta.isFinite() || delta >= 0f) return 0f
        val consumed = min(-delta, offset / 0.5f)
        offset = (offset - consumed * 0.5f).coerceAtLeast(0f)
        return -consumed
    }

    fun release(allowed: Boolean, atTop: Boolean): Boolean {
        val reachedThreshold = offset >= thresholdPx
        reset()
        return reachedThreshold && request(allowed && atTop)
    }

    /** Both accessible actions and gesture requests share the same cooldown. */
    fun request(allowed: Boolean): Boolean {
        if (!allowed) return false
        val now = elapsed()
        if (lastRequest?.let { now - it < cooldown } == true) return false
        lastRequest = now
        return true
    }

    fun reset() { offset = 0f }
}
