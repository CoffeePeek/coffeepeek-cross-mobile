package com.coffeepeek.admin.di.profile

import com.coffeepeek.admin.feature.profile.domain.PublicProfileCheckIn
import com.coffeepeek.domain.feature.feed.FeedHomePreparation
import com.coffeepeek.domain.model.CheckIn
import com.coffeepeek.domain.model.ReviewRating
import kotlin.test.*

class PublicProfileCheckInProjectionTest {
    @Test fun richRowsPreserveContentWithoutASecondLookupOrRoundTripToLegacy() {
        val source = CheckIn("entry", "shop", "CoffeePeek", "note", "created", visitedAt = "visited",
            username = "author", authorAvatarUrl = "avatar", rating = ReviewRating(3, 4, 5),
            photoUrls = listOf("original"), photoThumbnailUrls = listOf(""), photoDetailUrls = listOf("detail"),
            customDrinkName = "Мой кофе", helpfulCount = 3, isHelpfulByCurrentUser = true)
        val row = summary(source).toFeatureModel()
        assertEquals("created", row.createdAt)
        assertEquals("visited", row.occurredAt)
        assertEquals("author", row.authorName)
        assertEquals("avatar", row.authorAvatarUrl)
        assertEquals(4.0, row.scores?.average)
        assertEquals("Мой кофе", row.drink?.customName)
        assertEquals(listOf(""), row.photoPreviewUrls)
        assertEquals(listOf("detail"), row.photoDetailUrls)
        assertEquals(3, row.helpfulCount)
        assertTrue(row.isHelpfulByCurrentUser)
    }

    @Test fun homeContentKeepsExplicitKindAndRecipeInsteadOfInferringFromDisplayText() {
        val source = CheckIn("home", "", "", "note", "created", isHome = true,
            homePreparation = FeedHomePreparation("2026-10-26", "hario-v60", "Эфиопия", 18.0, 180, 5))
        val row = summary(source).copy(venue = "Localized venue").toFeatureModel()
        assertTrue(row.isHome)
        assertEquals("Эфиопия", row.preparation?.beanName)
        assertEquals(18.0, row.preparation?.doseGrams)
        assertEquals(180, row.preparation?.brewDurationSeconds)
        assertEquals(5, row.preparation?.tasteScore)
    }

    @Test fun summaryOnlyCompatibilityDoesNotInventRatingsOrLoseDrinkAndHomeContent() {
        val row = PublicProfileCheckIn("summary", "shop", "Shop", "Title", "Капучино", 4.0, "", "date", emptyList())
        val shop = row.toFeatureModel()
        assertNull(shop.scores)
        assertEquals("Капучино", shop.drink?.customName)
        val home = row.copy(venue = "Дома").toFeatureModel()
        assertTrue(home.isHome)
        assertNull(home.drink)
        assertEquals("Title", home.preparation?.beanName)
        assertEquals(4, home.preparation?.tasteScore)
    }

    @Test fun aLongScrollKeepsOldRenderingDataAfterOtherProfilesAreProjected() {
        val rows = (0 until 500).map { summary(CheckIn("entry-$it", "shop", "Shop", "note-$it", "created" )).toFeatureModel() }
        repeat(10) { summary(CheckIn("other-$it", "other", "Other", "", "created")).toFeatureModel() }
        assertEquals("entry-0", rows.first().id)
        assertEquals("note-0", rows.first().note)
        assertEquals("entry-499", rows.last().id)
    }

    private fun summary(source: CheckIn) = PublicProfileCheckIn(source.id, "shop", source.shopName, "Title", null,
        source.rating?.average, source.note, source.visitedAt, source.photoUrls, source)
}
