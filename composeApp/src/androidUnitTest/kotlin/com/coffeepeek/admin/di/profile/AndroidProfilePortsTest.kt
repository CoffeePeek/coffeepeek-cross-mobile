@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.coffeepeek.admin.di.profile

import com.coffeepeek.admin.analytics.Analytics
import com.coffeepeek.admin.analytics.AnalyticsEvent
import com.coffeepeek.admin.analytics.parity.ParityHarness
import com.coffeepeek.admin.analytics.parity.ParityHarness.record
import com.coffeepeek.feature.authorblocking.api.AuthorVisibility
import com.coffeepeek.feature.authorblocking.api.AuthorVisibilitySnapshot
import com.coffeepeek.feature.profile.api.ProfileAuthorVisibility
import com.coffeepeek.feature.profile.api.ProfileAuthorVisibilityState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class AndroidProfilePortsTest {
    @Test fun visibilityHidesEverythingUntilReadyThenByIdOrSlugAndRelaysChanges() = runTest {
        assertTrue(ProfileAuthorVisibilityState(ready = false).hides("x", "y"))
        val open = ProfileAuthorVisibilityState(blockedIds = setOf("id"), blockedSlugs = setOf("slug"))
        assertTrue(open.hides("id", null)); assertTrue(open.hides(null, "slug"))
        assertFalse(open.hides("other", "other")); assertFalse(open.hides(null, null))

        val source = object : AuthorVisibility {
            override val visibility = MutableStateFlow(AuthorVisibilitySnapshot(revision = 1, ready = true))
            var refreshes = 0
            override suspend fun refresh(): Result<Unit> { refreshes++; return Result.success(Unit) }
        }
        val port = AppProfileAuthorVisibility(source)
        assertEquals(ProfileAuthorVisibilityState(revision = 1), port.current())
        source.visibility.value = AuthorVisibilitySnapshot(2, true, setOf("id"), setOf("slug"))
        assertEquals(ProfileAuthorVisibilityState(2, true, setOf("id"), setOf("slug")), port.observe().first())
        assertTrue(port.refresh().isSuccess)
        assertEquals(1, source.refreshes)
        assertFalse(ProfileAuthorVisibility.None.current().hides("a", "b"))
    }

    @Test fun favoritesOpenedEmitsExactlyTheLegacyEvent() = runTest {
        val legacy = record { Analytics.track(AnalyticsEvent.FAVORITES_OPENED, "entry_point" to "profile") }
        val migrated = record { AppProfileAnalytics.favoritesOpened() }
        ParityHarness.assertParity("profile: open all favorites", legacy, migrated)
        assertEquals(1, migrated.size)
    }
}
