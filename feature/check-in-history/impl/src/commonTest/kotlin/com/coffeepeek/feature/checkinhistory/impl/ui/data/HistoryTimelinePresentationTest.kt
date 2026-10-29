package com.coffeepeek.feature.checkinhistory.impl.ui.data

import com.coffeepeek.feature.checkinhistory.domain.model.*
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.model.HistoryTimelineItem
import kotlin.test.*

class HistoryTimelinePresentationTest {
    @Test fun backdatedVisitUsesVisitTimestampWithoutChangingCreationMetadata() {
        val entry = shop().copy(createdAt = "2026-10-23T09:00:00Z", visitedAt = "2026-10-20T08:00:00Z")
        val item = HistoryTimelineItem.Shop(entry, "2026-10-20")
        assertEquals("2026-10-20T08:00:00Z", item.timestamp)
        assertEquals("2026-10-23T09:00:00Z", item.entry.createdAt)
        assertEquals(entry.createdAt, item.copy(entry = entry.copy(visitedAt = " ")).timestamp)
    }

    @Test fun backdatedHomePreparationUsesPreparationTimestampAndKeepsUnknownAccess() {
        val entry = home()
        val item = HistoryTimelineItem.Home(entry)
        assertEquals(entry.preparedAtUtc, item.timestamp)
        assertEquals(entry.createdAtUtc, item.entry.createdAtUtc)
        assertNull(item.entry.visibility)
        assertEquals(HistoryModeration.Unknown, item.entry.moderation)
        assertEquals("18.0", item.entry.recipe.doseGrams)
    }

    @Test fun personalPhotosPreserveExplicitlyMissingThumbnailAndOriginalIdentity() {
        val photos = shop().timelinePhotos()
        assertEquals(listOf(""), photos.previews)
        assertEquals(listOf("detail"), photos.details)
        assertEquals(listOf("original"), photos.originals)
        assertEquals("detail", photos.display(0))
        assertTrue(photos.canOpen(0))
    }

    @Test fun absentShopVariantsFallBackAndHomeBlankVariantsStayAligned() {
        val absent = shop().copy(photoThumbnailUrls = null, photoDetailUrls = null).timelinePhotos()
        assertEquals(listOf("original"), absent.previews)
        assertEquals(listOf("original"), absent.details)
        val photos = home().timelinePhotos()
        assertEquals(listOf(""), photos.previews)
        assertEquals(listOf("detail"), photos.details)
        assertEquals(listOf("original"), photos.originals)
    }

    @Test fun incompleteScoresAreNotZeroFilledAndUnusableDoseValuesAreNotRendered() {
        assertNull(HistoryRating(null, 4, 5).completeTimelineRating())
        assertEquals(HistoryRating(3, 4, 5), HistoryRating(3, 4, 5).completeTimelineRating())
        assertEquals(18.0, home().recipe.timelineDose())
        listOf("NaN", "Infinity", "invalid").forEach { value ->
            assertNull(home().recipe.copy(doseGrams = value).timelineDose())
        }
    }

    private fun shop() = PersonalCheckIn("visit", HistoryAddress("shop", "/shops/shop"), "Shop",
        HistoryAddress("author", "/users/author"), "name", "note", "created", "visited",
        HistoryRating(3, 4, 5), listOf("original"), HistoryDrink("drink", null, "Фильтр", "Filter"),
        HistoryVisibility.Private, HistoryModeration.Rejected, 7, "reason", 4, true,
        "avatar", listOf(""), listOf("detail"), listOf("calendar"))

    private fun home() = HomeHistoryEntry("home", "2026-10-20", "2026-10-20T08:00:00Z", "2026-10-23T09:00:00Z", "note",
        HomeHistoryRecipe("v60", "bean", "name", "18.0", 180, 4), "drink", "slug", null,
        listOf(HomeHistoryPhoto("photo", "original", "", "detail", null)), null, HistoryModeration.Unknown)
}
