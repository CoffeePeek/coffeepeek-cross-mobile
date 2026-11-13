package com.coffeepeek.feature.shelf.domain.model

/** A rectangle in the upright photo, expressed as shares of its width and height. */
data class BagCropBounds(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    fun isValid(): Boolean = left in 0f..1f && top in 0f..1f && right in 0f..1f && bottom in 0f..1f &&
        right - left >= 0.1f && bottom - top >= 0.1f

    companion object {
        val Default = BagCropBounds(0.12f, 0.08f, 0.88f, 0.92f)
    }
}

/** A manually cropped image; no automatic quality or moderation verdict is implied. */
class ManualBagCrop(val card: ShelfBagCard, val width: Int, val height: Int)
