package com.coffeepeek.feature.contentreport.domain

import com.coffeepeek.feature.contentreport.domain.model.validReportReason
import kotlin.test.*

class ReportReasonTest {
    @Test fun blankAndWhitespaceAreRejected() {
        assertFalse(validReportReason(""))
        assertFalse(validReportReason(" \n\t "))
    }
    @Test fun bothBoundsAndTrimmingMatchTheLegacyContract() {
        assertTrue(validReportReason(" x "))
        assertTrue(validReportReason("  ${"x".repeat(2000)}  "))
        assertFalse(validReportReason("x".repeat(2001)))
    }
}
