package com.coffeepeek.feature.community.impl

import com.coffeepeek.feature.community.domain.model.PostVisibility
import com.coffeepeek.feature.community.impl.ui.PostSharer
import com.coffeepeek.feature.community.impl.ui.previewPost
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PostSharingTest {
    @Test fun sharingKeepsPlaceholderCharactersInUserTextAndTracksBeforeSharing() {
        val calls = mutableListOf<String>()
        val sharer = PostSharer("%1\u0024s in %2\u0024s: %3\u0024s", "%1\u0024s: %2\u0024s", "User", "Shop", calls::add)
        val post = previewPost().let { it.copy(author = it.author.copy(name = "%2\u0024s"), shopName = "%3\u0024s") }
        sharer.action(post) { calls += "track" }!!.invoke()
        assertEquals(listOf("track", "%2\u0024s in %3\u0024s: https://coffeepeek.by/check-ins/preview"), calls)
    }

    @Test fun homeShareUsesAuthorFallbackAndDoesNotExposeShopText() {
        val texts = mutableListOf<String>()
        val sharer = PostSharer("%1\u0024s %2\u0024s %3\u0024s", "Home: %1\u0024s %2\u0024s", "User", "Shop", texts::add)
        val post = previewPost().let { it.copy(isHome = true, author = it.author.copy(name = "")) }
        sharer.action(post)!!.invoke()
        assertEquals(listOf("Home: User https://coffeepeek.by/check-ins/preview"), texts)
        assertNull(sharer.action(post.copy(visibility = PostVisibility.Private)))
    }
}
