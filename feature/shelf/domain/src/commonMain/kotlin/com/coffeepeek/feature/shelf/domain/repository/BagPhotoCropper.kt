package com.coffeepeek.feature.shelf.domain.repository

import com.coffeepeek.feature.shelf.domain.model.BagCropBounds
import com.coffeepeek.feature.shelf.domain.model.ManualBagCrop

/** Crops a photo locally without scanner models. The returned JPEG is the image uploaded with the bag. */
fun interface BagPhotoCropper {
    suspend fun crop(photo: ByteArray, bounds: BagCropBounds): Result<ManualBagCrop>
}
