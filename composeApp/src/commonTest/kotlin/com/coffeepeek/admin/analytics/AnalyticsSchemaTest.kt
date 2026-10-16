package com.coffeepeek.admin.analytics

import kotlin.test.*

class AnalyticsSchemaTest {
    @Test fun routesExposeStableGroupsWithoutTheirArguments() {
        assertEquals("shop_detail", analyticsPage("com.coffeepeek.admin.ui.Navigator.Screen.ShopDetail/private-slug?token=secret"))
        assertEquals("profile", analyticsPage("com.coffeepeek.admin.ui.Navigator.Screen.Favorites"))
        assertEquals("other", analyticsPage("unknown/private-user-id?token=secret"))
        assertEquals("login", analyticsPage("com.coffeepeek.admin.ui.Navigator.Screen.ResetPassword?token=secret"))
    }

    @Test fun exactWireNamesAndNoArbitraryProperties() {
        assertEquals(37, AnalyticsEvent.entries.size)
        assertEquals("catalog_search_failed", AnalyticsEvent.CATALOG_SEARCH_FAILED.wire)
        assertNull(AnalyticsSchema.validate(AnalyticsEvent.FAVORITE_CHANGED, mapOf("entity_type" to "coffee_shop", "is_favorite" to true, "shop_id" to "private")))
        assertNull(AnalyticsSchema.validate(AnalyticsEvent.FAVORITE_ATTEMPTED, mapOf("entity_type" to "coffee")))
        assertNull(AnalyticsSchema.validate(AnalyticsEvent.SIGN_IN_COMPLETED, mapOf("method" to "email", "entry_point" to "login")))
        assertNotNull(AnalyticsSchema.validate(AnalyticsEvent.SIGN_IN_COMPLETED, mapOf("method" to "apple", "entry_point" to "login")))
    }

    @Test fun fieldAndFilterAllowlistsSortAndRejectUnknownNames() {
        val props = mapOf("catalog" to "shops", "filter_keys" to listOf("tags", "city", "tags"), "changed_filter_keys" to listOf("city"), "action" to "apply")
        assertEquals(listOf("city", "tags"), AnalyticsSchema.validate(AnalyticsEvent.FILTERS_APPLIED, props)?.get("filter_keys"))
        assertNull(AnalyticsSchema.validate(AnalyticsEvent.FILTERS_APPLIED, props + ("filter_keys" to listOf("latitude"))))
        assertNull(AnalyticsSchema.validate(AnalyticsEvent.MAP_INTERACTION, mapOf("action" to "loaded", "result_count" to -1)))
        assertNull(AnalyticsSchema.validate(AnalyticsEvent.FAVORITE_FAILED, mapOf("entity_type" to "roaster", "duration_ms" to 1L, "error_category" to "network", "status_code" to 999)))
    }

    @Test fun diagnosticErrorsContainOnlySafeCategoryAndStatus() {
        assertEquals("authentication", safeStatusError(401)?.category)
        assertEquals("permission", safeStatusError(403)?.category)
        assertEquals("not_found", safeStatusError(404)?.category)
        assertEquals("rate_limited", safeStatusError(429)?.category)
        assertEquals("server", safeStatusError(503)?.category)
        assertEquals("validation", safeStatusError(422)?.category)
        assertEquals("unknown", safeAnalyticsError(IllegalStateException("network https://private?token=secret")).category)
        assertEquals("cancelled", safeAnalyticsError(kotlinx.coroutines.CancellationException("private")).category)
    }

    @Test fun sdkEnrichmentNeverReintroducesUrlsPersonPropertiesOrAutocapture() {
        val props = mapOf<String, Any?>("\$current_url" to "https://private?token=secret", "\$set" to mapOf("email" to "a@example.com"),
            "\$device_name" to "User's phone", "\$screen_name" to "discovery", "\$lib" to "posthog-kmp", "page_type" to "discovery")
        val safe = assertNotNull(sanitizeSdkProperties("\$screen", props))
        assertEquals(setOf("\$screen_name", "\$lib", "page_type"), safe.keys)
        assertNull(sanitizeSdkProperties("\$autocapture", props))
        assertNull(sanitizeSdkProperties("\$exception", props))
        assertNull(sanitizeSdkProperties("\$screen", mapOf("app_version" to "x".repeat(8193))))
    }
}
