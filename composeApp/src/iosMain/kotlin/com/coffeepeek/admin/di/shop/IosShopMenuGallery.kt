@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.coffeepeek.admin.di.shop

import androidx.compose.ui.window.ComposeUIViewController
import com.coffeepeek.admin.AppContent
import com.coffeepeek.core.designsystem.component.CoffeePeekPhotoViewer
import com.coffeepeek.core.media.ImageUrl
import com.coffeepeek.core.media.ImageUrlFetcher
import com.coffeepeek.core.media.resolveImageData
import com.coffeepeek.feature.shop.impl.IosShopMenuGallerySession
import com.coffeepeek.feature.shop.impl.ShopMenuGallerySnapshot
import io.kamel.core.config.ResourceConfigBuilder
import io.kamel.core.getOrNull
import io.ktor.utils.io.toByteArray
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIImage
import platform.UIKit.UIViewController

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
    private val imageFetcher: ImageUrlFetcher,
    private val apiBaseUrl: String,
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
            onImage(url?.takeIf(String::isNotBlank)?.let { loadImage(it) })
        })

    @OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
    fun makeViewer(photo: IosShopMenuGalleryPhoto, onDismiss: () -> Unit): UIViewController =
        ComposeUIViewController(configure = { opaque = false }) {
            AppContent {
                CoffeePeekPhotoViewer(photo.imageUrls, photo.index, onDismiss)
            }
        }

    fun close() {
        previewScope.cancel()
        session.close()
    }

    private suspend fun loadImage(url: String): UIImage? = withContext(Dispatchers.Default) {
        try {
            val imageUrl = resolveImageData(url, apiBaseUrl) as ImageUrl
            val config = ResourceConfigBuilder(currentCoroutineContext()).build()
            imageFetcher.fetch(imageUrl, config)
                .mapNotNull { it.getOrNull()?.toByteArray()?.toImage() }
                .firstOrNull()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
    }
}

private fun ShopMenuGallerySnapshot.toIosState() =
    IosShopMenuGalleryState(shopTitle, previewUrls, isLoading, hasError)

private fun ByteArray.toImage(): UIImage? {
    if (isEmpty()) return null
    val data = usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
    }
    return UIImage.imageWithData(data)
}
