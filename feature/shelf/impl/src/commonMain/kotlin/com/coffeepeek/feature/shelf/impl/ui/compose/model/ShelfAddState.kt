package com.coffeepeek.feature.shelf.impl.ui.compose.model

import com.coffeepeek.feature.shelf.api.ShelfCoffeeOption
import com.coffeepeek.feature.shelf.api.ShelfPhoto
import com.coffeepeek.feature.shelf.domain.model.*
import com.coffeepeek.feature.shelf.impl.ui.data.ShelfDay

/**
 * Get the scanner ready, take or pick a photo, watch it being scanned, then either retake it or see the card peel off the
 * photo ([Revealing]) and fill in the bag.
 */
internal enum class ShelfAddStep { Intro, Camera, Scanning, Revealing, Retake, Form }

/** The scanner's models: U2-Net-p is bundled, MI-GAN is downloaded once before the first scan. */
internal sealed interface ShelfScannerStatus {
    data object Checking : ShelfScannerStatus
    data object Ready : ShelfScannerStatus
    data class NeedsDownload(val bytes: Long) : ShelfScannerStatus
    data class Downloading(val done: Long, val total: Long) : ShelfScannerStatus
    data object DownloadFailed : ShelfScannerStatus
}

/** The form as typed. A chosen catalogue [coffee] wins over [query]; text alone is the bag's own name. */
internal data class ShelfBagForm(
    val query: String = "",
    val coffee: ShelfCoffeeOption? = null,
    val roaster: String = "",
    val weightGrams: Int? = null,
    val grind: ShelfGrind? = null,
    val roastPurpose: ShelfRoastPurpose? = null,
    val roastedOn: ShelfDay? = null,
) {
    fun details() = ShelfBagDetails(
        bean = coffee?.let { ShelfBean.Catalog(it.slug) } ?: query.trim().takeIf(String::isNotEmpty)?.let(ShelfBean::Custom) ?: ShelfBean.Unknown,
        roasterName = roaster.trim().ifEmpty { null },
        weightGrams = weightGrams,
        grind = grind,
        roastPurpose = roastPurpose,
        roastedOn = roastedOn?.iso(),
    )
}

/**
 * [result] is the last scan; [failure] is set instead when the scan could not run at all. [options] are catalogue matches
 * for the coffee field; [confirmDuplicate] asks before a second bag of a catalogue coffee already on the shelf.
 */
internal data class ShelfAddState(
    val today: ShelfDay,
    val step: ShelfAddStep = ShelfAddStep.Intro,
    val scanner: ShelfScannerStatus = ShelfScannerStatus.Checking,
    val photo: ShelfPhoto? = null,
    val stagesDone: Set<BagScanStage> = emptySet(),
    /** The found front, outlined on the photo while the rest of the scan runs. */
    val liveFrame: BagScanFrame? = null,
    val result: BagScanResult? = null,
    val failure: BagScanIssue? = null,
    val form: ShelfBagForm = ShelfBagForm(),
    val options: List<ShelfCoffeeOption> = emptyList(),
    val saving: Boolean = false,
    val confirmDuplicate: Boolean = false,
    /** The card has just flown off the scan and still has to drop into its place in the form. */
    val cardLanding: Boolean = false,
    val message: ShelfAddMessage? = null,
) {
    /** Advice that does not block saving ("Принято с подсказкой"). */
    val hints: List<PackModerationReason>
        get() = result?.moderation?.takeIf { it.status == PackModerationStatus.AcceptedWithHint }?.reasons.orEmpty()
}

internal sealed interface ShelfAddMessage {
    data object PhotoUnreadable : ShelfAddMessage
    data class Failed(val issue: ShelfIssue) : ShelfAddMessage
}

internal fun ShelfDay.iso(): String =
    "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"

/** How one stage went, known only once the scan has a verdict. */
internal enum class ShelfStageStatus { Done, Warning, Failed, Skipped }

internal data class ShelfStageOutcome(val stage: BagScanStage, val status: ShelfStageStatus, val reasons: List<PackModerationReason>)

/**
 * The finished scan stage by stage: each moderation reason marks the stage that produced it (a retake reason fails it,
 * a hint warns), a scan that broke off fails the stage it was in, and stages it never reached are skipped.
 */
internal fun ShelfAddState.stageOutcomes(): List<ShelfStageOutcome> {
    val moderation = result?.moderation
    val brokeOffAt = failure?.let { BagScanStage.entries.firstOrNull { it !in stagesDone } }
    return BagScanStage.entries.map { stage ->
        val own = moderation?.reasons.orEmpty().filter { it.stage == stage }
        val status = when {
            stage == brokeOffAt -> ShelfStageStatus.Failed
            own.isNotEmpty() -> if (moderation?.status == PackModerationStatus.Retake) ShelfStageStatus.Failed else ShelfStageStatus.Warning
            stage !in stagesDone -> ShelfStageStatus.Skipped
            else -> ShelfStageStatus.Done
        }
        ShelfStageOutcome(stage, status, own)
    }
}

private val PackModerationReason.stage: BagScanStage
    get() = when (this) {
        PackModerationReason.NotWholeBag, PackModerationReason.TooFar, PackModerationReason.OutOfFrame,
        PackModerationReason.MoveCloser -> BagScanStage.Segmentation
        PackModerationReason.EdgesNotFound, PackModerationReason.Tilted -> BagScanStage.Front
        PackModerationReason.ObjectsOnBag, PackModerationReason.BagCovered -> BagScanStage.Repair
        PackModerationReason.Glare -> BagScanStage.Soften
    }
