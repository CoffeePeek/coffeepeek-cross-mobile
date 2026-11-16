package com.coffeepeek.feature.shelf.data.scan

import com.coffeepeek.feature.shelf.domain.model.*
import com.coffeepeek.feature.shelf.domain.repository.BagScanner

/** Local xcross builds omit ONNX until its native import is supported on Windows. */
@Suppress("UNUSED_PARAMETER")
fun createIosBagScanner(segmenter: IosBagScannerModel, inpainter: IosBagScannerModel): BagScanner = DisabledIosBagScanner

private object DisabledIosBagScanner : BagScanner {
    override suspend fun readiness(): BagScannerReadiness = BagScannerReadiness.Disabled

    override suspend fun prepare(onProgress: (Long, Long) -> Unit): Result<Unit> =
        Result.failure(BagScanFailure(BagScanIssue.Failed))

    override suspend fun scan(photo: ByteArray, onStage: (BagScanStage) -> Unit, onFront: (BagScanFrame) -> Unit): Result<BagScanResult> =
        Result.failure(BagScanFailure(BagScanIssue.Failed))
}
