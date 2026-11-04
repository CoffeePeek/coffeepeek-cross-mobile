package com.coffeepeek.feature.shelf.domain.usecase

import com.coffeepeek.feature.shelf.domain.model.PackModeration
import com.coffeepeek.feature.shelf.domain.model.PackModerationReason.*
import com.coffeepeek.feature.shelf.domain.model.PackModerationStatus.*
import com.coffeepeek.feature.shelf.domain.model.PackQuality
import kotlin.test.Test
import kotlin.test.assertEquals

class PackModerationRulesTest {
    private val good = PackQuality(sideSupport = 0.85, aspect = 0.75, angleDeviation = 4.0, quadShare = 0.5,
        borderTouch = 0.0, clutterOutside = 0.0, repaintShare = 0.02, strongGlare = 0.0)

    @Test fun cleanScanIsAccepted() = assertEquals(PackModeration(Accepted, emptyList()), moderate(good))

    @Test fun eachHardSignalAsksForARetakeAtItsThreshold() {
        val cases = mapOf(
            good.copy(sideSupport = 0.64) to EdgesNotFound, good.copy(angleDeviation = 20.1) to EdgesNotFound,
            good.copy(aspect = 0.39) to NotWholeBag, good.copy(aspect = 1.36) to NotWholeBag,
            good.copy(quadShare = 0.079) to TooFar, good.copy(borderTouch = 0.26) to OutOfFrame,
            good.copy(clutterOutside = 0.051) to ObjectsOnBag, good.copy(repaintShare = 0.101) to BagCovered,
        )
        cases.forEach { (quality, reason) -> assertEquals(PackModeration(Retake, listOf(reason)), moderate(quality), "$reason") }
    }

    @Test fun boundaryValuesStillPass() {
        val edge = good.copy(sideSupport = 0.65, angleDeviation = 12.0, aspect = 1.35, quadShare = 0.2, borderTouch = 0.25,
            clutterOutside = 0.05, repaintShare = 0.10, strongGlare = 0.2)
        assertEquals(Accepted, moderate(edge).status)
    }

    @Test fun retakeReasonsKeepReadingOrderAndHideHints() {
        val result = moderate(good.copy(sideSupport = 0.56, aspect = 1.77, angleDeviation = 31.0, strongGlare = 0.5))
        assertEquals(PackModeration(Retake, listOf(EdgesNotFound, NotWholeBag)), result)
    }

    @Test fun softSignalsOnlyHint() {
        val result = moderate(good.copy(angleDeviation = 13.0, quadShare = 0.1, strongGlare = 0.44))
        assertEquals(PackModeration(AcceptedWithHint, listOf(Tilted, MoveCloser, Glare)), result)
    }
}
