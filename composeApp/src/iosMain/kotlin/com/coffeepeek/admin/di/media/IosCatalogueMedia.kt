@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.coffeepeek.admin.di.media

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeUIViewController
import com.coffeepeek.admin.AppContent
import com.coffeepeek.core.designsystem.component.CoffeePeekPhotoViewer
import com.coffeepeek.core.media.ImageUrl
import com.coffeepeek.core.media.ImageUrlFetcher
import com.coffeepeek.core.media.resolveImageData
import io.kamel.core.config.ResourceConfigBuilder
import io.kamel.core.getOrNull
import io.ktor.utils.io.toByteArray
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withContext
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIImage
import platform.UIKit.UIViewController

internal class IosCatalogueMedia(
    private val imageFetcher: ImageUrlFetcher,
    private val apiBaseUrl: String,
) {
    suspend fun loadImage(url: String): UIImage? = withContext(Dispatchers.Default) {
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

    @OptIn(ExperimentalComposeUiApi::class)
    fun makeViewer(imageUrls: List<String>, index: Int, onDismiss: () -> Unit): UIViewController =
        ComposeUIViewController(configure = { opaque = false }) {
            AppContent { CoffeePeekPhotoViewer(imageUrls, index, onDismiss) }
        }
}

private fun ByteArray.toImage(): UIImage? {
    if (isEmpty()) return null
    val data = usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
    }
    return UIImage.imageWithData(data)
}
