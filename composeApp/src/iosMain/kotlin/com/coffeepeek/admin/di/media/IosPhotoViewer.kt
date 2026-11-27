@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.admin.di.media

import com.coffeepeek.admin.analytics.Analytics
import com.coffeepeek.admin.analytics.correlationId
import com.coffeepeek.core.designsystem.component.CoffeePeekPhotoViewerImages
import com.coffeepeek.core.media.image.FullScreenPhotoPresence
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import platform.UIKit.UIImage

/** Exports a pageable image source without exposing URLs or access tokens to Swift. */
class IosPhotoViewer internal constructor(
    private val images: CoffeePeekPhotoViewerImages,
    index: Int,
    private val frames: (String) -> Flow<UIImage?>,
) {
    private val scope = CoroutineScope(Dispatchers.Main)
    private val overlayId = correlationId("overlay")
    private var visible = false

    val photoCount = images.previews.size
    val initialIndex = index.coerceIn(0, (photoCount - 1).coerceAtLeast(0))

    fun setVisible(shown: Boolean) {
        if (visible == shown) return
        visible = shown
        if (shown) FullScreenPhotoPresence.enter() else {
            FullScreenPhotoPresence.leave()
            scope.coroutineContext.cancelChildren()
        }
        Analytics.service?.overlay(overlayId, shown)
    }

    fun loadPreview(page: Int, selected: Int, onFrame: (UIImage?) -> Unit) =
        observe(images.displayUrl(page, selected, isVisible = true), onFrame)

    fun loadOriginal(page: Int, selected: Int, onFrame: (UIImage?) -> Unit) =
        observe(images.highQualityUrl(page, selected), onFrame)

    private fun observe(url: String?, onFrame: (UIImage?) -> Unit) = IosPhotoViewerObservation(scope.launch {
        if (url == null) onFrame(null) else frames(url).collect { onFrame(it) }
    })
}

class IosPhotoViewerObservation internal constructor(private val job: Job) {
    fun cancel() = job.cancel()
}
