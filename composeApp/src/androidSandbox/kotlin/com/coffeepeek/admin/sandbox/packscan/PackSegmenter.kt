package com.coffeepeek.admin.sandbox.packscan

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import com.coffeepeek.admin.sandbox.packscan.imaging.Plane
import com.coffeepeek.admin.sandbox.packscan.imaging.RgbPlanes
import com.coffeepeek.admin.sandbox.packscan.imaging.resizeBilinear
import java.io.File
import java.nio.FloatBuffer

/**
 * U2-Net-p salient-object segmentation (rembg's u2netp.onnx, Apache-2.0, 4.5 MB) with rembg's preprocessing:
 * 320 px input scaled by its maximum and ImageNet-normalised; the first output is min-max normalised.
 */
internal class PackSegmenter(model: File) : AutoCloseable {
    private val environment = OrtEnvironment.getEnvironment()
    private val session = environment.createSession(model.absolutePath, OrtSession.SessionOptions())

    /** Subject probability (0..1) for every pixel of [image]. */
    fun probability(image: RgbPlanes): Plane {
        val input = image.resizeArea(SIDE, SIDE)
        val peak = maxOf(input.r.data.max(), input.g.data.max(), input.b.data.max(), 1e-6f)
        val buffer = FloatBuffer.allocate(3 * SIDE * SIDE)
        listOf(input.r, input.g, input.b).forEachIndexed { c, plane ->
            for (value in plane.data) buffer.put((value / peak - MEAN[c]) / STD[c])
        }
        buffer.rewind()
        val shape = longArrayOf(1, 3, SIDE.toLong(), SIDE.toLong())
        val raw = OnnxTensor.createTensor(environment, buffer, shape).use { tensor ->
            session.run(mapOf(session.inputNames.first() to tensor)).use { outputs ->
                val out = (outputs.get(0) as OnnxTensor).floatBuffer
                FloatArray(out.remaining()).also { out.get(it) }
            }
        }
        val low = raw.min()
        val range = maxOf(raw.max() - low, 1e-6f)
        return Plane(SIDE, SIDE, FloatArray(raw.size) { (raw[it] - low) / range }).resizeBilinear(image.width, image.height)
    }

    override fun close() = session.close()

    private companion object {
        const val SIDE = 320
        val MEAN = floatArrayOf(0.485f, 0.456f, 0.406f)
        val STD = floatArrayOf(0.229f, 0.224f, 0.225f)
    }
}
