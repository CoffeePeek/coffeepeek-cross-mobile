package com.coffeepeek.feature.shelf.data.scan

import com.coffeepeek.feature.shelf.domain.model.BagScanFailure
import com.coffeepeek.feature.shelf.domain.model.BagScanIssue
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.test.runTest
import platform.Foundation.*
import kotlin.test.*

@OptIn(ExperimentalForeignApi::class)
class IosBagScannerModelsTest {
    private val root = "${NSTemporaryDirectory()}shelf-models-${NSUUID().UUIDString}"
    private val requests = mutableListOf<Pair<String, String?>>()

    @AfterTest fun cleanUp() {
        NSFileManager.defaultManager.removeItemAtPath(root, null)
    }

    @Test fun missingModelsAreDownloadedVerifiedAndThenReady() = runTest {
        val models = models(served = mapOf("/u2netp" to "abc", "/migan" to "hello"))
        assertEquals(8, models.missingBytes())
        assertFailsWith<BagScanFailure> { models.files() }
        val progress = mutableListOf<Pair<Long, Long>>()
        models.prepare { done, total -> progress += done to total }
        assertEquals(0, models.missingBytes())
        assertEquals(8L to 8L, progress.last())
        assertEquals("$root/migan.onnx", models.files().inpainter)
        assertEquals("hello", NSString.stringWithContentsOfFile("$root/migan.onnx", NSUTF8StringEncoding, null))
    }

    @Test fun aChecksumMismatchIsADownloadFailureAndLeavesNothingReady() = runTest {
        val models = models(served = mapOf("/u2netp" to "abc", "/migan" to "jello"))
        val failure = assertFailsWith<BagScanFailure> { models.prepare { _, _ -> } }
        assertEquals(BagScanIssue.ModelDownload, failure.issue)
        assertEquals(5, models.missingBytes())
        assertNull(models.files().inpainter)
        assertFalse(NSFileManager.defaultManager.fileExistsAtPath("$root/migan.onnx.part"))
    }

    @Test fun anInterruptedDownloadResumesWhereItStopped() = runTest {
        NSFileManager.defaultManager.createDirectoryAtPath(root, true, null, null)
        NSFileManager.defaultManager.createFileAtPath("$root/migan.onnx.part", "he".encodeToNSData(), null)
        val models = models(served = mapOf("/u2netp" to "abc", "/migan" to "hello"))
        models.prepare { _, _ -> }
        assertEquals("bytes=2-", requests.single { it.first == "/migan" }.second)
        assertEquals(0, models.missingBytes())
    }

    @Test fun anHttpErrorIsADownloadFailure() = runTest {
        val failure = assertFailsWith<BagScanFailure> { models(served = emptyMap()).prepare { _, _ -> } }
        assertEquals(BagScanIssue.ModelDownload, failure.issue)
    }

    /** Serves [served] bodies by path; a Range request gets the rest of the body as partial content. */
    private fun models(served: Map<String, String>): IosBagScannerModels {
        val client = HttpClient(MockEngine { request ->
            val path = request.url.encodedPath
            val range = request.headers[HttpHeaders.Range]
            requests += path to range
            val body = served[path] ?: return@MockEngine respond("", HttpStatusCode.NotFound)
            val from = range?.removePrefix("bytes=")?.removeSuffix("-")?.toInt()
            if (from == null) respond(body, HttpStatusCode.OK) else respond(body.substring(from), HttpStatusCode.PartialContent)
        })
        return IosBagScannerModels(root,
            IosBagScannerModel("u2netp.onnx", 3, ABC_SHA256, "https://models.invalid/u2netp"),
            IosBagScannerModel("migan.onnx", 5, HELLO_SHA256, "https://models.invalid/migan"), client)
    }
}

private const val ABC_SHA256 = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"
private const val HELLO_SHA256 = "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824"

@OptIn(ExperimentalForeignApi::class)
private fun String.encodeToNSData(): NSData? = (this as NSString).dataUsingEncoding(NSUTF8StringEncoding)
