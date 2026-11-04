package com.coffeepeek.admin.sandbox.packscan

import ai.onnxruntime.OnnxJavaType
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.coffeepeek.admin.sandbox.packscan.geometry.PackPoint
import com.coffeepeek.admin.sandbox.packscan.geometry.extendOutward
import java.io.File
import java.nio.ByteBuffer
import kotlin.math.max

/**
 * MI-GAN 512 Places2 ONNX pipeline (Picsart AI Research, MIT code and weights). The pipeline crops
 * around the mask, inpaints at 512 px and blends back, so any image size can be passed.
 */
internal class PackInpainter(model: File) : AutoCloseable {
    private val environment = OrtEnvironment.getEnvironment()
    private val session = environment.createSession(model.absolutePath, OrtSession.SessionOptions())

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

/** Brush radius in image pixels; the editor draws strokes with the same radius. */
internal fun Bitmap.brushRadius(): Float = max(width, height) * 0.02f

/** Rasterizes brush strokes (image pixels) into a repaint mask. */
internal fun strokeMask(width: Int, height: Int, strokes: List<List<PackPoint>>, radius: Float): BooleanArray {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = radius * 2
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    val canvas = Canvas(bitmap)
    for (stroke in strokes.filter { it.isNotEmpty() }) {
        val path = Path().apply { moveTo(stroke.first().x, stroke.first().y) }
        // A single tap still marks a dot.
        (stroke.drop(1).ifEmpty { listOf(stroke.first()) }).forEach { path.lineTo(it.x, it.y) }
        canvas.drawPath(path, paint)
    }
    // ALPHA_8 rows may be padded, so index through rowBytes.
    val stride = bitmap.rowBytes
    val alpha = ByteBuffer.allocate(stride * height).also { bitmap.copyPixelsToBuffer(it) }.array()
    return BooleanArray(width * height) { (alpha[(it / width) * stride + it % width].toInt() and 0xFF) > 0 }
}
