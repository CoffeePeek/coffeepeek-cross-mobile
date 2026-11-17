@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.admin.di.media

import com.coffeepeek.core.designsystem.component.CoffeePeekPhotoViewerImages
import com.coffeepeek.core.media.ImageUrl
import com.coffeepeek.core.media.ImageUrlFetcher
import com.coffeepeek.core.media.loadIosImage
import com.coffeepeek.core.media.observeIosViewerImage
import com.coffeepeek.core.media.resolveImageData
import platform.UIKit.UIImage

internal class IosCatalogueMedia(
    private val imageFetcher: ImageUrlFetcher,
    private val apiBaseUrl: String,
) {
    suspend fun loadImage(url: String): UIImage? = imageFetcher.loadIosImage(imageUrl(url))

    fun viewer(
        images: CoffeePeekPhotoViewerImages,
        index: Int,
        canLoad: () -> Boolean = { true },
    ) = IosPhotoViewer(images, index) { url -> imageFetcher.observeIosViewerImage(imageUrl(url), canLoad) }

    private fun imageUrl(url: String) = resolveImageData(url, apiBaseUrl) as ImageUrl
}
