package com.coffeepeek.feature.shelf.data.scan

import com.coffeepeek.feature.shelf.domain.model.BagScanFailure

private const val PHOTO_SIDE = 2400
private const val PHOTO_JPEG_QUALITY = 92

/**
 * A picked or captured photo (JPEG, HEIC, PNG…) as the scan and the shelf screens read it: upright, sRGB, at most the scanner's
 * working size. Null when it is no readable image.
 */
fun shelfPhotoJpeg(photo: ByteArray): ByteArray? = try {
    IosPackImaging.jpeg(IosPackImaging.decode(photo, PHOTO_SIDE), PHOTO_JPEG_QUALITY)
} catch (_: BagScanFailure) {
    null
}
