package com.coffeepeek.admin.di.shelf

import android.content.Context
import com.coffeepeek.BuildConfig
import com.coffeepeek.feature.shelf.data.scan.BagScannerModel
import com.coffeepeek.feature.shelf.data.scan.BagScannerModelSource
import com.coffeepeek.feature.shelf.data.scan.createBagScannerDownloadClient
import io.ktor.client.engine.okhttp.OkHttp

/** U2-Net-p and MI-GAN for one build variant; checksums pin the exact files the probe was tuned with. */
internal class ShelfScannerModels(val segmenter: BagScannerModel, val inpainter: BagScannerModel)

/** Both models download on first scan from the same URLs as iOS (GitHub + Hugging Face). */
internal fun productionShelfModels(@Suppress("UNUSED_PARAMETER") context: () -> Context): ShelfScannerModels {
    val client = createBagScannerDownloadClient(OkHttp.create())
    return shelfModels(
        BagScannerModelSource.https(BuildConfig.SHELF_SEGMENTER_URL, client),
        BagScannerModelSource.https(BuildConfig.SHELF_INPAINTER_URL, client),
    )
}

/** The pinned files, from wherever [segmenter] and [inpainter] fetch them. */
internal fun shelfModels(segmenter: BagScannerModelSource, inpainter: BagScannerModelSource) = ShelfScannerModels(
    BagScannerModel(SEGMENTER_FILE, SEGMENTER_BYTES, SEGMENTER_SHA256, segmenter),
    BagScannerModel(INPAINTER_FILE, INPAINTER_BYTES, INPAINTER_SHA256, inpainter),
)
