package com.coffeepeek.feature.shelf.data.scan

import com.coffeepeek.feature.shelf.domain.model.BagCropBounds
import com.coffeepeek.feature.shelf.domain.model.ManualBagCrop
import com.coffeepeek.feature.shelf.domain.model.ShelfBagCard
import com.coffeepeek.feature.shelf.domain.repository.BagPhotoCropper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val PHOTO_SIDE = 2400
private const val CARD_SIDE = 1200
private const val MAX_CARD_BYTES = 1024 * 1024

/** Reuses the platform image codec, without loading or running either scanner model. */
internal fun manualBagCropper(imaging: PackImaging): BagPhotoCropper = BagPhotoCropper { photo, bounds ->
    try {
        require(bounds.isValid()) { "Invalid crop bounds" }
        Result.success(withContext(Dispatchers.Default) { imaging.crop(photo, bounds) })
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        Result.failure(failure)
    }
}

private fun PackImaging.crop(photo: ByteArray, bounds: BagCropBounds): ManualBagCrop {
    val source = decode(photo, PHOTO_SIDE)
    val left = (bounds.left * source.width).toInt().coerceIn(0, source.width - 1)
    val top = (bounds.top * source.height).toInt().coerceIn(0, source.height - 1)
    val right = (bounds.right * source.width).toInt().coerceIn(left + 1, source.width)
    val bottom = (bounds.bottom * source.height).toInt().coerceIn(top + 1, source.height)
    val width = right - left
    val height = bottom - top
    val pixels = IntArray(width * height)
    repeat(height) { row ->
        source.pixels.copyInto(pixels, row * width, (top + row) * source.width + left,
            (top + row) * source.width + right)
    }
    return encodeCard(PackImage(width, height, pixels))
}

private fun PackImaging.encodeCard(crop: PackImage): ManualBagCrop {
    for ((side, quality) in listOf(CARD_SIDE to 85, 1000 to 75, 800 to 65)) {
        val image = scaledDown(crop, side)
        val bytes = jpeg(image, quality)
        if (bytes.isNotEmpty() && bytes.size <= MAX_CARD_BYTES) {
            return ManualBagCrop(ShelfBagCard(bytes, "image/jpeg", "bag.jpg"), image.width, image.height)
        }
    }
    error("Cropped JPEG exceeds the shelf image limit")
}
