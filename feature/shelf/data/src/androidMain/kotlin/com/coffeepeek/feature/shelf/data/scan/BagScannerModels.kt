package com.coffeepeek.feature.shelf.data.scan

import android.content.Context
import com.coffeepeek.feature.shelf.domain.model.BagScanFailure
import com.coffeepeek.feature.shelf.domain.model.BagScanIssue
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.File
import java.io.InputStream
import java.security.MessageDigest

/** One model file and where a missing copy comes from. [sha256] is lowercase hex. */
class BagScannerModel(val fileName: String, val sizeBytes: Long, val sha256: String, val source: BagScannerModelSource)

/** Writes the bytes of model [fileName] to [target], continuing a partial [target] when it can. */
fun interface BagScannerModelSource {
    suspend fun fetch(fileName: String, target: File, onBytes: (Long) -> Unit)

    companion object {
        /** Bundled in the APK's assets. */
        fun asset(context: () -> Context, path: String) = BagScannerModelSource { _, target, onBytes ->
            target.delete()
            context().assets.open(path).use { it.copyTo(target, append = false, onBytes) }
        }

        /** Downloaded once over HTTPS (follows redirects like Hugging Face → CDN); resumes with Range when interrupted. */
        fun https(url: String, client: HttpClient) = BagScannerModelSource { _, target, onBytes ->
            client.download(url, target, onBytes)
        }

        /** Copied from a directory, e.g. a file pushed with adb into the Sandbox, which has no network. */
        fun directory(directory: () -> File?) = BagScannerModelSource { fileName, target, onBytes ->
            val source = directory()?.let { File(it, fileName) }?.takeIf(File::isFile) ?: throw BagScanFailure(BagScanIssue.ModelMissing)
            target.delete()
            source.inputStream().use { it.copyTo(target, append = false, onBytes) }
        }
    }
}

/** Plain HTTP client for scanner models only — not the authenticated API client. */
fun createBagScannerDownloadClient(engine: HttpClientEngine): HttpClient = HttpClient(engine) {
    expectSuccess = false
}

/** Model files for one scan; [inpainter] is null while MI-GAN is not downloaded yet. */
internal class ScanModelFiles(val segmenter: File, val inpainter: File?)

/** Model files in no-backup storage; a file counts only after its checksum matched once. */
internal class BagScannerModels(private val directory: File, private val segmenter: BagScannerModel, private val inpainter: BagScannerModel) {

    fun missingBytes(): Long = listOf(segmenter, inpainter).filterNot(::isReady).sumOf { it.sizeBytes }

    fun files() = ScanModelFiles(
        file(segmenter).takeIf { isReady(segmenter) } ?: throw BagScanFailure(BagScanIssue.ModelMissing),
        file(inpainter).takeIf { isReady(inpainter) },
    )

    suspend fun prepare(onProgress: (Long, Long) -> Unit) {
        val missing = listOf(segmenter, inpainter).filterNot(::isReady)
        val total = missing.sumOf { it.sizeBytes }
        var done = 0L
        for (model in missing) {
            val partial = File(directory.apply { mkdirs() }, "${model.fileName}.part")
            val before = done
            model.source.fetch(model.fileName, partial) { written -> onProgress(before + written, total) }
            if (partial.length() != model.sizeBytes || partial.sha256() != model.sha256) {
                partial.delete()
                throw BagScanFailure(BagScanIssue.ModelDownload)
            }
            check(partial.renameTo(file(model)))
            done += model.sizeBytes
        }
    }

    private fun file(model: BagScannerModel) = File(directory, model.fileName)
    private fun isReady(model: BagScannerModel) = file(model).length() == model.sizeBytes
}

private suspend fun HttpClient.download(url: String, target: File, onBytes: (Long) -> Unit) {
    val offset = target.length()
    try {
        prepareGet(url) {
            header(HttpHeaders.UserAgent, "CoffeePeek")
            if (offset > 0) header(HttpHeaders.Range, "bytes=$offset-")
        }.execute { response ->
            val resumed = response.status == HttpStatusCode.PartialContent
            if (!resumed && response.status != HttpStatusCode.OK) throw BagScanFailure(BagScanIssue.ModelDownload)
            response.bodyAsChannel().copyToFile(target, resumed, onBytes)
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: BagScanFailure) {
        throw failure
    } catch (_: Exception) {
        throw BagScanFailure(BagScanIssue.ModelDownload)
    }
}

/** Copies with cancellation checks; [onBytes] gets the file's total length so far. */
private suspend fun io.ktor.utils.io.ByteReadChannel.copyToFile(target: File, append: Boolean, onBytes: (Long) -> Unit) {
    var written = if (append) target.length() else 0L
    java.io.FileOutputStream(target, append).use { out ->
        val buffer = ByteArray(64 * 1024)
        while (true) {
            currentCoroutineContext().ensureActive()
            val read = readAvailable(buffer, 0, buffer.size)
            if (read <= 0) break
            out.write(buffer, 0, read)
            written += read
            onBytes(written)
        }
    }
}

private suspend fun InputStream.copyTo(target: File, append: Boolean, onBytes: (Long) -> Unit) {
    var written = if (append) target.length() else 0L
    java.io.FileOutputStream(target, append).use { out ->
        val buffer = ByteArray(64 * 1024)
        while (true) {
            currentCoroutineContext().ensureActive()
            val read = read(buffer)
            if (read < 0) break
            out.write(buffer, 0, read)
            written += read
            onBytes(written)
        }
    }
}

private fun File.sha256(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    inputStream().use { input ->
        val buffer = ByteArray(64 * 1024)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            digest.update(buffer, 0, read)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}
