@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
package com.coffeepeek.admin.di.favorites

import com.coffeepeek.admin.analytics.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.*
import kotlinx.coroutines.withContext
import kotlin.test.*

/** Shared analytics adapter contracts; the transport records only in memory. */
class MigratedFavoriteAnalyticsTest {
    @Test fun mutationReportsOneAttemptAndOneServerResultWithoutIdentity() = runTest {
        val sdk = installAnalytics()
        trackFavoriteMutation(Entity.Shop, false) { Result.success(Unit) }
        runCurrent()
        val changed = sdk.events.single { it.first == "favorite_changed" }.second
        assertEquals("coffee_shop", changed["entity_type"])
        assertEquals(false, changed["is_favorite"])
        assertEquals("server", changed["favorite_storage"])
        assertEquals(1, sdk.events.count { it.first == "favorite_attempted" })
        assertTrue(sdk.events.none { "fixture-owner" in it.second.toString() })
    }

    @Test fun writeResumedAfterSignInKeepsTheEarlierAttemptAndFlagsTheResult() = runTest {
        val sdk = installAnalytics()
        Analytics.track(AnalyticsEvent.FAVORITE_ATTEMPTED, "entity_type" to "coffee_shop", "is_favorite" to true)
        withContext(ResumedAfterAuth) { trackFavoriteMutation(Entity.Shop, true) { Result.success(Unit) } }
        runCurrent()
        assertEquals(1, sdk.events.count { it.first == "favorite_attempted" })
        assertEquals(true, sdk.events.single { it.first == "favorite_changed" }.second["resumed_after_auth"])
    }

    @Test fun ordinaryWriteIsNotFlaggedAsResumed() = runTest {
        val sdk = installAnalytics()
        trackFavoriteMutation(Entity.Shop, true) { Result.success(Unit) }
        runCurrent()
        assertEquals(false, sdk.events.single { it.first == "favorite_changed" }.second["resumed_after_auth"])
    }

    @Test fun accountReplacementAndCancellationNeverReportAChangedFavorite() = runTest {
        val sdk = installAnalytics()
        trackFavoriteMutation(Entity.Roaster, false) {
            Analytics.service!!.syncIdentity("replacement")
            Result.success(Unit)
        }
        assertFailsWith<CancellationException> {
            trackFavoriteMutation(Entity.Shop, true) { Result.failure(CancellationException()) }
        }
        runCurrent()
        assertTrue(sdk.events.none { it.first in setOf("favorite_changed", "favorite_failed") })
    }

    @Test fun declinedConsentDoesNotBlockTheMutationOrProduceTelemetry() = runTest {
        val sdk = installAnalytics(AnalyticsConsent.Declined)
        var writes = 0
        assertTrue(trackFavoriteMutation(Entity.Shop, false) { writes++; Result.success(Unit) }.isSuccess)
        runCurrent()
        assertEquals(1, writes)
        assertTrue(sdk.events.isEmpty())
    }
}

private suspend fun TestScope.installAnalytics(consent: AnalyticsConsent = AnalyticsConsent.Accepted): RecordingTransport {
    val sdk = RecordingTransport()
    val service = analytics(MemoryConsent(consent), sdk)
    service.setForeground(true)
    service.restoreConsent()
    service.syncIdentity("fixture-owner")
    Analytics.install(service)
    return sdk
}
