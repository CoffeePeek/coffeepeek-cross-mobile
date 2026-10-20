@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
package com.coffeepeek.admin.di.shop

import com.coffeepeek.admin.analytics.*
import com.coffeepeek.admin.feature.checkin.api.CheckInFlowHandoff
import com.coffeepeek.feature.shop.api.*
import com.coffeepeek.admin.ui.screen.shop.shopRouteOptions
import kotlinx.coroutines.test.*
import kotlin.test.*

class AndroidShopAnalyticsTest {
    @Test fun detailActionsUseTheLegacySchemaAndRelatedEntry() = runTest {
        val sdk = startAnalytics()
        AndroidShopAnalytics.action(ShopActionMetric.Directions)
        AndroidShopAnalytics.action(ShopActionMetric.ShowOnMap)
        AndroidShopAnalytics.action(ShopActionMetric.PhoneCall)
        AndroidShopAnalytics.content(ShopContentMetric.Hours, ShopContentAction.Close)
        AndroidShopAnalytics.content(ShopContentMetric.Roasters, ShopContentAction.Select)
        runCurrent()
        val actions = sdk.events.filter { it.first == "shop_action_clicked" }.map { it.second }
        assertEquals(listOf("mobile_actions", "shop_detail", "shop_contacts"), actions.map { it["entry_point"] })
        assertEquals(listOf("directions", "directions", "phone_call"), actions.map { it["action"] })
        assertEquals("related", DetailEntryMemory.consume(Entity.Roaster)["entry_point"])
        assertEquals(2, sdk.events.count { it.first == "shop_content_interacted" })
    }

    @Test fun formKeepsTheGlobalFlowAndStopsAfterItsSingleSave() = runTest {
        val sdk = startAnalytics()
        CheckInFlowHandoff.tracker = CheckInTracker(CheckInKind.Visit, CheckInSource.Global)
        val form = AndroidShopCheckInAnalytics { true }
        form.opened()
        form.field(ShopCheckInField.RatingCoffee)
        form.submitted()
        form.saved(public = false, hasPhotos = true)
        form.cancelled()
        runCurrent()
        val saved = sdk.events.single { it.first == "check_in_saved" }.second
        assertEquals("global_create", saved["source"])
        assertEquals("Private", saved["visibility"])
        assertEquals(true, saved["has_photos"])
        assertEquals(1, sdk.events.count { it.first == "check_in_flow_started" })
        assertTrue(sdk.events.none { it.first == "check_in_cancelled" })
    }

    @Test fun revokedSessionAndDeclinedConsentDoNotReportFormWrites() = runTest {
        val sdk = startAnalytics()
        var current = true
        val form = AndroidShopCheckInAnalytics { current }
        form.opened()
        current = false
        form.submitted()
        form.saved(false, false)
        Analytics.service!!.setConsent(AnalyticsConsent.Declined)
        AndroidShopAnalytics.action(ShopActionMetric.Share)
        runCurrent()
        assertTrue(sdk.events.none { it.first in setOf("check_in_saved", "check_in_submit_attempted", "shop_action_clicked") })
    }

    @Test fun androidKeepsBothMapProviders() {
        val options = shopRouteOptions(53.9, 27.5)
        assertEquals(2, options.size)
        assertTrue(options.any { it.url.startsWith("https://www.google.com/maps/dir/") })
        assertTrue(options.any { it.url.startsWith("https://yandex.ru/maps/") })
    }
}

private suspend fun TestScope.startAnalytics(): RecordingTransport {
    val sdk = RecordingTransport()
    val service = analytics(MemoryConsent(AnalyticsConsent.Accepted), sdk)
    service.setForeground(true)
    service.restoreConsent()
    service.syncIdentity("fixture-owner")
    service.screenShown("shop_detail", "fixture-visit")
    Analytics.install(service)
    CheckInFlowHandoff.tracker = null
    return sdk
}
