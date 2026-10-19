package com.coffeepeek.admin.ui.component

import com.coffeepeek.admin.utils.image.originalCheckInPhoto
import kotlin.math.abs

/** Full-screen detail URLs with aligned originals used only while a detail variant is unavailable. */
internal data class PhotoViewerImages(
    val previews: List<String>,
    val originals: List<String> = previews.map { originalCheckInPhoto(it).orEmpty() },
) {
    init { require(previews.size == originals.size) }

    fun highQualityUrl(page: Int, currentPage: Int): String? = originals.getOrNull(page)
        ?.takeIf { page == currentPage && previews.getOrNull(page)?.isBlank() == true }
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
