@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.coffeepeek.admin.di.shop

import com.coffeepeek.admin.di.media.IosCatalogueMedia
import com.coffeepeek.core.designsystem.component.CoffeePeekPhotoViewerImages
import com.coffeepeek.feature.shop.impl.IosShopMenuGallerySession
import com.coffeepeek.feature.shop.impl.ShopMenuGallerySnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import platform.UIKit.UIImage

data class IosShopMenuGalleryState(
    val shopTitle: String,
    val previewUrls: List<String>,
    val isLoading: Boolean,
    val hasError: Boolean,
)

class IosShopMenuGalleryPhoto internal constructor(
    internal val imageUrls: List<String>,
    internal val index: Int,
)

class IosShopMenuGalleryObservation internal constructor(private vararg val jobs: Job) {
    fun cancel() = jobs.forEach { it.cancel() }
}

class IosShopMenuGallery internal constructor(
    private val session: IosShopMenuGallerySession,
    private val media: IosCatalogueMedia,
    private val onBack: () -> Unit,
) {
    private val previewScope = CoroutineScope(Dispatchers.Main)

    fun currentState(): IosShopMenuGalleryState = session.currentState().toIosState()

    fun observe(
        onState: (IosShopMenuGalleryState) -> Unit,
        onPhoto: (IosShopMenuGalleryPhoto) -> Unit,
    ): IosShopMenuGalleryObservation {
        val state = session.observeState { onState(it.toIosState()) }
        val events = session.observeEvents(onBack) { urls, index -> onPhoto(IosShopMenuGalleryPhoto(urls, index)) }
        return IosShopMenuGalleryObservation(state, events)
    }

    fun retry() = session.retry()

    fun openPhoto(index: Int) = session.openPhoto(index)

    fun back() = session.back()

    fun loadPreview(index: Int, onImage: (UIImage?) -> Unit): IosShopMenuGalleryObservation =
        IosShopMenuGalleryObservation(previewScope.launch {
            val url = session.currentState().previewUrls.getOrNull(index)
            onImage(url?.takeIf(String::isNotBlank)?.let { media.loadImage(it) })
        })

    fun photoViewer(photo: IosShopMenuGalleryPhoto) =
        media.viewer(CoffeePeekPhotoViewerImages(photo.imageUrls), photo.index)

    fun close() {
        previewScope.cancel()
        session.close()
    }

}

private fun ShopMenuGallerySnapshot.toIosState() =
    IosShopMenuGalleryState(shopTitle, previewUrls, isLoading, hasError)
