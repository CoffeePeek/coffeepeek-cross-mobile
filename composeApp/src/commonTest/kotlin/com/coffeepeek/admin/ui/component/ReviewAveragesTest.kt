package com.coffeepeek.admin.ui.component

import com.coffeepeek.domain.model.Review
import com.coffeepeek.domain.model.ReviewRating
import com.coffeepeek.domain.model.CheckIn
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ReviewAveragesTest {
    @Test
    fun averagesCheckInsWithoutDroppingRepeatVisitsAndSkipsUnratedHistory() {
        val visits = listOf(ReviewRating(4, 5, 3), ReviewRating(5, 3, 5), null)
            .mapIndexed { index, rating -> CheckIn("$index", "shop", "Кофейня", "Текст", "", rating = rating) }
        val averages = averageCheckInRatings(visits)!!
        assertEquals(4.0, averages.coffee)
        assertEquals(4.0, averages.service)
        assertEquals(4.5, averages.place)
        assertNull(averageCheckInRatings(emptyList()))
        assertNull(averageCheckInRatings(listOf(visits.last())))
    }
    @Test
    fun averagesEveryReviewAndHandlesEmptyList() {
        assertNull(averageReviewRatings(emptyList()))
        val reviews = listOf(ReviewRating(4, 5, 5), ReviewRating(5, 5, 5), ReviewRating(5, 5, 5))
            .mapIndexed { index, rating -> Review("$index", username = "User", header = "", comment = "", rating = rating, createdAt = "") }
        val averages = averageReviewRatings(reviews)!!
        assertEquals(5.0, averages.coffee)
        assertEquals(5.0, averages.service)
        assertEquals(14.0 / 3, averages.place)
        assertEquals(44.0 / 9, averages.overall, 0.00001)
    }
}
