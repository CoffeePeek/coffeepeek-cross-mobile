package com.coffeepeek.feature.profile.data

import com.coffeepeek.feature.community.api.PublicAuthorPost
import com.coffeepeek.feature.community.api.PublicAuthorPostPage
import com.coffeepeek.feature.community.api.PublicAuthorPosts
import com.coffeepeek.feature.community.api.PublicPostDrink
import com.coffeepeek.feature.community.api.PublicPostPreparation
import com.coffeepeek.feature.community.api.PublicPostScores
import com.coffeepeek.feature.profile.data.repository.createPublicProfileCheckIns
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PublicProfileCheckInsTest {
    @Test fun onlyExactAuthorPublicApprovedRowsSurviveWithPublicationOrderAndCursor() = runTest {
        val source = source { slug, cursor ->
            assertEquals("anna", slug)
            assertEquals("opaque", cursor)
            PublicAuthorPostPage(listOf(post("old"), post("private").copy(isPublicApproved = false),
                post("other").copy(authorSlug = "other"), post("unknown").copy(authorSlug = null), post("new")), "next")
        }
        val page = createPublicProfileCheckIns(source).page("anna", "opaque").getOrThrow()
        assertEquals(listOf("old", "new"), page.items.map { it.id })
        assertEquals("next", page.nextCursor)
    }

    @Test fun fullyFilteredPageStillKeepsCursorInsteadOfEndingPagination() = runTest {
        val source = source { _, _ -> PublicAuthorPostPage(listOf(post().copy(isPublicApproved = false)), "next") }
        val page = createPublicProfileCheckIns(source).page("anna").getOrThrow()
        assertTrue(page.items.isEmpty())
        assertEquals("next", page.nextCursor)
    }

    @Test fun completeRowsRetainDatesAuthorRatingsDrinkVotesAndAlignedPhotoSources() = runTest {
        val row = load(post())
        assertEquals("created", row.createdAt)
        assertEquals("visited", row.occurredAt)
        assertEquals("Anna", row.authorName)
        assertEquals("avatar", row.authorAvatarUrl)
        assertEquals(4.0, row.scores?.average)
        assertEquals("Мой кофе", row.drink?.customName)
        assertEquals("Coffee", row.drink?.nameEn)
        assertEquals(listOf("original"), row.photoUrls)
        assertEquals(listOf(""), row.photoPreviewUrls)
        assertEquals(listOf("detail"), row.photoDetailUrls)
        assertEquals(3, row.helpfulCount)
        assertTrue(row.isHelpfulByCurrentUser)
        assertFalse(row.isHome)
    }

    @Test fun homeKindIsExplicitAndPreparedDateAndRecipeSurvive() = runTest {
        val row = load(post().copy(isHome = true,
            home = PublicPostPreparation("prepared", "hario-v60", "Эфиопия", 18.0, 180, 5)))
        assertTrue(row.isHome)
        assertNull(row.coffeeShopSlug)
        assertEquals("Дома", row.venue)
        assertEquals("prepared", row.occurredAt)
        assertEquals("created", row.createdAt)
        assertEquals("Эфиопия", row.title)
        assertEquals("hario-v60 · 18.0 г · 180 с", row.details)
        assertEquals(5.0, row.rating)
        assertEquals(18.0, row.preparation?.doseGrams)
        assertEquals(180, row.preparation?.brewDurationSeconds)
    }

    @Test fun missingRecipeScoresAndDrinkNeverInventRatingsOrHomeKind() = runTest {
        val row = load(post().copy(shopName = "Дома", scores = null, drink = null, visitedAt = ""))
        assertFalse(row.isHome)
        assertNull(row.preparation)
        assertNull(row.scores)
        assertNull(row.rating)
        assertEquals("created", row.occurredAt)
        val home = load(post().copy(isHome = true, home = null))
        assertNull(home.rating)
        assertEquals("visited", home.occurredAt)
    }

    @Test fun invalidProfileAddressesAreRejectedBeforeCallingAnySource() = runTest {
        val reader = createPublicProfileCheckIns(source { _, _ -> error("No IO") })
        for (slug in listOf("", " ", "x".repeat(101))) assertTrue(reader.page(slug).isFailure)
    }

    @Test fun originalFailuresAreRetainedAndCancellationPropagates() = runTest {
        val error = IllegalStateException("failed")
        val failed = object : PublicAuthorPosts {
            override suspend fun page(slug: String, cursor: String?) = Result.failure<PublicAuthorPostPage>(error)
        }
        assertSame(error, createPublicProfileCheckIns(failed).page("anna").exceptionOrNull())
        val cancelled = createPublicProfileCheckIns(source { _, _ -> throw CancellationException("cancelled") })
        assertFailsWith<CancellationException> { cancelled.page("anna") }
    }

    @Test fun rowsStaySelfContainedAfterLongScrollAndLoadingOtherAuthors() = runTest {
        val source = source { slug, _ -> PublicAuthorPostPage((0 until 500).map { post("$slug-$it").copy(authorSlug = slug) }, null) }
        val reader = createPublicProfileCheckIns(source)
        val first = reader.page("anna").getOrThrow().items
        reader.page("other").getOrThrow()
        assertEquals("anna-0", first.first().id)
        assertEquals("anna-499", first.last().id)
        assertEquals("note", first.first().note)
    }

    private suspend fun load(post: PublicAuthorPost) = createPublicProfileCheckIns(source { _, _ ->
        PublicAuthorPostPage(listOf(post), null)
    }).page("anna").getOrThrow().items.single()

    private fun source(block: suspend (String, String?) -> PublicAuthorPostPage) = object : PublicAuthorPosts {
        override suspend fun page(slug: String, cursor: String?) = Result.success(block(slug, cursor))
    }

    private fun post(id: String = "entry") = PublicAuthorPost(
        id = id, authorSlug = "anna", authorName = "Anna", authorAvatarUrl = "avatar",
        shopSlug = "shop", shopName = "Shop", note = "note", createdAt = "created", visitedAt = "visited",
        isPublicApproved = true, isHome = false, scores = PublicPostScores(3, 4, 5),
        drink = PublicPostDrink("Мой кофе", "Кофе", "Coffee"), home = null,
        originals = listOf("original"), thumbnails = listOf(""), details = listOf("detail"),
        helpfulCount = 3, isHelpfulByCurrentUser = true,
    )
}
