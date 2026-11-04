package com.coffeepeek.admin.sandbox.packscan

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.provider.MediaStore
import java.io.File
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Saves every probe output to Pictures/CoffeePeekProbe so a batch of real photos can be pulled for desktop
 * comparison, and appends the pre-moderation signals to pack-metrics.jsonl in the app's external files dir
 * (next to the models) for threshold calibration. MediaStore writes of the app's own files need no permission.
 */
internal fun Context.exportPackProbe(result: PackCutoutResult) {
    val stamp = stamp()
    exportPackImages(
        "overlay" to result.overlay,
        "rect" to result.rectified,
        "final" to result.inpainted,
        "soft" to result.softened,
        "card" to result.card,
        stamp = stamp,
    )
    File(getExternalFilesDir(null), METRICS_FILE).appendText(metricsLine(stamp, result) + "\n")
}

internal fun Context.exportPackImages(vararg images: Pair<String, Bitmap?>, stamp: String = stamp()) {
    images.forEach { (name, bitmap) -> bitmap?.let { savePng("pack-$stamp-$name.png", it) } }
}

private fun stamp(): String = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))

private fun metricsLine(stamp: String, result: PackCutoutResult): String {
    val q = result.quality
    val signals = q?.let {
        listOf(
            "sideSupport" to it.sideSupport, "aspect" to it.aspect, "angleDeviation" to it.angleDeviation,
            "quadShare" to it.quadShare, "borderTouch" to it.borderTouch, "clutterOutside" to it.clutterOutside,
            "repaintShare" to it.repaintShare, "strongGlare" to it.strongGlare,
        ).joinToString(",") { (key, value) -> "\"$key\":${String.format(Locale.US, "%.4f", value)}" }
    }
    val reasons = result.moderation.reasons.joinToString(",") { "\"$it\"" }
    val t = result.timings
    return "{\"stamp\":\"$stamp\",\"status\":\"${result.moderation.status}\",\"reasons\":[$reasons],\"start\":\"${result.start}\"," +
        "\"millis\":{\"segmentation\":${t.segmentation},\"fit\":${t.fit},\"repair\":${t.repair},\"soften\":${t.soften},\"total\":${t.total}}" +
        (signals?.let { ",$it" } ?: "") + "}"
}

private fun Context.savePng(name: String, bitmap: Bitmap) {
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, name)
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CoffeePeekProbe")
    }
    val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        ?: throw IOException("MediaStore refused $name")
    contentResolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        ?: throw IOException("No output stream for $name")
}

private const val METRICS_FILE = "pack-metrics.jsonl"
