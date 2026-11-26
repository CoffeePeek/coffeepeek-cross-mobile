package com.coffeepeek.admin.di.community

import kotlin.test.*

class CommunityFeedRefreshTest {
    @Test fun disposingAnOldControllerCannotClearItsReplacementRegistration() {
        val refresh = CommunityFeedRefresh()
        val calls = mutableListOf<String>()
        val old = refresh.register { calls += "old" }
        val current = refresh.register { calls += "current" }
        old()
        refresh.refresh()
        current()
        refresh.refresh()
        assertEquals(listOf("current"), calls)
    }
}
