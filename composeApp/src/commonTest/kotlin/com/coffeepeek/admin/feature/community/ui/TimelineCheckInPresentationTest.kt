package com.coffeepeek.admin.feature.community.ui

import com.coffeepeek.domain.feature.feed.FeedHomePreparation
import kotlin.test.Test
import kotlin.test.assertEquals

class TimelineCheckInPresentationTest {
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

        assertEquals("Hario V60 · 15.0 г · 260 с · 5/5", recipe.timelineDetails())
        assertEquals("hario-v60", recipe.brewMethodCode)
        assertEquals("future-method · 15.0 г · 260 с · 5/5",
            recipe.copy(brewMethodCode = "future-method").timelineDetails())
    }

    @Test
    fun recipeWithoutOptionalDetailsDoesNotInventADifferentMethod() {
        val recipe = FeedHomePreparation("2026-10-14", null, null, null, null, null)

        assertEquals("", recipe.timelineDetails())
        assertEquals("Другое", recipe.copy(brewMethodCode = "other").timelineDetails())
        assertEquals("Пуровер", recipe.copy(brewMethodCode = "pour_over").timelineDetails())
    }
}
