package com.coffeepeek.feature.shelf.data.scan

import android.content.Context
import com.coffeepeek.feature.shelf.domain.model.*
import com.coffeepeek.feature.shelf.domain.repository.BagScanner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * The Android [BagScanner]. [segmenter] and [inpainter] say where U2-Net-p and MI-GAN come from; application composition chooses them
 * (bundled asset, HTTPS download, or an adb-pushed file in the Sandbox).
 */
fun createAndroidBagScanner(context: () -> Context, segmenter: BagScannerModel, inpainter: BagScannerModel): BagScanner =
    AndroidBagScanner { BagScannerModels(java.io.File(context().noBackupFilesDir, "shelf-scanner"), segmenter, inpainter) }

/** One scan or download at a time: two MI-GAN sessions at once would double the memory peak. Creating it touches no files. */
private class AndroidBagScanner(store: () -> BagScannerModels) : BagScanner {
    private val models by lazy(store)
    private val lock = Mutex()
    private val pipeline = AndroidPackPipeline { models.files() }

    override suspend fun readiness(): BagScannerReadiness = withContext(Dispatchers.IO) {
        models.missingBytes().let { if (it == 0L) BagScannerReadiness.Ready else BagScannerReadiness.NeedsDownload(it) }
    }

    override suspend fun prepare(onProgress: (downloaded: Long, total: Long) -> Unit): Result<Unit> = scanResult {
        lock.withLock { withContext(Dispatchers.IO) { models.prepare(onProgress) } }
    }

    override suspend fun scan(photo: ByteArray, onStage: (BagScanStage) -> Unit, onFront: (BagScanFrame) -> Unit): Result<BagScanResult> =
        scanResult { lock.withLock { withContext(Dispatchers.Default) { pipeline.scan(photo, onStage, onFront) } } }
}

/** Running out of memory is a retake, never a crash; ONNX errors are reported without their native messages. */
private suspend fun <T> scanResult(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (failure: BagScanFailure) {
    Result.failure(failure)
} catch (_: OutOfMemoryError) {
    Result.failure(BagScanFailure(BagScanIssue.OutOfMemory))
} catch (_: Exception) {
    Result.failure(BagScanFailure(BagScanIssue.Failed))
}
