package com.coffeepeek.admin.di.checkinhistory

import com.coffeepeek.feature.checkinhistory.domain.model.*
import com.coffeepeek.domain.model.CheckInVisibility
import com.coffeepeek.legacy.homecheckin.domain.HomeCheckInModeration
import com.coffeepeek.admin.ui.screen.checkins.historyTimestamp
import com.coffeepeek.admin.ui.screen.checkins.toTimelineCheckIn
import kotlin.test.*

class HistoryTimelinePresentationTest {
    @Test fun backdatedVisitUsesVisitTimestampWithoutChangingCreationMetadata() {
        val result = shop().copy(createdAt = "2026-10-23T09:00:00Z", visitedAt = "2026-10-20T08:00:00Z").toTimelineCheckIn()
        assertEquals("2026-10-20T08:00:00Z", result.historyTimestamp())
        assertEquals("2026-10-23T09:00:00Z", result.createdAt)
        assertEquals(result.createdAt, result.copy(visitedAt = " ").historyTimestamp())
    }

    @Test fun backdatedHomePreparationUsesPreparationTimestamp() {
        val entry = HomeHistoryEntry("home", "2026-10-20", "2026-10-20T08:00:00Z", "2026-10-23T09:00:00Z", "note",
            HomeHistoryRecipe("v60", "bean", "name", "18.0", 180, 4), "drink", "slug", null,
            emptyList(), null, HistoryModeration.Unknown)
        val result = entry.toTimelineHome().toTimelineCheckIn()
        assertEquals(entry.preparedAtUtc, result.historyTimestamp())
        assertEquals(entry.createdAtUtc, result.createdAt)
    }

    private fun shop() = PersonalCheckIn("visit", HistoryAddress("shop", "/shops/shop"), "Shop",
        HistoryAddress("author", "/users/author"), "name", "note", "created", "visited",
        HistoryRating(3, 4, 5), listOf("original"), HistoryDrink("drink", null, "Фильтр", "Filter"),
        HistoryVisibility.Private, HistoryModeration.Rejected, 7, "reason", 4, true,
        "avatar", listOf(""), listOf("detail"), listOf("calendar"))

    @Test fun personalCardPreservesIdentityRatingsAccessMetadataAndExplicitlyMissingThumbnail() {
        val result = shop().toTimelineCheckIn()
        assertEquals("shop", result.shopId)
        assertEquals("author", result.authorAddress?.slug)
        assertEquals("avatar", result.authorAvatarUrl)
        assertEquals(4.0, result.rating?.average)
        assertEquals(CheckInVisibility.Private, result.visibility)
        assertEquals(7, result.contentRevision)
        assertEquals("reason", result.rejectionReason)
        assertEquals(4, result.helpfulCount)
        assertTrue(result.isHelpfulByCurrentUser)
        assertEquals(listOf(""), result.photoThumbnailUrls)
        assertEquals(listOf("detail"), result.photoDetailUrls)
    }

    @Test fun legacyPhotosFallbackOnlyWhenVariantsAreAbsentAndIncompleteRatingsAreNotZeroFilled() {
        val result = shop().copy(photoThumbnailUrls = null, rating = HistoryRating(null, 4, 5)).toTimelineCheckIn()
        assertEquals(listOf("original"), result.photoThumbnailUrls)
        assertNull(result.rating)
    }

    @Test fun homeAdapterKeepsUnknownAccessAndBlankVariantsWithoutLosingRecipe() {
        val entry = HomeHistoryEntry("home", "date", "prepared", "created", "note",
            HomeHistoryRecipe("v60", "bean", "name", "18.0", 180, 4), "drink", "slug", null,
            listOf(HomeHistoryPhoto("photo", "original", "", "detail", null)), null, HistoryModeration.Unknown)
        val result = entry.toTimelineHome()
        assertNull(result.visibility)
        assertEquals(HomeCheckInModeration.Unknown, result.moderation)
        assertEquals("18.0", result.doseGrams)
        assertEquals("", result.photos.single().thumbnailUrl)
        assertEquals("detail", result.photos.single().detailUrl)
        assertEquals("", result.photos.single().calendarUrl)
    }
}
