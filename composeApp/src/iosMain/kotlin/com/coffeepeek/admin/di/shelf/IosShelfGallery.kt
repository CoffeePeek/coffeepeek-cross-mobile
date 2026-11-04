@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.admin.di.shelf

import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.coffeepeek.admin.theme.ThemeManager
import com.coffeepeek.admin.theme.uiKitStyle
import com.coffeepeek.feature.shelf.api.ShelfGalleryPicker
import com.coffeepeek.feature.shelf.api.ShelfPhoto
import com.coffeepeek.feature.shelf.data.scan.shelfPhotoJpeg
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.posix.memcpy

/** The system photo picker, one image; it runs out of process, so no photo-library permission is asked. */
internal val IosShelfGallery: ShelfGalleryPicker = { onPicked, onFailed ->
    val picked by rememberUpdatedState(onPicked)
    val failed by rememberUpdatedState(onFailed)
    // The picker holds its delegate weakly: composition keeps it alive while the picker can call back.
    val delegate = remember { ShelfPickerDelegate(onPicked = { picked(it) }, onFailed = { failed() }) }
    remember(delegate) { { present(delegate) } }
}

private fun present(delegate: ShelfPickerDelegate) {
    val presenter = topViewController() ?: return
    val configuration = PHPickerConfiguration().apply {
        selectionLimit = 1
        filter = PHPickerFilter.imagesFilter
    }
    val picker = PHPickerViewController(configuration).apply {
        overrideUserInterfaceStyle = ThemeManager.themeMode.value.uiKitStyle
        this.delegate = delegate
    }
    presenter.presentViewController(picker, animated = true, completion = null)
}

private class ShelfPickerDelegate(
    private val onPicked: (ShelfPhoto) -> Unit,
    private val onFailed: () -> Unit,
) : NSObject(), PHPickerViewControllerDelegateProtocol {
    override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
        picker.dismissViewControllerAnimated(true, completion = null)
        val result = didFinishPicking.firstOrNull() as? PHPickerResult ?: return
        // The provider calls back on a background queue: the photo is made upright and sized there, off the main thread.
        result.itemProvider.loadDataRepresentationForTypeIdentifier(IMAGE_TYPE) { data, _ ->
            val photo = data?.toByteArray()?.let(::shelfPhotoJpeg)
            dispatch_async(dispatch_get_main_queue()) {
                if (photo != null) onPicked(ShelfPhoto(photo, fromCamera = false)) else onFailed()
            }
        }
    }
}

internal fun topViewController(): UIViewController? {
    var current = UIApplication.sharedApplication.keyWindow?.rootViewController
    while (current?.presentedViewController != null) current = current.presentedViewController
    return current
}

internal fun NSData.toByteArray(): ByteArray {
    if (length == 0UL) return ByteArray(0)
    return ByteArray(length.toInt()).also { bytes -> bytes.usePinned { memcpy(it.addressOf(0), this.bytes, length) } }
}

private const val IMAGE_TYPE = "public.image"
