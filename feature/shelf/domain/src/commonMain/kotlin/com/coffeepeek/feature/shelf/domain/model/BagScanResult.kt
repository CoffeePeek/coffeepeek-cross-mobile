package com.coffeepeek.feature.shelf.domain.model

/** Milliseconds per pipeline stage. */
data class BagScanTimings(val segmentation: Long, val fit: Long, val repair: Long, val soften: Long, val total: Long)

/** Corners of the found front in the photo, as shares of its width and height, clockwise from top-left. */
data class BagScanFrame(val points: List<Pair<Float, Float>>)

/**
 * One scanned photo. [card] is the flat front ready to upload and is null when no front was found;
 * [quality] is null in that case too, and [moderation] then asks for a retake.
 */
class BagScanResult(
    val card: ShelfBagCard?,
    val cardWidth: Int,
    val cardHeight: Int,
    val frame: BagScanFrame?,
    val quality: PackQuality?,
    val moderation: PackModeration,
    val timings: BagScanTimings,
)

/** Whether the scanner can run now or first needs to fetch a model of [downloadBytes]. */
sealed interface BagScannerReadiness {
    data object Ready : BagScannerReadiness
    data class NeedsDownload(val downloadBytes: Long) : BagScannerReadiness
}

enum class BagScanIssue { ModelDownload, ModelMissing, UnreadablePhoto, OutOfMemory, Failed }

class BagScanFailure(val issue: BagScanIssue) : Exception("Bag scan: ${issue.name}")
