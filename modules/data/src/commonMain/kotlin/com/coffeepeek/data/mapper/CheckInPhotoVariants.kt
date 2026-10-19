package com.coffeepeek.data.mapper

import com.coffeepeek.api.model.response.CheckInPhotoVariantDto

/** Null is a legacy response. An explicit empty/unsupported manifest never downloads an original. */
internal fun List<CheckInPhotoVariantDto>?.photoUrl(purpose: String, original: String): String {
    if (this == null) return original
    // JPEG/PNG also work on older iOS versions.
    val formats = listOf("image/png", "image/jpeg", "image/webp")
    val supported = filter { it.purpose == purpose && it.width > 0 && it.height > 0 && it.sizeBytes > 0 && it.url.isNotBlank() }
    return formats.firstNotNullOfOrNull { type -> supported.firstOrNull { it.contentType == type }?.url }.orEmpty()
}
