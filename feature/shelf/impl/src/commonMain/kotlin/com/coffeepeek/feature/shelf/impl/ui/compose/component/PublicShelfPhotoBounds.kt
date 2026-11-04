package com.coffeepeek.feature.shelf.impl.ui.compose.component

private val pngSignature = byteArrayOf(-119, 80, 78, 71, 13, 10, 26, 10)
private val jpegFrameMarkers = setOf(0xC0, 0xC1, 0xC2, 0xC3, 0xC5, 0xC6, 0xC7, 0xC9, 0xCA, 0xCB, 0xCD, 0xCE, 0xCF)

/** Bound encoded bytes and the RGBA raster before a decoder allocates pixel storage. Unknown formats fail closed. */
internal fun publicShelfPhotoFits(bytes: ByteArray): Boolean {
    if (bytes.size > 1024 * 1024) return false
    val bounds = pngBounds(bytes) ?: jpegBounds(bytes) ?: return false
    val (width, height) = bounds
    return width in 1..4096 && height in 1..4096 && width.toLong() * height * 4 <= 8 * 1024 * 1024
}

private fun pngBounds(bytes: ByteArray): Pair<Int, Int>? {
    if (bytes.size < 25 || !bytes.copyOfRange(0, 8).contentEquals(pngSignature)) return null
    if (bytes.number(8, 4) != 13 || bytes.copyOfRange(12, 16).decodeToString() != "IHDR" || bytes.unsigned(24) > 8) return null
    return bytes.number(16, 4) to bytes.number(20, 4)
}

private fun jpegBounds(bytes: ByteArray): Pair<Int, Int>? {
    if (bytes.size < 4 || bytes.unsigned(0) != 0xFF || bytes.unsigned(1) != 0xD8) return null
    var offset = 2
    while (offset + 3 < bytes.size) {
        if (bytes.unsigned(offset++) != 0xFF) return null
        while (offset < bytes.size && bytes.unsigned(offset) == 0xFF) offset++
        if (offset >= bytes.size) return null
        val marker = bytes.unsigned(offset++)
        if (marker == 0xD9 || marker == 0xDA || offset + 1 >= bytes.size) return null
        if (marker in 0xD0..0xD7 || marker == 0x01) continue
        val length = bytes.number(offset, 2)
        if (length < 2 || offset + length > bytes.size) return null
        if (marker in jpegFrameMarkers) return if (length >= 7 && bytes.unsigned(offset + 2) == 8)
            bytes.number(offset + 5, 2) to bytes.number(offset + 3, 2) else null
        offset += length
    }
    return null
}

private fun ByteArray.number(offset: Int, count: Int): Int = (offset until offset + count).fold(0) { value, index ->
    (value shl 8) or unsigned(index)
}

private fun ByteArray.unsigned(index: Int): Int = this[index].toInt() and 0xFF
