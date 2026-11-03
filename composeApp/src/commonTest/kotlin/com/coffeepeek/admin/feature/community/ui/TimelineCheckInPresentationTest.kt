package com.coffeepeek.admin.feature.community.ui

import com.coffeepeek.domain.feature.feed.FeedHomePreparation
import kotlin.test.Test
import kotlin.test.assertEquals

class TimelineCheckInPresentationTest {
    private val metricLabels = com.coffeepeek.admin.ui.component.PreparationMetricLabels("%1\$s г", "%1\$s с", "%1\$s мин", "%1\$s ч", "Другое")

    @Test
    fun v60RecipeInFeedKeepsItsRecordedMethodAndDetails() {
        val recipe = FeedHomePreparation(
            preparedOn = "2026-10-14",
            brewMethodCode = "hario-v60",
            beanName = "Руанда Ньякибанда",
            doseGrams = 15.0,
            brewDurationSeconds = 260,
            tasteScore = 5,
        )

        assertEquals("Hario V60 · 15.0 г · 260 с · 5/5", recipe.timelineDetails(metricLabels))
        assertEquals("hario-v60", recipe.brewMethodCode)
        assertEquals("future-method · 15.0 г · 260 с · 5/5",
            recipe.copy(brewMethodCode = "future-method").timelineDetails(metricLabels))
    }

    @Test
    fun recipeWithoutOptionalDetailsDoesNotInventADifferentMethod() {
        val recipe = FeedHomePreparation("2026-10-14", null, null, null, null, null)

        assertEquals("", recipe.timelineDetails(metricLabels))
        assertEquals("Другое", recipe.copy(brewMethodCode = "other").timelineDetails(metricLabels))
        assertEquals("Пуровер", recipe.copy(brewMethodCode = "pour_over").timelineDetails(metricLabels))
    }
    @Test
    fun syntheticOtherUsesPreparedBelarusianCopyWithoutChangingUnknownNames() {
        val translated = metricLabels.copy(otherMethod = "Іншае")
        val recipe = com.coffeepeek.domain.feature.feed.FeedHomePreparation("2026-10-19", "other", null, null, null, null)
        assertEquals("Іншае", recipe.timelineDetails(translated))
        assertEquals("User method", recipe.copy(brewMethodCode = "User method").timelineDetails(translated))
    }
}
