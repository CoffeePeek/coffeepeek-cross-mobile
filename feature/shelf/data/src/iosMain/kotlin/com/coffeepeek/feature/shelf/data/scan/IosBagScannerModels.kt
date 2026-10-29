package com.coffeepeek.feature.shelf.data.scan

import com.coffeepeek.feature.shelf.domain.model.BagScanFailure
import com.coffeepeek.feature.shelf.domain.model.BagScanIssue
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readAvailable
import kotlinx.cinterop.*
import kotlinx.coroutines.CancellationException
import platform.CoreCrypto.*
import platform.Foundation.*
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fread
import platform.posix.fwrite

/** One model file, pinned by its size and SHA-256 (lowercase hex), and the HTTPS address a missing copy is fetched from. */
class IosBagScannerModel(val fileName: String, val sizeBytes: Long, val sha256: String, val url: String)

/** Paths of the model files for one scan; [inpainter] is null while MI-GAN is not downloaded yet. */
internal class IosScanModelFiles(val segmenter: String, val inpainter: String?)

/** Model files in [directory], excluded from backups; a file counts only after its checksum matched once. */
internal class IosBagScannerModels(
    private val directory: String,
    private val segmenter: IosBagScannerModel,
    private val inpainter: IosBagScannerModel,
    private val client: HttpClient,
) {
    fun missingBytes(): Long = listOf(segmenter, inpainter).filterNot(::isReady).sumOf { it.sizeBytes }

    fun files() = IosScanModelFiles(
        path(segmenter).takeIf { isReady(segmenter) } ?: throw BagScanFailure(BagScanIssue.ModelMissing),
        path(inpainter).takeIf { isReady(inpainter) },
    )

    @OptIn(ExperimentalForeignApi::class)
    suspend fun prepare(onProgress: (Long, Long) -> Unit) {
        val missing = listOf(segmenter, inpainter).filterNot(::isReady)
        val total = missing.sumOf { it.sizeBytes }
        var done = 0L
        if (missing.isNotEmpty()) createExcludedFromBackup(directory)
        for (model in missing) {
            val partial = "${path(model)}.part"
            val before = done
            client.download(model.url, partial) { written -> onProgress(before + written, total) }
            if (fileSize(partial) != model.sizeBytes || sha256(partial) != model.sha256) {
                NSFileManager.defaultManager.removeItemAtPath(partial, null)
                throw BagScanFailure(BagScanIssue.ModelDownload)
            }
            replace(partial, path(model))
            done += model.sizeBytes
        }
    }

    private fun path(model: IosBagScannerModel) = "$directory/${model.fileName}"
    private fun isReady(model: IosBagScannerModel) = fileSize(path(model)) == model.sizeBytes
}

/** Downloads [url] into [target]; an interrupted download resumes with a Range request. */
private suspend fun HttpClient.download(url: String, target: String, onBytes: (Long) -> Unit) {
    val offset = fileSize(target)
    try {
        prepareGet(url) { if (offset > 0) header(HttpHeaders.Range, "bytes=$offset-") }.execute { response ->
            val resumed = response.status == HttpStatusCode.PartialContent
            if (!resumed && response.status != HttpStatusCode.OK) throw BagScanFailure(BagScanIssue.ModelDownload)
            response.bodyAsChannel().writeTo(target, resumed, onBytes)
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: BagScanFailure) {
        throw failure
    } catch (_: Exception) {
        throw BagScanFailure(BagScanIssue.ModelDownload)
    }
}

/** Writes the channel to [path]; [onBytes] gets the file's total length so far. */
@OptIn(ExperimentalForeignApi::class)
private suspend fun ByteReadChannel.writeTo(path: String, append: Boolean, onBytes: (Long) -> Unit) {
    var written = if (append) fileSize(path) else 0L
    val file = fopen(path, if (append) "ab" else "wb") ?: throw BagScanFailure(BagScanIssue.ModelDownload)
    try {
        val buffer = ByteArray(64 * 1024)
        while (true) {
            val read = readAvailable(buffer, 0, buffer.size)
            if (read < 0) break
            val stored = buffer.usePinned { fwrite(it.addressOf(0), 1u, read.toULong(), file) }
            if (stored != read.toULong()) throw BagScanFailure(BagScanIssue.ModelDownload)
            written += read
            onBytes(written)
        }
    } finally {
        fclose(file)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun sha256(path: String): String = memScoped {
    val context = alloc<CC_SHA256_CTX>()
    CC_SHA256_Init(context.ptr)
    val file = fopen(path, "rb") ?: return ""
    try {
        val buffer = ByteArray(64 * 1024)
        buffer.usePinned { pinned ->
            while (true) {
                val read = fread(pinned.addressOf(0), 1u, buffer.size.toULong(), file).toInt()
                if (read <= 0) break
                CC_SHA256_Update(context.ptr, pinned.addressOf(0), read.toUInt())
            }
        }
    } finally {
        fclose(file)
    }
    val digest = UByteArray(CC_SHA256_DIGEST_LENGTH)
    digest.usePinned { CC_SHA256_Final(it.addressOf(0), context.ptr) }
    digest.joinToString("") { it.toString(16).padStart(2, '0') }
}

@OptIn(ExperimentalForeignApi::class)
private fun fileSize(path: String): Long =
    (NSFileManager.defaultManager.attributesOfItemAtPath(path, null)?.get(NSFileSize) as? NSNumber)?.longLongValue ?: 0L

@OptIn(ExperimentalForeignApi::class)
private fun replace(source: String, target: String) {
    NSFileManager.defaultManager.removeItemAtPath(target, null)
    check(NSFileManager.defaultManager.moveItemAtPath(source, target, null)) { "Could not move the model into place" }
}

/** Models are re-downloadable, so they stay out of iCloud and device backups. */
@OptIn(ExperimentalForeignApi::class)
private fun createExcludedFromBackup(directory: String) {
    NSFileManager.defaultManager.createDirectoryAtPath(directory, withIntermediateDirectories = true, attributes = null, error = null)
    NSURL.fileURLWithPath(directory).setResourceValue(true, NSURLIsExcludedFromBackupKey, null)
}
