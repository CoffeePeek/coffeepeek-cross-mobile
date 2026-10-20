package com.coffeepeek.feature.shop.impl.ui.data

import com.coffeepeek.feature.shop.api.ShopCheckInAuthor
import com.coffeepeek.feature.shop.domain.model.ShopCheckIn
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ShopCheckInAuthorTest {
    private val row = ShopCheckIn("visit", "auth-user", "shop", "", "", "", null,
        emptyList(), emptyList(), null, username = "Author", authorId = "public-id",
        authorSlug = "public-slug", authorAvatarUrl = "issued-avatar")

    @Test fun publicIdentityIsNotInferredFromUserId() {
        assertEquals(ShopCheckInAuthor("public-id", "public-slug", "Author", "issued-avatar"),
            row.blockableAuthor(false, true, "viewer"))
        assertNull(row.copy(authorId = " ", authorSlug = null).blockableAuthor(false, false, null))
    }

    @Test fun guestsAreNotOfferedBlockingForAnyPublicIdentity() {
        assertNull(row.blockableAuthor(false, false, null))
        assertNull(row.copy(authorId = null).blockableAuthor(false, false, null))
        assertNull(row.copy(authorSlug = null).blockableAuthor(false, false, "viewer"))
    }

    @Test fun signedInViewersCanBlockUsingIdOrCanonicalSlug() {
        assertEquals("public-slug", row.copy(authorId = null).blockableAuthor(false, true, "viewer")?.slug)
        assertEquals("public-id", row.copy(authorSlug = null).blockableAuthor(false, true, "viewer")?.id)
    }

    @Test fun ownersAndUnresolvedSignedInProfilesAreNotOfferedBlocking() {
        assertNull(row.blockableAuthor(true, true, "viewer"))
        assertNull(row.blockableAuthor(false, true, null))
        assertNull(row.blockableAuthor(false, true, " "))
        assertNull(row.blockableAuthor(false, true, "public-slug"))
    }
}
