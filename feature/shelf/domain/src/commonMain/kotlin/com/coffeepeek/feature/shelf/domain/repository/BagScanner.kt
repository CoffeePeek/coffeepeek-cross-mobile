package com.coffeepeek.feature.shelf.domain.repository

import com.coffeepeek.feature.shelf.domain.model.BagScanResult
import com.coffeepeek.feature.shelf.domain.model.BagScanStage
import com.coffeepeek.feature.shelf.domain.model.BagScannerReadiness

/** Platform scanner: photo of a bag -> flat front card plus pre-moderation. Failures are [com.coffeepeek.feature.shelf.domain.model.BagScanFailure]. */
interface BagScanner {
    suspend fun readiness(): BagScannerReadiness

    /** Fetches missing models; [onProgress] gets downloaded and total bytes. Safe to call again after a failure. */
    suspend fun prepare(onProgress: (downloaded: Long, total: Long) -> Unit = { _, _ -> }): Result<Unit>

    /** [photo] is an encoded image (JPEG, HEIC, PNG…) as picked or captured; [onStage] hears each finished stage, on any thread. */
    suspend fun scan(photo: ByteArray, onStage: (BagScanStage) -> Unit = {}): Result<BagScanResult>
}
