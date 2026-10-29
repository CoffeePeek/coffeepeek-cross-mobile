package com.coffeepeek.feature.shelf.impl.ui.compose.component

import androidx.compose.runtime.Composable
import com.coffeepeek.feature.shelf.domain.model.BagScanIssue
import com.coffeepeek.feature.shelf.domain.model.BagScanStage
import com.coffeepeek.feature.shelf.domain.model.PackModerationReason
import com.coffeepeek.feature.shelf.impl.resources.*
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** What the scanner saw; the same nine reasons as the Sandbox probe. */
@Composable
internal fun PackModerationReason.text(): String = stringResource(
    when (this) {
        PackModerationReason.EdgesNotFound -> Res.string.shelf_reason_edges
        PackModerationReason.NotWholeBag -> Res.string.shelf_reason_whole
        PackModerationReason.TooFar -> Res.string.shelf_reason_far
        PackModerationReason.OutOfFrame -> Res.string.shelf_reason_frame
        PackModerationReason.ObjectsOnBag -> Res.string.shelf_reason_objects
        PackModerationReason.BagCovered -> Res.string.shelf_reason_covered
        PackModerationReason.Tilted -> Res.string.shelf_reason_tilted
        PackModerationReason.MoveCloser -> Res.string.shelf_reason_closer
        PackModerationReason.Glare -> Res.string.shelf_reason_glare
    },
)

/** How to fix a retake reason; hint reasons already read as advice and have none. */
@Composable
internal fun PackModerationReason.tip(): String? = tipText(this)?.let { stringResource(it) }

private fun tipText(reason: PackModerationReason): StringResource? = when (reason) {
    PackModerationReason.EdgesNotFound -> Res.string.shelf_tip_edges
    PackModerationReason.NotWholeBag -> Res.string.shelf_tip_whole
    PackModerationReason.TooFar -> Res.string.shelf_tip_far
    PackModerationReason.OutOfFrame -> Res.string.shelf_tip_frame
    PackModerationReason.ObjectsOnBag -> Res.string.shelf_tip_objects
    PackModerationReason.BagCovered -> Res.string.shelf_tip_covered
    PackModerationReason.Tilted, PackModerationReason.MoveCloser, PackModerationReason.Glare -> null
}

@Composable
internal fun BagScanIssue.text(): String = stringResource(
    when (this) {
        BagScanIssue.UnreadablePhoto -> Res.string.shelf_scan_failed_photo
        BagScanIssue.OutOfMemory -> Res.string.shelf_scan_failed_memory
        BagScanIssue.ModelDownload, BagScanIssue.ModelMissing -> Res.string.shelf_scan_failed_model
        BagScanIssue.Failed -> Res.string.shelf_scan_failed
    },
)

/** "Ищем пачку" while it runs or was never reached, "Нашли пачку" once it has passed. */
@Composable
internal fun BagScanStage.text(passed: Boolean): String = stringResource(
    when (this) {
        BagScanStage.Segmentation -> if (passed) Res.string.shelf_scan_stage_found else Res.string.shelf_scan_stage_found_running
        BagScanStage.Front -> if (passed) Res.string.shelf_scan_stage_front else Res.string.shelf_scan_stage_front_running
        BagScanStage.Repair -> if (passed) Res.string.shelf_scan_stage_repair else Res.string.shelf_scan_stage_repair_running
        BagScanStage.Soften -> if (passed) Res.string.shelf_scan_stage_soften else Res.string.shelf_scan_stage_soften_running
    },
)
