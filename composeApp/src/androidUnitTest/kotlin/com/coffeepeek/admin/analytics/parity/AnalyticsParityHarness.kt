@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.coffeepeek.admin.analytics.parity

import com.coffeepeek.admin.analytics.Analytics
import com.coffeepeek.admin.analytics.AnalyticsConsent
import com.coffeepeek.admin.analytics.MemoryConsent
import com.coffeepeek.admin.analytics.RecordingTransport
import com.coffeepeek.admin.analytics.analytics
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlin.test.fail

/**
 * Legacy-vs-migrated analytics parity. A scenario is one user journey written once as a [Driver]; the
 * legacy implementation (the production reference in composeApp) and the migrated adapter each run it
 * against a fresh consent-aware service, and the emitted wire events must be identical.
 *
 * Add an owner by writing its two drivers and a test that calls [assertParity]. Volatile values
 * (random ids, timings) are dropped; everything else, including property names and values, must match.
 */
internal data class WireEvent(val name: String, val properties: Map<String, Any>) {
    override fun toString() = "$name ${properties.toSortedMap()}"
}

internal object ParityHarness {
    private val volatileProperties = setOf("search_id", "flow_id", "duration_ms", "latency_ms", "elapsed_ms")

    /** Runs [scenario] with a recording transport installed as the global analytics service. */
    suspend fun TestScope.record(owner: String? = "fixture-owner", scenario: suspend () -> Unit): List<WireEvent> {
        val sdk = RecordingTransport()
        val service = analytics(MemoryConsent(AnalyticsConsent.Accepted), sdk)
        service.setForeground(true)
        service.restoreConsent()
        service.syncIdentity(owner)
        Analytics.install(service)
        try {
            scenario()
            runCurrent()
            return sdk.events.map { (name, properties) ->
                WireEvent(name, properties.filterKeys { it !in volatileProperties })
            }
        } finally {
            service.setConsent(AnalyticsConsent.Declined)
        }
    }

    fun assertParity(scenario: String, legacy: List<WireEvent>, migrated: List<WireEvent>) {
        // An empty journey would "match" vacuously, e.g. when analytics consent or installation is broken.
        if (legacy.isEmpty()) fail("Legacy reference emitted no events for \"$scenario\"; the scenario proves nothing")
        if (legacy == migrated) return
        fail(buildString {
            appendLine("Analytics parity broken for \"$scenario\"")
            appendLine("legacy (${legacy.size}):")
            legacy.forEachIndexed { i, e -> appendLine("  $i. $e") }
            appendLine("migrated (${migrated.size}):")
            migrated.forEachIndexed { i, e -> appendLine("  $i. $e") }
        })
    }
}
