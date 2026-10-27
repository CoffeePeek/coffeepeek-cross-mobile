package com.coffeepeek.admin.di.shelf

import android.content.Context
import com.coffeepeek.feature.shelf.data.scan.BagScannerModel
import com.coffeepeek.feature.shelf.data.scan.BagScannerModelSource

/** U2-Net-p and MI-GAN for one build variant; checksums pin the exact files the probe was tuned with. */
internal class ShelfScannerModels(val segmenter: BagScannerModel, val inpainter: BagScannerModel)

internal const val SEGMENTER_FILE = "u2netp.onnx"
internal const val INPAINTER_FILE = "migan_pipeline_v2.onnx"
private const val SEGMENTER_BYTES = 4_574_861L
private const val SEGMENTER_SHA256 = "309c8469258dda742793dce0ebea8e6dd393174f89934733ecc8b14c76f4ddd8"
private const val INPAINTER_BYTES = 28_079_181L
private const val INPAINTER_SHA256 = "6f1f3530a1a2324b19752018ce756088b07973cda8d7d890034ace5c8a48c40b"
private const val INPAINTER_URL = "https://huggingface.co/andraniksargsyan/migan/resolve/main/$INPAINTER_FILE"

/** U2-Net-p ships in the APK; MI-GAN (28 MB) is downloaded on the first scan. */
internal fun productionShelfModels(context: () -> Context) = ShelfScannerModels(
    BagScannerModel(SEGMENTER_FILE, SEGMENTER_BYTES, SEGMENTER_SHA256, BagScannerModelSource.asset(context, "shelf-scanner/$SEGMENTER_FILE")),
    BagScannerModel(INPAINTER_FILE, INPAINTER_BYTES, INPAINTER_SHA256, BagScannerModelSource.https(INPAINTER_URL)),
)
