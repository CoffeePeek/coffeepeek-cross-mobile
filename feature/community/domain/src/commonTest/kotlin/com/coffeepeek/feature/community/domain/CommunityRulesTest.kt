package com.coffeepeek.feature.community.domain

import com.coffeepeek.feature.community.domain.model.CommunityPost
import com.coffeepeek.feature.community.domain.model.PostAuthor
import com.coffeepeek.feature.community.domain.model.PostEdit
import com.coffeepeek.feature.community.domain.model.PostModeration
import com.coffeepeek.feature.community.domain.model.PostPhotos
import com.coffeepeek.feature.community.domain.model.PostRating
import com.coffeepeek.feature.community.domain.model.PostVisibility
import com.coffeepeek.feature.community.domain.rules.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CommunityRulesTest {
    private fun post(
        id: String = "post", slug: String? = "other", note: String = "", visibility: PostVisibility = PostVisibility.Public,
        moderation: PostModeration = PostModeration.Approved,
    ) = CommunityPost(id, "shop", "Shop", PostAuthor("author-$id", slug, "Name", null), note, "2026-10-07T10:00:00Z", "", PostPhotos.None,
        PostRating(4, 4, 5), null, visibility, moderation, 0, null, 0, false, false, null)

    @Test fun editIsCheckedInTheLegacyOrderNoteThenRatingThenDrink() {
        assertNull(validateEdit(PostEdit("ok")))
        assertEquals(EditIssue.NoteTooLong, validateEdit(PostEdit("x".repeat(1001), PostRating(0, 0, 0), "other", null)))
        assertNull(validateEdit(PostEdit(" " + "x".repeat(1000) + " ")))
        assertEquals(EditIssue.RatingOutOfRange, validateEdit(PostEdit("ok", PostRating(6, 4, 4))))
        assertEquals(EditIssue.RatingOutOfRange, validateEdit(PostEdit("ok", PostRating(4, 0, 4))))
        assertNull(validateEdit(PostEdit("ok", PostRating(1, 5, 3))))
    }

    @Test fun otherDrinkNeedsAShortNameAndNamedDrinksCannotCarryOne() {
        assertEquals(EditIssue.DrinkNameRequired, validateEdit(PostEdit("ok", drinkSlug = "other", customDrinkName = "  ")))
        assertEquals(EditIssue.DrinkNameRequired, validateEdit(PostEdit("ok", drinkSlug = "other")))
        assertEquals(EditIssue.DrinkNameRequired, validateEdit(PostEdit("ok", drinkSlug = "other", customDrinkName = "x".repeat(101))))
        assertNull(validateEdit(PostEdit("ok", drinkSlug = "other", customDrinkName = "x".repeat(100))))
        assertEquals(EditIssue.DrinkNameNotAllowed, validateEdit(PostEdit("ok", drinkSlug = "cappuccino", customDrinkName = "x")))
        assertEquals(EditIssue.DrinkNameNotAllowed, validateEdit(PostEdit("ok", customDrinkName = "x")))
        assertNull(validateEdit(PostEdit("ok", drinkSlug = "cappuccino")))
    }

    @Test fun reportNeedsAtLeastOneAndAtMost2000CharactersAfterTrimming() {
        assertEquals(ReportIssue.Empty, validateReport("   "))
        assertEquals(ReportIssue.TooLong, validateReport("x".repeat(2001)))
        assertNull(validateReport(" " + "x".repeat(2000) + " "))
        assertNull(validateReport("a"))
    }

    @Test fun publicationLabelsFollowVisibilityThenModeration() {
        assertEquals(PublicationLabel.Private, post(visibility = PostVisibility.Private, moderation = PostModeration.Approved).publicationLabel())
        assertEquals(PublicationLabel.Pending, post(moderation = PostModeration.Pending).publicationLabel())
        assertEquals(PublicationLabel.Published, post(moderation = PostModeration.Approved).publicationLabel())
        assertEquals(PublicationLabel.Rejected, post(moderation = PostModeration.Rejected).publicationLabel())
        assertEquals(PublicationLabel.Public, post(moderation = PostModeration.NotSubmitted).publicationLabel())
        assertEquals(PublicationLabel.Unknown, post(moderation = PostModeration.Unknown).publicationLabel())
    }

    @Test fun onlyPublicAndApprovedPostsStayInThePublicTimeline() {
        assertFalse(isWithdrawnFromPublic(post()))
        assertTrue(isWithdrawnFromPublic(post(visibility = PostVisibility.Private)))
        assertTrue(isWithdrawnFromPublic(post(moderation = PostModeration.Pending)))
        assertTrue(isWithdrawnFromPublic(post(moderation = PostModeration.Rejected)))
    }

    @Test fun onlyAPublishedPostHasALinkToShare() {
        assertEquals("https://coffeepeek.by/check-ins/6966d36f", post("6966d36f").shareLink())
        assertNull(post(visibility = PostVisibility.Private).shareLink())
        assertNull(post(moderation = PostModeration.Pending).shareLink())
        assertNull(post(moderation = PostModeration.NotSubmitted).shareLink())
        assertNull(post(" ").shareLink())
    }

    @Test fun appendingKeepsOrderRefreshesKnownPostsInPlaceAndSkipsDuplicates() {
        val first = post("a"); val second = post("b")
        val merged = appendTimeline(listOf(first, second), listOf(post("b", note = "fresh"), post("c")))
        assertEquals(listOf("a", "b", "c"), merged.map { it.id })
        assertEquals("fresh", merged[1].note)
        assertEquals(listOf("a"), appendTimeline(listOf(first), listOf(first, first)).map { it.id })
    }

    @Test fun ownershipNeedsASignedInViewerAndEitherThePersonalTimelineOrTheCanonicalSlug() {
        val mine = post(slug = "me"); val other = post(slug = "other")
        assertTrue(ownsPost(mine, true, personalTimeline = false, ownSlug = "me"))
        assertFalse(ownsPost(other, true, personalTimeline = false, ownSlug = "me"))
        assertFalse(ownsPost(mine, true, personalTimeline = false, ownSlug = null))
        assertTrue(ownsPost(other, true, personalTimeline = true, ownSlug = null))
        assertFalse(ownsPost(mine, false, personalTimeline = true, ownSlug = "me"))
        assertFalse(ownsPost(mine, null, personalTimeline = true, ownSlug = "me"))
        assertFalse(ownsPost(post(slug = null), true, personalTimeline = false, ownSlug = "me"))
    }

    @Test fun blockingIsOfferedOnlyOnThePublicTimelineAndWaitsForTheOwnSlug() {
        assertTrue(canBlockAuthor(false, personalTimeline = false, ownSlug = null, owned = false)) // guest: sign-in prompt
        assertFalse(canBlockAuthor(null, personalTimeline = false, ownSlug = null, owned = false))
        assertFalse(canBlockAuthor(true, personalTimeline = false, ownSlug = null, owned = false)) // slug still unknown
        assertTrue(canBlockAuthor(true, personalTimeline = false, ownSlug = "me", owned = false))
        assertFalse(canBlockAuthor(true, personalTimeline = false, ownSlug = "me", owned = true))
        assertFalse(canBlockAuthor(true, personalTimeline = true, ownSlug = "me", owned = false))
        assertFalse(canBlockAuthor(false, personalTimeline = true, ownSlug = null, owned = false))
    }
}
