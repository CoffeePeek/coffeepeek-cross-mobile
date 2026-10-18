package com.coffeepeek.admin.ui.component

import com.coffeepeek.admin.utils.image.originalCheckInPhoto
import kotlin.math.abs

/** Aligned previews and originals; an absent preview never selects an original implicitly. */
internal data class PhotoViewerImages(
    val previews: List<String>,
    val originals: List<String> = previews.map { originalCheckInPhoto(it).orEmpty() },
) {
    init { require(previews.size == originals.size) }

    fun originalForAction(page: Int): String? = originals.getOrNull(page)
        ?.takeIf { it.isNotBlank() && it != previews.getOrNull(page) }

    fun displayUrl(page: Int, currentPage: Int, originalPage: Int?, isVisible: Boolean): String? {
        if ((page != currentPage && !isVisible) || abs(page - currentPage) > 1) return null
        val preview = previews.getOrNull(page) ?: return null
        return (if (page == currentPage && originalPage == page) originalForAction(page) ?: preview else preview)
            .takeIf(String::isNotBlank)
    }
}
