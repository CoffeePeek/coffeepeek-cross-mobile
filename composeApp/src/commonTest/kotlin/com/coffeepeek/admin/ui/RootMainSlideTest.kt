package com.coffeepeek.admin.ui

import androidx.compose.animation.core.TargetBasedAnimation
import androidx.compose.animation.core.VectorConverter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RootMainSlideTest {
    @Test
    fun retainedMainSlidesOffscreenAndBackDuringTheRootTransition() {
        for ((start, end) in listOf(0f to -1f, -1f to 0f)) {
            val animation = TargetBasedAnimation(RootMainSlideSpec, Float.VectorConverter, start, end)
            assertEquals(300_000_000L, animation.durationNanos)
            assertEquals(start, animation.getValueFromNanos(0L))
            val middle = animation.getValueFromNanos(animation.durationNanos / 2)
            assertTrue(middle > -1f && middle < 0f)
            for (step in 1..100) {
                val fraction = animation.getValueFromNanos(animation.durationNanos * step / 100)
                assertTrue(fraction >= -1f && fraction <= 0f)
            }
            assertEquals(end, animation.getValueFromNanos(animation.durationNanos))
        }
    }
}
