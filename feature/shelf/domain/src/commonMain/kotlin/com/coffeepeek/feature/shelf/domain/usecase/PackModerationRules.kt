package com.coffeepeek.feature.shelf.domain.usecase

import com.coffeepeek.feature.shelf.domain.model.PackModeration
import com.coffeepeek.feature.shelf.domain.model.PackModerationReason
import com.coffeepeek.feature.shelf.domain.model.PackModerationStatus
import com.coffeepeek.feature.shelf.domain.model.PackQuality

/** A scan whose front could not be found at all. */
val PackNotFound = PackModeration(PackModerationStatus.Retake, listOf(PackModerationReason.EdgesNotFound))

/**
 * Accept / hint / retake from [quality]. Thresholds come from 19 sample photos and 2 negatives, so they
 * are a starting point to calibrate on logged scan metrics, not final values.
 */
fun moderate(quality: PackQuality): PackModeration {
    val retake = buildList {
        if (quality.sideSupport < 0.65 || quality.angleDeviation > 20) add(PackModerationReason.EdgesNotFound)
        if (quality.aspect !in 0.4..1.35) add(PackModerationReason.NotWholeBag)
        if (quality.quadShare < 0.08) add(PackModerationReason.TooFar)
        if (quality.borderTouch > 0.25) add(PackModerationReason.OutOfFrame)
        if (quality.clutterOutside > 0.05) add(PackModerationReason.ObjectsOnBag)
        if (quality.repaintShare > 0.10) add(PackModerationReason.BagCovered)
    }
    if (retake.isNotEmpty()) return PackModeration(PackModerationStatus.Retake, retake)
    val hints = buildList {
        if (quality.angleDeviation > 12) add(PackModerationReason.Tilted)
        if (quality.quadShare < 0.2) add(PackModerationReason.MoveCloser)
        if (quality.strongGlare > 0.2) add(PackModerationReason.Glare)
    }
    return PackModeration(if (hints.isEmpty()) PackModerationStatus.Accepted else PackModerationStatus.AcceptedWithHint, hints)
}
