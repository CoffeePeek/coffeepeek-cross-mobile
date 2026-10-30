package com.coffeepeek.feature.shelf.data.scan

import com.coffeepeek.feature.shelf.domain.model.BagScanFailure
import com.coffeepeek.feature.shelf.domain.model.BagScanIssue
import kotlinx.coroutines.test.runTest
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import kotlin.test.*

class BagScannerModelsTest {
    private val root = Files.createTempDirectory("shelf-models").toFile()
    private val pushed = File(root, "pushed").apply { mkdirs() }
    private val store = File(root, "store")

    @AfterTest fun cleanUp() { root.deleteRecursively() }

    @Test fun missingModelsAreFetchedVerifiedAndThenReady() = runTest {
        val models = models(segmenter = push("u2netp.onnx", 10), inpainter = push("migan.onnx", 20))
        assertEquals(30, models.missingBytes())
        assertFailsWith<BagScanFailure> { models.files() }
        val progress = mutableListOf<Pair<Long, Long>>()
        models.prepare { done, total -> progress += done to total }
        assertEquals(0, models.missingBytes())
        assertEquals(30L to 30L, progress.last())
        assertEquals(File(store, "migan.onnx"), models.files().inpainter)
    }

    @Test fun aChecksumMismatchIsADownloadFailureAndLeavesNothingReady() = runTest {
        val good = push("u2netp.onnx", 10)
        val wrong = BagScannerModel("migan.onnx", 20, "0".repeat(64), BagScannerModelSource.directory { pushed })
        File(pushed, "migan.onnx").writeBytes(ByteArray(20) { 7 })
        val models = models(good, wrong)
        val failure = assertFailsWith<BagScanFailure> { models.prepare { _, _ -> } }
        assertEquals(BagScanIssue.ModelDownload, failure.issue)
        assertEquals(20, models.missingBytes())
        assertNull(models.files().inpainter)
        assertFalse(File(store, "migan.onnx.part").exists())
    }

    @Test fun aMissingSourceFileIsReportedAsAMissingModel() = runTest {
        val absent = BagScannerModel("migan.onnx", 20, "0".repeat(64), BagScannerModelSource.directory { pushed })
        val failure = assertFailsWith<BagScanFailure> { models(push("u2netp.onnx", 10), absent).prepare { _, _ -> } }
        assertEquals(BagScanIssue.ModelMissing, failure.issue)
    }

    private fun models(segmenter: BagScannerModel, inpainter: BagScannerModel) = BagScannerModels(store, segmenter, inpainter)

    private fun push(name: String, size: Int): BagScannerModel {
        val bytes = ByteArray(size) { it.toByte() }
        File(pushed, name).writeBytes(bytes)
        val sha = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        return BagScannerModel(name, size.toLong(), sha, BagScannerModelSource.directory { pushed })
    }
}
