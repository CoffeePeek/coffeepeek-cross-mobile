package com.coffeepeek.feature.shelf.data.scan

import ai.onnxruntime.OnnxJavaType
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import android.graphics.Bitmap
import android.graphics.Color
import com.coffeepeek.feature.shelf.data.scan.geometry.extendOutward
import java.io.File
import java.nio.ByteBuffer

/**
 * MI-GAN 512 Places2 ONNX pipeline (Picsart AI Research, MIT code and weights). The pipeline crops
 * around the mask, inpaints at 512 px and blends back, so any image size can be passed.
 */
internal class OnnxPackInpainter(model: File) : AutoCloseable {
    private val environment = OrtEnvironment.getEnvironment()
    private val session = environment.createSession(model.absolutePath, sessionOptions())

    /** Regenerates [repaint] pixels of [image]; the image alpha (bag outline) is kept as is. */
    fun inpaint(image: Bitmap, repaint: BooleanArray): Bitmap {
        val width = image.width
        val pixels = IntArray(width * image.height).also { image.getPixels(it, 0, width, 0, 0, width, image.height) }
        val inside = BooleanArray(pixels.size) { Color.alpha(pixels[it]) > 127 }
        val context = pixels.copyOf().also { extendOutward(it, inside, width) }
        val result = run(context, repaint, width, image.height)
        for (i in result.indices) result[i] = (pixels[i] and ALPHA) or (result[i] and RGB)
        return Bitmap.createBitmap(result, width, image.height, Bitmap.Config.ARGB_8888)
    }

    private fun run(pixels: IntArray, repaint: BooleanArray, width: Int, height: Int): IntArray {
        val plane = width * height
        val image = ByteBuffer.allocateDirect(3 * plane)
        for (shift in intArrayOf(16, 8, 0)) for (pixel in pixels) image.put((pixel shr shift).toByte())
        val known = ByteBuffer.allocateDirect(plane)
        for (pixel in repaint) known.put(if (pixel) 0 else KNOWN)
        image.rewind()
        known.rewind()
        OnnxTensor.createTensor(environment, image, longArrayOf(1, 3, height.toLong(), width.toLong()), OnnxJavaType.UINT8).use { imageTensor ->
            OnnxTensor.createTensor(environment, known, longArrayOf(1, 1, height.toLong(), width.toLong()), OnnxJavaType.UINT8).use { maskTensor ->
                session.run(mapOf("image" to imageTensor, "mask" to maskTensor)).use { outputs ->
                    val out = (outputs.get(0) as OnnxTensor).byteBuffer
                    return IntArray(plane) { Color.rgb(out.channel(it), out.channel(plane + it), out.channel(2 * plane + it)) }
                }
            }
        }
    }

    override fun close() = session.close()

    private companion object {
        const val ALPHA = 0xFF000000.toInt()
        const val RGB = 0x00FFFFFF
        const val KNOWN: Byte = -1 // 255 as uint8
    }
}

private fun ByteBuffer.channel(index: Int): Int = get(index).toInt() and 0xFF
