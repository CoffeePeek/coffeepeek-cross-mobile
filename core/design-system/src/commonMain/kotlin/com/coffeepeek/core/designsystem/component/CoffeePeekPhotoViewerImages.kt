package com.coffeepeek.core.designsystem.component

import com.coffeepeek.core.media.image.originalCheckInPhoto
import kotlin.math.abs

/** Already displayed previews with aligned originals loaded only for the selected photo. */
data class CoffeePeekPhotoViewerImages(
    val previews: List<String>,
    val originals: List<String> = previews.map { originalCheckInPhoto(it) ?: it },
) {
    init { require(previews.size == originals.size) }

    fun highQualityUrl(page: Int, currentPage: Int): String? = originals.getOrNull(page)
        ?.takeIf { page == currentPage && it != previews.getOrNull(page) }
        ?.takeIf(String::isNotBlank)

    fun displayUrl(page: Int, currentPage: Int, isVisible: Boolean): String? {
        if ((page != currentPage && !isVisible) || abs(page - currentPage) > 1) return null
        val preview = previews.getOrNull(page) ?: return null
        return preview.takeIf(String::isNotBlank) ?: highQualityUrl(page, currentPage)
    }

    fun adjacentPreviewUrls(currentPage: Int): List<String> =
        listOf(currentPage - 1, currentPage + 1).mapNotNull { page ->
            previews.getOrNull(page)?.takeIf(String::isNotBlank)
        }.distinct()
}
