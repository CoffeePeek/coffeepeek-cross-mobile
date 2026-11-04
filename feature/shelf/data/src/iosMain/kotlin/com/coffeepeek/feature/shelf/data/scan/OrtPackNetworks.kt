package com.coffeepeek.feature.shelf.data.scan

import com.coffeepeek.feature.shelf.domain.model.BagScanFailure
import com.coffeepeek.feature.shelf.domain.model.BagScanIssue
import kotlinx.cinterop.*
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSMutableData
import platform.Foundation.NSNumber
import platform.Foundation.dataWithLength
import platform.posix.memcpy
import swiftPMImport.CoffeePeek.feature.shelf.feature.shelf.data.*
import kotlin.native.runtime.GC
import kotlin.native.runtime.NativeRuntimeApi

/** U2-Net-p and MI-GAN on ONNX Runtime's Objective-C API; [files] resolves the model files when a network is first needed. */
internal class OrtPackNetworks(private val files: () -> IosScanModelFiles) : PackNetworks {
    @OptIn(ExperimentalForeignApi::class)
    override fun segment(input: FloatArray): FloatArray {
        val shape = listOf(1, 3, SEGMENTER_SIDE, SEGMENTER_SIDE)
        val tensor = OrtInput(null, input.data(), ORTTensorElementDataType.ORTTensorElementDataTypeFloat, shape)
        return run(files().segmenter, listOf(tensor)) { it.toFloatArray() }
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun inpaint(image: ByteArray, known: ByteArray, width: Int, height: Int): ByteArray {
        val model = files().inpainter ?: throw BagScanFailure(BagScanIssue.ModelMissing)
        val inputs = listOf(
            OrtInput("image", image.data(), ORTTensorElementDataType.ORTTensorElementDataTypeUInt8, listOf(1, 3, height, width)),
            OrtInput("mask", known.data(), ORTTensorElementDataType.ORTTensorElementDataTypeUInt8, listOf(1, 1, height, width)),
        )
        return run(model, inputs) { it.toByteArray() }
    }
}

/** One input tensor; a null [name] is the model's first input. */
@OptIn(ExperimentalForeignApi::class)
private class OrtInput(val name: String?, val data: NSMutableData, val type: ORTTensorElementDataType, val shape: List<Int>)

/** The process-wide runtime environment; ONNX Runtime shares one per process anyway. */
@OptIn(ExperimentalForeignApi::class)
private val environment: ORTEnv by lazy { orFail { error -> ORTEnv(ORTLoggingLevel.ORTLoggingLevelWarning, error) } }

/**
 * Opens [model], runs it once on [inputs] and hands its first output to [read]. The session is an Objective-C object that
 * Kotlin frees only when its collector drops it, so a collection follows each run: MI-GAN's memory then leaves with its
 * stage, as it does on Android when the session is closed.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class, NativeRuntimeApi::class)
private fun <T> run(model: String, inputs: List<OrtInput>, read: (NSData) -> T): T = try {
    autoreleasepool {
        val options = orFail { error -> ORTSessionOptions(error) }
        val session = orFail { error -> ORTSession(environment, model, options, error) }
        val firstInput = orFail { error -> session.inputNamesWithError(error) }.first() as String
        val values: Map<Any?, ORTValue> = inputs.associate { input ->
            val shape = input.shape.map { NSNumber(int = it) }
            (input.name ?: firstInput) to orFail { error -> ORTValue(input.data, input.type, shape, error) }
        }
        val output = orFail { error -> session.outputNamesWithError(error) }.first() as String
        val results = orFail { error -> session.runWithInputs(values, setOf(output), null, error) }
        read(orFail { error -> (results[output] as ORTValue).tensorDataWithError(error) })
    }
} finally {
    GC.collect()
}

/** Runs an Objective-C call that reports failure as null plus an NSError. */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun <T : Any> orFail(call: (CPointer<ObjCObjectVar<NSError?>>) -> T?): T = memScoped {
    val error = alloc<ObjCObjectVar<NSError?>>()
    call(error.ptr) ?: throw IllegalStateException("ONNX Runtime: ${error.value?.localizedDescription}")
}

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.data(): NSMutableData = usePinned { mutableData(it.addressOf(0), size) }

@OptIn(ExperimentalForeignApi::class)
private fun FloatArray.data(): NSMutableData = usePinned { mutableData(it.addressOf(0), 4 * size) }

@OptIn(ExperimentalForeignApi::class)
private fun mutableData(bytes: CPointer<*>, size: Int): NSMutableData =
    checkNotNull(NSMutableData.dataWithLength(size.toULong())).also { memcpy(it.mutableBytes, bytes, size.toULong()) }

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toFloatArray(): FloatArray {
    val floats = FloatArray(length.toInt() / 4)
    if (floats.isNotEmpty()) floats.usePinned { memcpy(it.addressOf(0), bytes, (4 * floats.size).toULong()) }
    return floats
}
