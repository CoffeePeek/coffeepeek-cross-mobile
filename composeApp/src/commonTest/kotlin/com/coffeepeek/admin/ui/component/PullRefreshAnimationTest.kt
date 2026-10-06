package com.coffeepeek.admin.ui.component

import androidx.compose.animation.core.TargetBasedAnimation
import androidx.compose.animation.core.VectorConverter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PullRefreshAnimationTest {
    @Test
    fun settlesSmoothlyWithoutOvershootingLoadingOrRestPosition() {
        for ((start, end) in listOf(100f to 72f, 72f to 0f, 0f to 72f)) {
            val animation = TargetBasedAnimation(PullRefreshSettleSpec, Float.VectorConverter, start, end)
            val middle = animation.getValueFromNanos(animation.durationNanos / 4)
            assertTrue(middle > minOf(start, end) && middle < maxOf(start, end))
            var previous = start
            for (step in 1..100) {
                val value = animation.getValueFromNanos(animation.durationNanos * step / 100)
                assertTrue(value >= minOf(start, end) - 0.001f && value <= maxOf(start, end) + 0.001f)
                assertTrue(if (start > end) value <= previous + 0.001f else value >= previous - 0.001f)
                previous = value
            }
            assertEquals(end, animation.getValueFromNanos(animation.durationNanos))
        }
    }
}
