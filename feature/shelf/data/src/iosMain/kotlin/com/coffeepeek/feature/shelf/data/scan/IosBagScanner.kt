package com.coffeepeek.feature.shelf.data.scan

import com.coffeepeek.feature.shelf.domain.model.*
import com.coffeepeek.feature.shelf.domain.repository.BagScanner
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.HttpTimeout
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

/**
 * The iOS [BagScanner]. [segmenter] and [inpainter] say which U2-Net-p and MI-GAN files to use and where a missing one is
 * downloaded from; application composition chooses them. Creating it touches no files and no network.
 */
fun createIosBagScanner(segmenter: IosBagScannerModel, inpainter: IosBagScannerModel): BagScanner = IosBagScanner {
    val client = HttpClient(Darwin) {
        install(HttpTimeout) {
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 30_000
        }
    }
    IosBagScannerModels("${applicationSupport()}/shelf-scanner", segmenter, inpainter, client)
}

/** One scan or download at a time: two MI-GAN sessions at once would double the memory peak. */
internal class IosBagScanner(store: () -> IosBagScannerModels) : BagScanner {
    private val models by lazy(store)
    private val lock = Mutex()
    private val pipeline = PackPipeline(IosPackImaging, OrtPackNetworks { models.files() })

    override suspend fun readiness(): BagScannerReadiness = withContext(Dispatchers.IO) {
        models.missingBytes().let { if (it == 0L) BagScannerReadiness.Ready else BagScannerReadiness.NeedsDownload(it) }
    }

    override suspend fun prepare(onProgress: (downloaded: Long, total: Long) -> Unit): Result<Unit> = scanResult {
        lock.withLock { withContext(Dispatchers.IO) { models.prepare(onProgress) } }
    }

    override suspend fun scan(photo: ByteArray, onStage: (BagScanStage) -> Unit, onFront: (BagScanFrame) -> Unit): Result<BagScanResult> =
        scanResult { lock.withLock { withContext(Dispatchers.Default) { pipeline.scan(photo, onStage, onFront) } } }
}

/** ONNX Runtime and Core Graphics errors are reported without their native messages. */
private suspend fun <T> scanResult(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (failure: BagScanFailure) {
    Result.failure(failure)
} catch (_: Exception) {
    Result.failure(BagScanFailure(BagScanIssue.Failed))
}

@OptIn(ExperimentalForeignApi::class)
private fun applicationSupport(): String =
    NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, true).first() as String
