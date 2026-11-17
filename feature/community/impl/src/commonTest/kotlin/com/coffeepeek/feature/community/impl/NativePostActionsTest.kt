package com.coffeepeek.feature.community.impl

import com.coffeepeek.feature.community.impl.ui.PostCardActions
import com.coffeepeek.feature.community.impl.ui.nativePostActions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NativePostActionsTest {
    @Test fun disabledMenuDoesNotDisableIndependentNavigationAndLikes() {
        val calls = mutableListOf<String>()
        val actions = PostCardActions({ calls += "author" }, { calls += "shop" }, { calls += "like" },
            { calls += "block" }, {}, onEdit = {}, onHide = {}, onReport = {}, onPublish = {}, onShare = {})
        val native = nativePostActions(actions, enabled = false, changing = false)
        native.actions.values.forEach { it() }
        assertEquals(listOf("author", "shop", "like"), calls)
        assertEquals(setOf("author", "shop", "like"), native.actions.keys)
    }

    @Test fun changingVisibilityDisablesOnlyHideAndRetainsFeatureCallbacks() {
        var shared = 0
        val actions = PostCardActions(null, null, null, null, {}, onHide = {}, onShare = { shared++ })
        val native = nativePostActions(actions, enabled = true, changing = true)
        assertNull(native.actions["hide"])
        native.actions.getValue("share")()
        assertEquals(1, shared)
    }

    @Test fun missingCapabilitiesStayAbsentAndPhotoIndexIsPreserved() {
        var index = -1
        val native = nativePostActions(PostCardActions(null, null, null, null, { index = it }), true, false)
        assertEquals(emptySet(), native.actions.keys)
        native.onPhoto(3)
        assertEquals(3, index)
    }
}
