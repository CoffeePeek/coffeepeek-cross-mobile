package com.coffeepeek.feature.shelf.data.scan

import ai.onnxruntime.OnnxJavaType
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import com.coffeepeek.feature.shelf.domain.model.BagScanFailure
import com.coffeepeek.feature.shelf.domain.model.BagScanIssue
import java.io.File
import java.nio.ByteBuffer
import java.nio.FloatBuffer

/** U2-Net-p and MI-GAN on ONNX Runtime for Android; [files] resolves the model files when a network is first needed. */
internal class OnnxPackNetworks(private val files: () -> ScanModelFiles) : PackNetworks {
    private val environment get() = OrtEnvironment.getEnvironment()

    override fun segment(input: FloatArray): FloatArray = session(files().segmenter) { session ->
        val shape = longArrayOf(1, 3, SEGMENTER_SIDE.toLong(), SEGMENTER_SIDE.toLong())
        OnnxTensor.createTensor(environment, FloatBuffer.wrap(input), shape).use { tensor ->
            session.run(mapOf(session.inputNames.first() to tensor)).use { outputs ->
                val out = (outputs.get(0) as OnnxTensor).floatBuffer
                FloatArray(out.remaining()).also { out.get(it) }
            }
        }
    }

    override fun inpaint(image: ByteArray, known: ByteArray, width: Int, height: Int): ByteArray {
        val model = files().inpainter ?: throw BagScanFailure(BagScanIssue.ModelMissing)
        return session(model) { session ->
            uint8(image, 3, width, height).use { imageTensor ->
                uint8(known, 1, width, height).use { maskTensor ->
                    session.run(mapOf("image" to imageTensor, "mask" to maskTensor)).use { outputs ->
                        val out = (outputs.get(0) as OnnxTensor).byteBuffer
                        ByteArray(out.remaining()).also { out.get(it) }
                    }
                }
            }
        }
    }

    private fun uint8(values: ByteArray, channels: Int, width: Int, height: Int): OnnxTensor {
        val buffer = ByteBuffer.allocateDirect(values.size).put(values).rewind() as ByteBuffer
        return OnnxTensor.createTensor(environment, buffer, longArrayOf(1, channels.toLong(), height.toLong(), width.toLong()), OnnxJavaType.UINT8)
    }

    // Sessions live for one run; without the CPU arena their memory returns to the system when closed.
    private fun <T> session(model: File, block: (OrtSession) -> T): T = OrtSession.SessionOptions().use { options ->
        options.setCPUArenaAllocator(false)
        environment.createSession(model.absolutePath, options).use(block)
    }
}
