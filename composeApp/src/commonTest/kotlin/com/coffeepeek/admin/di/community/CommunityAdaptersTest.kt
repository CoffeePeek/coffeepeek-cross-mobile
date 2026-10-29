@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
package com.coffeepeek.admin.di.community

import com.coffeepeek.admin.analytics.*
import com.coffeepeek.admin.feature.community.ui.FeedShops
import com.coffeepeek.admin.feature.community.ui.FeedSessions
import com.coffeepeek.core.session.Session
import com.coffeepeek.feature.community.api.EditField
import com.coffeepeek.feature.community.api.FeedAction
import com.coffeepeek.feature.community.api.FeedItemAction
import com.coffeepeek.feature.community.api.FeedTrigger
import com.coffeepeek.feature.community.domain.model.PostVisibility
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/** The platform-neutral account/catalog/analytics adapters are shared by the Android and native iOS feed roots. */
class CommunityAdaptersTest {
    @Test fun theAccountKeyIsTheUserIdElseTheTokenAndNullForAGuestOrAnInactiveSession() = runTest {
        val sessions = FeedSessions(null)
        val account = SessionCommunityAccount(sessions, CommunityProfileAccounts())
        assertNull(account.accountKey.first()); assertNull(account.currentAccountKey())
        sessions.applySession(Session("token", userId = "user-1"))
        assertEquals("user-1", account.accountKey.first()); assertEquals("user-1", account.currentAccountKey())
        sessions.applySession(Session("token-only"))
        assertEquals("token-only", account.currentAccountKey())
        sessions.applySession(Session(" ", userId = "user-1")) // a blank token is not an active session
        assertNull(account.currentAccountKey()); assertNull(account.accountKey.first())
    }

    @Test fun theOwnSlugComesFromTheRefreshedProfileAndAFailureIsPassedOn() = runTest {
        val users = CommunityProfileAccounts()
        val account = SessionCommunityAccount(FeedSessions(Session("token", userId = "one")), users)
        assertEquals("me", account.ownAuthorSlug().getOrThrow())
        users.load = { Result.success(communityOwnProfile(" ")) }
        assertNull(account.ownAuthorSlug().getOrThrow()) // a blank slug is no slug
        users.load = { Result.success(communityOwnProfile(null)) }
        assertNull(account.ownAuthorSlug().getOrThrow())
        users.load = { Result.failure(IllegalStateException("Offline")) }
        assertTrue(account.ownAuthorSlug().isFailure)
    }

    @Test fun theDrinkCatalogIsMappedFromTheShopRepository() = runTest {
        val drinks = ShopCommunityDrinks(FeedShops()).load().getOrThrow()
        assertEquals(listOf("cappuccino"), drinks.map { it.slug }); assertEquals("Капучино", drinks.single().nameRu); assertEquals("Cappuccino", drinks.single().nameEn)
    }

    @Test fun feedEventsKeepTheLegacyNamesAndProperties() = runTest {
        val sdk = startAnalytics()
        val analytics = AppCommunityAnalytics()
        analytics.feedAction(FeedAction.Refresh, FeedTrigger.Gesture)
        analytics.feedAction(FeedAction.LoadMore, FeedTrigger.Scroll)
        analytics.feedAction(FeedAction.Retry, FeedTrigger.Button)
        analytics.itemInteracted(FeedItemAction.Like, isHome = false)
        analytics.itemInteracted(FeedItemAction.Unlike, isHome = true)
        analytics.itemInteracted(FeedItemAction.Shop, isHome = false)
        analytics.itemInteracted(FeedItemAction.Photo, isHome = true)
        analytics.likeGateShown()
        runCurrent()
        assertEquals(listOf("refresh" to "gesture", "load_more" to "scroll", "retry" to "button"),
            sdk.events.filter { it.first == "feed_action_clicked" }.map { it.second["action"] to it.second["trigger"] })
        assertEquals(listOf("like" to "shop", "unlike" to "home", "shop" to "shop", "photo" to "home"),
            sdk.events.filter { it.first == "feed_item_interacted" }.map { it.second["action"] to it.second["check_in_kind"] })
        assertEquals("feed_like", sdk.events.single { it.first == "auth_gate_shown" }.second["feature"])
    }

    @Test fun theEditFunnelReportsTheDiaryEditFlowLikeTheLegacyEditor() = runTest {
        val sdk = startAnalytics()
        val analytics = AppCommunityAnalytics()
        analytics.editOpened()
        analytics.editFieldChanged(EditField.Note); analytics.editFieldChanged(EditField.Rating); analytics.editFieldChanged(EditField.Drink)
        analytics.editSubmitted(); analytics.editRejected()
        analytics.editSubmitted(); analytics.editSaved(PostVisibility.Public, hasPhotos = true)
        runCurrent()
        val started = sdk.events.single { it.first == "check_in_flow_started" }.second
        assertEquals("edit", started["mode"]); assertEquals("my_check_ins", started["source"]); assertEquals("visit", started["kind"])
        assertEquals("editor", sdk.events.single { it.first == "check_in_step_viewed" }.second["step"])
        assertEquals(listOf("note", "rating", "drink"), sdk.events.filter { it.first == "check_in_field_interacted" }.map { it.second["field"] })
        assertEquals(listOf("form"), sdk.events.single { it.first == "check_in_validation_failed" }.second["fields"])
        val saved = sdk.events.single { it.first == "check_in_saved" }.second
        assertEquals("Public", saved["visibility"]); assertEquals(true, saved["has_photos"])
    }

    @Test fun aFailedSaveReportsTheSaveStageAndCancelAnEditedFlow() = runTest {
        val sdk = startAnalytics()
        val failing = AppCommunityAnalytics()
        failing.editOpened(); failing.editSubmitted(); failing.editFailed(IllegalStateException("private server detail"))
        runCurrent()
        assertEquals("save", sdk.events.single { it.first == "check_in_failed" }.second["stage"])
        assertTrue(sdk.events.none { "private server detail" in it.second.toString() })
        val cancelling = AppCommunityAnalytics()
        cancelling.editOpened(); cancelling.editCancelled()
        runCurrent()
        assertTrue(sdk.events.any { it.first == "check_in_cancelled" })
    }

    @Test fun eventsBeforeAnEditorWasOpenedAreIgnoredWithoutFailing() = runTest {
        val sdk = startAnalytics()
        val analytics = AppCommunityAnalytics()
        analytics.editFieldChanged(EditField.Note); analytics.editSubmitted(); analytics.editCancelled()
        runCurrent()
        assertTrue(sdk.events.none { it.first.startsWith("check_in") })
    }
}

private suspend fun TestScope.startAnalytics(): RecordingTransport {
    val sdk = RecordingTransport()
    val service = analytics(MemoryConsent(AnalyticsConsent.Accepted), sdk)
    service.setForeground(true)
    service.restoreConsent()
    service.syncIdentity("fixture-owner")
    service.screenShown("feed", "fixture-visit")
    Analytics.install(service)
    return sdk
}
