package com.coffeepeek.feature.settings.domain

import kotlin.time.Duration.Companion.days

/** Installation-local history. A store request may or may not display a review sheet. */
data class AppReviewUsage(
    val firstUsedAt: Long,
    val lastActiveDay: Long,
    val activeDays: Int,
    val lastRequestedAt: Long = 0,
) {
    fun recordUse(now: Long): AppReviewUsage {
        val day = now / Day
        if (day <= lastActiveDay || activeDays >= MinimumActiveDays) return this
        return copy(lastActiveDay = day, activeDays = activeDays + 1)
    }

    fun canRequest(now: Long): Boolean =
        activeDays >= MinimumActiveDays && now - firstUsedAt >= MinimumAge &&
            (lastRequestedAt == 0L || now - lastRequestedAt >= Cooldown)

    companion object {
        fun firstUse(now: Long): AppReviewUsage = AppReviewUsage(now, now / Day, activeDays = 1)

        private const val MinimumActiveDays = 3
        private val Day = 1.days.inWholeSeconds
        private val MinimumAge = 3.days.inWholeSeconds
        private val Cooldown = 30.days.inWholeSeconds
    }
}
