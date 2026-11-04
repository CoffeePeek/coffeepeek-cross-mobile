package com.coffeepeek.admin.ui.screen.profile

import com.coffeepeek.core.session.Session
import com.coffeepeek.core.session.SessionReader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class PersonalProfileWeekCacheTest {
    @Test fun emptyWeekIsCachedAndInputCollectionsAreDetached() = runTest {
        val sessions = Sessions()
        val cache = PersonalProfileWeekCache(sessions, backgroundScope)
        val counts = mutableMapOf("2026-11-04" to 2)
        val photos = mutableMapOf("2026-11-04" to mutableListOf("https://example.invalid/photo"))
        cache.write("1:a", "2026-11-04", PersonalProfileWeekSnapshot(counts, photos))
        counts.clear()
        photos.getValue("2026-11-04").clear()
        assertEquals(2, cache.read("1:a", "2026-11-04")?.counts?.get("2026-11-04"))
        assertEquals(1, cache.read("1:a", "2026-11-04")?.photoUrls?.get("2026-11-04")?.size)
        cache.write("1:a", "2026-11-05", PersonalProfileWeekSnapshot(emptyMap(), emptyMap()))
        assertNotNull(cache.read("1:a", "2026-11-05"))
        assertNull(cache.read("1:a", "2026-11-04"))
        cache.close()
    }

    @Test fun logoutSameOwnerReloginAndSwitchCannotReadPreviousWeek() = runTest {
        val sessions = Sessions()
        val cache = PersonalProfileWeekCache(sessions, backgroundScope)
        val week = PersonalProfileWeekSnapshot(mapOf("day" to 3), emptyMap())
        cache.write("1:a", "today", week)
        sessions.set(Session("rotated", null, "a"))
        assertEquals(week, cache.read("1:a", "today"))
        sessions.set(null)
        runCurrent()
        assertNull(cache.read("1:a", "today"))
        sessions.set(Session("second-login", null, "a"))
        assertNull(cache.read("3:a", "today"))
        cache.write("3:a", "today", week)
        sessions.set(Session("other", null, "b"))
        assertNull(cache.read("4:b", "today"))
        cache.close()
    }

    private class Sessions : SessionReader {
        val state = MutableStateFlow<Session?>(Session("first", null, "a"))
        var generation = 1L
        override fun accountGeneration() = generation
        override fun peekSession() = state.value
        override fun isActiveSession(session: Session?) = session?.userId != null
        override fun observeSession() = state
        override suspend fun getSession() = state.value
        fun set(value: Session?) {
            val old = state.value
            if (old?.userId == null || old.userId != value?.userId) generation++
            state.value = value
        }
    }
}
