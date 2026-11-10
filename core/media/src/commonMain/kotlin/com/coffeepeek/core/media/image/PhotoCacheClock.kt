package com.coffeepeek.core.media.image

import kotlin.time.Clock
import kotlin.time.TimeSource

internal interface PhotoCacheClock {
    fun wallMillis(): Long
    fun monotonicMillis(): Long
}

internal object SystemPhotoCacheClock : PhotoCacheClock {
    private val origin = TimeSource.Monotonic.markNow()
    override fun wallMillis() = Clock.System.now().toEpochMilliseconds()
    override fun monotonicMillis() = origin.elapsedNow().inWholeMilliseconds
}

internal data class PhotoPermission(val monotonicDeadline: Long, val wallDeadline: Long) {
    fun remaining(clock: PhotoCacheClock): Long = minOf(
        monotonicDeadline - clock.monotonicMillis(), wallDeadline - clock.wallMillis(),
    ).coerceAtLeast(0)
}
