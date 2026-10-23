package com.coffeepeek.admin.ui.screen.checkins

import com.coffeepeek.domain.model.CheckInModerationState
import com.coffeepeek.domain.model.CheckInVisibility
import com.coffeepeek.feature.homecheckin.domain.HomeCheckIn
import com.coffeepeek.feature.homecheckin.domain.HomeCheckInModeration
import com.coffeepeek.feature.homecheckin.domain.HomeCheckInPhoto
import com.coffeepeek.feature.homecheckin.domain.HomeCheckInVisibility
import kotlin.test.*

class HomeTimelinePresentationTest {
    private fun entry() = HomeCheckIn("home", "2026-10-23T08:00:00Z", "2026-10-23",
        "2026-10-22T10:00:00Z", "note", "hario-v60", "bean", "Эфиопия", "18.50", 180, 4,
        "Фильтр", "filter", null, listOf(HomeCheckInPhoto("photo", "original", "thumb", "detail", "calendar")))

    @Test fun preservesRecipeDatesAndAlignedPhotoVariants() {
        val result = entry().toTimelineCheckIn()
        assertTrue(result.isHome)
        assertEquals("2026-10-23T08:00:00Z", result.visitedAt)
        assertEquals("2026-10-22T10:00:00Z", result.createdAt)
        assertEquals(18.5, result.homePreparation?.doseGrams)
        assertEquals("hario-v60", result.homePreparation?.brewMethodCode)
        assertEquals(listOf("original"), result.photoUrls)
        assertEquals(listOf("thumb"), result.photoThumbnailUrls)
        assertEquals(listOf("detail"), result.photoDetailUrls)
        assertEquals(listOf("calendar"), result.photoCalendarUrls)
        assertEquals("filter", result.drinkSlug)
    }

    @Test fun unknownVisibilityNeverBecomesPublicAndAllModerationStatesSurvive() {
        assertEquals(CheckInVisibility.Private, entry().toTimelineCheckIn().visibility)
        assertEquals(CheckInVisibility.Public, entry().copy(visibility = HomeCheckInVisibility.Public).toTimelineCheckIn().visibility)
        HomeCheckInModeration.entries.forEach {
            assertEquals(CheckInModerationState.valueOf(it.name), entry().copy(moderation = it).toTimelineCheckIn().moderationState)
        }
    }

    @Test fun malformedOrNonfiniteDoseDoesNotProduceANonsensicalRecipeLabel() {
        listOf(null, "", "invalid", "NaN", "Infinity").forEach {
            assertNull(entry().copy(doseGrams = it).toTimelineCheckIn().homePreparation?.doseGrams)
        }
    }
}
