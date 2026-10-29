package com.coffeepeek.feature.shelf.impl.ui.compose.model

import com.coffeepeek.feature.shelf.domain.model.BagScanIssue
import com.coffeepeek.feature.shelf.domain.model.BagScanStage
import com.coffeepeek.feature.shelf.domain.model.BagScanStage.*
import com.coffeepeek.feature.shelf.domain.model.PackModerationReason
import com.coffeepeek.feature.shelf.domain.model.PackModerationStatus
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfStageStatus.*
import com.coffeepeek.feature.shelf.impl.ui.data.ShelfDay
import com.coffeepeek.feature.shelf.impl.ui.scanResult
import kotlin.test.Test
import kotlin.test.assertEquals

class ShelfStageOutcomesTest {
    private fun outcomes(done: Set<BagScanStage>, status: PackModerationStatus? = null, reasons: List<PackModerationReason> = emptyList(),
        failure: BagScanIssue? = null) =
        ShelfAddState(ShelfDay(2026, 10, 27), stagesDone = done, result = status?.let { scanResult(it, reasons) }, failure = failure)
            .stageOutcomes().map { it.status }

    @Test fun aRetakeReasonFailsTheStageThatProducedIt() {
        assertEquals(listOf(Done, Done, Failed, Done),
            outcomes(BagScanStage.entries.toSet(), PackModerationStatus.Retake, listOf(PackModerationReason.ObjectsOnBag)))
    }

    @Test fun noFrontFailsAlignmentAndSkipsWhatNeverRan() {
        assertEquals(listOf(Done, Failed, Skipped, Skipped),
            outcomes(setOf(Segmentation, Front), PackModerationStatus.Retake, listOf(PackModerationReason.EdgesNotFound)))
    }

    @Test fun hintsOnlyWarn() {
        assertEquals(listOf(Done, Warning, Done, Warning), outcomes(BagScanStage.entries.toSet(), PackModerationStatus.AcceptedWithHint,
            listOf(PackModerationReason.Tilted, PackModerationReason.Glare)))
    }

    @Test fun aScanThatBrokeOffFailsTheStageItWasIn() {
        assertEquals(listOf(Done, Failed, Skipped, Skipped), outcomes(setOf(Segmentation), failure = BagScanIssue.OutOfMemory))
    }
}
