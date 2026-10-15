package com.coffeepeek.admin.analytics

import android.content.Context
import android.net.ConnectivityManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlinx.coroutines.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Run on an isolated OFFLINE emulator: inspect native SDK records, never ingest test events. */
@RunWith(AndroidJUnit4::class)
class PostHogPayloadTest {
    @Test fun nativeSdkPayloadIsSanitizedAndRevocationDeletesTheOfflineQueue() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        assumeTrue("This test requires an offline emulator and never sends test events", connectivity.activeNetwork == null)
        val queue = File(context.cacheDir, "posthog-disk-queue/$POSTHOG_PROJECT_TOKEN")
        val transport = PostHogTransport()
        transport.erase()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        var storedConsent = AnalyticsConsent.Unknown
        val store = object : AnalyticsConsentStore {
            override suspend fun read() = storedConsent
            override suspend fun write(consent: AnalyticsConsent) { storedConsent = consent }
        }
        val service = AnalyticsService(AnalyticsContext("coffee-peek-android", "sdk-smoke+0", "development"), store, transport, scope, true)
        try {
            service.setForeground(true)
            service.restoreConsent()
            service.syncIdentity(null)
            service.screenShown("discovery", "sdk-smoke-visit")
            assertFalse(service.track(AnalyticsEvent.AUTH_GATE_SHOWN, mapOf("feature" to "check_in")))
            delay(200)
            assertTrue(queue.listFiles().isNullOrEmpty())

            service.setConsent(AnalyticsConsent.Accepted)
            assertFalse(service.track(AnalyticsEvent.AUTH_GATE_SHOWN, mapOf("feature" to "check_in")))
            assertTrue("Guests must not initialize SDK or write events", queue.listFiles().isNullOrEmpty())
            service.syncIdentity("sdk-smoke-user")
            assertTrue(service.track(AnalyticsEvent.CATALOG_SEARCH_STARTED, mapOf(
                "catalog" to "shops", "search_id" to "search-smoke-1", "source" to "list",
                "has_query" to true, "query_length" to 4, "filter_keys" to listOf("tags"),
                "sort" to "relevance", "nearby" to false,
            )))
            // Verify defense against properties reintroduced below the typed application boundary.
            transport.capture("feature_exposed", mapOf(
                "feature" to "search", "placement" to "shops", "page_type" to "discovery",
                "app" to "coffee-peek-android", "event_schema_version" to 1, "app_version" to "sdk-smoke+0",
                "environment" to "development", "auth_state" to "signed_in",
                "query" to "DO_NOT_SEND_RAW_TEXT", "latitude" to 12.34, "longitude" to 56.78,
                "\$current_url" to "https://private.invalid/?token=DO_NOT_SEND_RAW_TEXT",
                "\$device_name" to "DO_NOT_SEND_RAW_TEXT", "\$set" to mapOf("email" to "DO_NOT_SEND_RAW_TEXT"),
            ))
            val records = withTimeout(10_000) {
                var records: List<JSONObject>
                do {
                    records = queue.listFiles().orEmpty().mapNotNull { file -> runCatching { JSONObject(file.readText()) }.getOrNull() }
                    if (records.none { it.optString("event") == "catalog_search_started" } || records.none { it.optString("event") == "feature_exposed" }) delay(50)
                } while (records.none { it.optString("event") == "catalog_search_started" } || records.none { it.optString("event") == "feature_exposed" })
                records
            }
            val search = records.single { it.getString("event") == "catalog_search_started" }.getJSONObject("properties")
            assertEquals(true, search.get("has_query"))
            assertEquals(false, search.get("nearby"))
            assertEquals("development", search.getString("environment"))
            assertEquals("discovery", search.getString("page_type"))
            assertEquals(1, search.getInt("event_schema_version"))
            records.forEach { record ->
                assertFalse(record.toString().contains("DO_NOT_SEND_RAW_TEXT"))
                val properties = record.getJSONObject("properties")
                listOf("query", "latitude", "longitude", "\$current_url", "\$device_name", "\$set", "\$set_once").forEach {
                    assertFalse("Forbidden SDK property: $it", properties.has(it))
                }
            }
            service.setConsent(AnalyticsConsent.Declined)
            assertFalse(service.track(AnalyticsEvent.AUTH_GATE_SHOWN, mapOf("feature" to "check_in")))
            assertTrue("Revocation must delete the native offline queue", queue.listFiles().isNullOrEmpty())
        } finally {
            service.setConsent(AnalyticsConsent.Declined)
            scope.cancel()
        }
    }
}
