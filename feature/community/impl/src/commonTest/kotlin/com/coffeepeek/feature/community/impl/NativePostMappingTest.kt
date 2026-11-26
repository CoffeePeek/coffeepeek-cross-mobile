package com.coffeepeek.feature.community.impl

import com.coffeepeek.feature.community.domain.model.*
import com.coffeepeek.feature.community.impl.ui.*
import kotlin.test.*

class NativePostMappingTest {
    private val texts = NativePostTexts("You", "User", "Shop", "Bag", "Other", " · ", "Coffee", "Saving", "Publish",
        "Private", "Public", "Rejected", "Unknown", "Fix it", "Edit rejection", "Share", "Edit", "Hide", "Report", "Block", "Open", "Actions")
    private val formatted = NativePostFormattedTexts("Open profile", "Rating", listOf("First", "Second"), "Likes", "18 g", "120 s", "5/5")
    private fun card(post: CommunityPost, own: Boolean = false, public: Boolean = true,
        enabled: Boolean = true, changing: Boolean = false, actions: PostCardActions = PostCardActions(null, null, null, null, {})) =
        nativePostPresentation(post, own, public, true, enabled, changing, actions, texts, formatted, "Date", "en", { it }, 16.0)

    @Test fun galleryPreservesThumbnailFallbackAndIndependentInlineUpgradeSource() {
        val post = post().copy(photos = PostPhotos(listOf("full-1", "full-2"), listOf("thumb", " "), listOf("detail", "")))
        val photos = card(post).body.photos
        assertEquals(listOf("thumb", "full-2"), photos.map { it.previewUrl })
        assertEquals(listOf("detail", "full-2"), photos.map { it.detailUrl })
        assertEquals(listOf("First", "Second"), photos.map { it.label })
        assertEquals("false:post", card(post).key)
    }

    @Test fun rejectedOwnerKeepsReasonAndOnlyOfferedCorrectionWithoutAudienceLabel() {
        val post = post().copy(moderation = PostModeration.Rejected, rejectionReason = " ")
        val actions = PostCardActions(null, null, null, null, {}, onEdit = {})
        val status = card(post, own = true, public = false, actions = actions).status
        assertEquals("Rejected", status.rejectedTitle)
        assertEquals("Fix it", status.rejectedReason)
        assertEquals("Edit rejection", status.editLabel)
        assertNull(status.publication)
        assertNull(card(post, own = true, public = false).status.editLabel)
    }

    @Test fun menuOffersOnlyCapabilitiesAndVisibilityBusyOnlyDisablesHide() {
        val actions = PostCardActions(null, null, null, {}, {}, onHide = {}, onShare = {}, onPublish = {})
        val card = card(post(), changing = true, actions = actions)
        assertEquals(listOf("share", "hide", "block"), card.menu.map { it.id })
        assertTrue(card.menu.first().enabled)
        assertFalse(card.menu[1].enabled)
        assertEquals("Saving", card.menu[1].label)
        assertTrue(card.menu.last().destructive)
        assertEquals("Saving", card.footer.publishLabel)
        assertTrue(card(post(), enabled = false, actions = actions).menu.none { it.enabled })
    }

    @Test fun emptyAuthorAndRecipeUseResolvedCopyWhileKeepingBagAndRatingRules() {
        val home = HomePreparation("2026-11-25", "other", " ", 18.0, 120, 5, shelfBagCardUrl = "bag")
        val post = post(home = true).copy(author = PostAuthor(null, null, " ", null), home = home, rating = null)
        val card = card(post, own = true)
        assertEquals("You", card.header.name)
        assertEquals("Date", card.header.dateLabel)
        assertEquals("Bag", card.body.beanName)
        assertEquals("Other · 18 g · 120 s · 5/5", card.body.recipe)
        assertNull(card.body.rating)
        assertEquals("true:post", card.key)
    }

    @Test fun helpfulCopySelectionRetainsReadOnlyAndToggleDescriptions() {
        val post = post()
        assertNotEquals(nativeHelpfulResource(post, false), nativeHelpfulResource(post, true))
        assertNotEquals(nativeHelpfulResource(post, true), nativeHelpfulResource(post.copy(isHelpfulByCurrentUser = true), true))
        assertEquals(nativeHelpfulResource(post, false), nativeHelpfulResource(post.copy(isHelpfulByCurrentUser = true), false))
    }
}
