package com.coffeepeek.admin.di.shelf

import android.content.Context
import com.coffeepeek.BuildConfig
import com.coffeepeek.feature.shelf.data.scan.BagScannerModel
import com.coffeepeek.feature.shelf.data.scan.BagScannerModelSource

/** U2-Net-p and MI-GAN for one build variant; checksums pin the exact files the probe was tuned with. */
internal class ShelfScannerModels(val segmenter: BagScannerModel, val inpainter: BagScannerModel)

/** U2-Net-p ships in the APK; MI-GAN (28 MB) is downloaded on the first scan from the build's SHELF_INPAINTER_URL. */
internal fun productionShelfModels(context: () -> Context) = shelfModels(
    BagScannerModelSource.asset(context, "shelf-scanner/$SEGMENTER_FILE"), BagScannerModelSource.https(BuildConfig.SHELF_INPAINTER_URL),
)

/** The pinned files, from wherever [segmenter] and [inpainter] fetch them. */
internal fun shelfModels(segmenter: BagScannerModelSource, inpainter: BagScannerModelSource) = ShelfScannerModels(
    BagScannerModel(SEGMENTER_FILE, SEGMENTER_BYTES, SEGMENTER_SHA256, segmenter),
    BagScannerModel(INPAINTER_FILE, INPAINTER_BYTES, INPAINTER_SHA256, inpainter),
)
