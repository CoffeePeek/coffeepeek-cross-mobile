package com.coffeepeek.admin.analytics

import kotlin.test.*

class FeatureExposureLedgerTest {
    @Test fun shortScrollBackgroundAndCoveredViewsDoNotCount() {
        val ledger = FeatureExposureLedger()
        var sent = 0
        fun report(fraction: Float = .1f, foreground: Boolean = true, unobscured: Boolean = true, millis: Long = 500) =
            ledger.report("consent-1:visit-1", "search", "shops", fraction, foreground, unobscured, millis) { sent++; true }
        assertFalse(report(millis = 100))
        assertFalse(report(fraction = .09f))
        assertFalse(report(foreground = false))
        assertFalse(report(unobscured = false))
        assertEquals(0, sent)
        assertTrue(report())
        assertEquals(1, sent)
    }

    @Test fun responsiveCopiesRebindAndRotationShareOneImpressionButNewVisitDoesNot() {
        val ledger = FeatureExposureLedger()
        var sent = 0
        fun report(visit: String, accepted: Boolean = true) = ledger.report(visit, "favorites", "catalog_card", 1f, true, true, 500) {
            if (accepted) sent++
            accepted
        }
        assertFalse(report("consent1:visit1", false))
        assertTrue(report("consent1:visit1"))
        repeat(10) { assertFalse(report("consent1:visit1")) }
        assertTrue(report("consent1:visit2"))
        assertTrue(report("consent2:visit2"))
        assertEquals(3, sent)
    }
}
