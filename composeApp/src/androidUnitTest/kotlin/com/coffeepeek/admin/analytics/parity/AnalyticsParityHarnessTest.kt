package com.coffeepeek.admin.analytics.parity

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** The harness itself must fail on drift, otherwise every parity test would pass vacuously. */
class AnalyticsParityHarnessTest {
    private val base = listOf(WireEvent("favorite_changed", mapOf("entity_type" to "roaster", "is_favorite" to true)))

    @Test fun identicalJourneysMatch() = ParityHarness.assertParity("same", base, base.toList())

    @Test fun differentPropertyValueFails() {
        val drifted = listOf(WireEvent("favorite_changed", mapOf("entity_type" to "coffee_shop", "is_favorite" to true)))
        val error = assertFailsWith<AssertionError> { ParityHarness.assertParity("drift", base, drifted) }
        assertTrue("legacy (1)" in error.message.orEmpty() && "migrated (1)" in error.message.orEmpty())
    }

    @Test fun missingEventFails() {
        assertFailsWith<AssertionError> { ParityHarness.assertParity("missing", base, emptyList()) }
    }

    @Test fun silentLegacyReferenceFails() {
        assertFailsWith<AssertionError> { ParityHarness.assertParity("silent", emptyList(), emptyList()) }
    }
}
