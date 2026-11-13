package com.coffeepeek.feature.homecheckin.data.mapper

import com.coffeepeek.feature.homecheckin.data.backend.HomeDto
import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationModeration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HomePreparationRejectionTest {
    @Test fun reasonSurvivesOwnerReadAndMissingReasonSupportsOlderServer() {
        val dto = HomeDto("id", "2026-11-14T09:00:00Z", "2026-11-14",
            visibility = "Public", moderationState = "Rejected", rejectionReason = "Fix the text")
        val rejected = dto.toDomain("https://example.invalid")
        assertEquals(HomePreparationModeration.Rejected, rejected.moderation)
        assertEquals("Fix the text", rejected.rejectionReason)
        assertNull(dto.copy(rejectionReason = null).toDomain("https://example.invalid").rejectionReason)
    }
}
