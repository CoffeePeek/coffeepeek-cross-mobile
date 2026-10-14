@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.admin.utils

import com.coffeepeek.admin.theme.ThemeManager
import com.coffeepeek.admin.theme.uiKitStyle

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.UIKit.UIViewController
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.posix.memcpy

@Composable
actual fun rememberPhotoPicker(
    maxSelection: Int,
    isLoading: (Boolean) -> Unit,
    onPhotosPicked: (List<PickedImage>) -> Unit,
): PhotoPickerController {
    val loadingState = rememberUpdatedState(isLoading)
    val pickedState = rememberUpdatedState(onPhotosPicked)
    val delegate = remember {
        IosImagePickerDelegate(
            isLoading = { loadingState.value(it) },
            onPicked = { pickedState.value(it) },
        )
    }

    fun presentCamera() {
        val presenter = topViewController() ?: return
        val camera = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
        if (!UIImagePickerController.isSourceTypeAvailable(camera)) return
        val picker = UIImagePickerController().apply {
            overrideUserInterfaceStyle = ThemeManager.themeMode.value.uiKitStyle
            sourceType = camera
            allowsEditing = false
            this.delegate = delegate
        }
        presenter.presentViewController(picker, animated = true, completion = null)
    }

    fun presentGallery() {
        val presenter = topViewController() ?: return
        val configuration = PHPickerConfiguration().apply {
            selectionLimit = maxSelection.coerceAtLeast(1).toLong()
            filter = PHPickerFilter.imagesFilter
        }
        val picker = PHPickerViewController(configuration).apply {
            overrideUserInterfaceStyle = ThemeManager.themeMode.value.uiKitStyle
            this.delegate = delegate
        }
        presenter.presentViewController(picker, animated = true, completion = null)
    }

    return remember(delegate, maxSelection) {
        PhotoPickerController(
            pickFromGallery = ::presentGallery,
            takePhoto = ::presentCamera,
        )
    }
}

private class IosImagePickerDelegate(
    private val isLoading: (Boolean) -> Unit,
    private val onPicked: (List<PickedImage>) -> Unit,
) : NSObject(),
    UIImagePickerControllerDelegateProtocol,
    UINavigationControllerDelegateProtocol,
    PHPickerViewControllerDelegateProtocol {

    override fun picker(
        picker: PHPickerViewController,
        didFinishPicking: List<*>,
    ) {
        picker.dismissViewControllerAnimated(true, completion = null)
        val results = didFinishPicking.filterIsInstance<PHPickerResult>()
        if (results.isEmpty()) return

        isLoading(true)
        val selected = arrayOfNulls<PickedImage>(results.size)
        var pending = results.size
        results.forEachIndexed { index, result ->
            result.itemProvider.loadDataRepresentationForTypeIdentifier(IMAGE_TYPE_IDENTIFIER) { data, _ ->
                val picked = data
                    ?.let(::imageDataAsJpeg)
                    ?.let { bytes ->
                        PickedImage(
                            bytes = bytes,
                            fileName = "photo_${currentEpochMillis()}_$index.jpg",
                            contentType = "image/jpeg",
                        )
                    }
                dispatch_async(dispatch_get_main_queue()) {
                    selected[index] = picked
                    pending -= 1
                    if (pending == 0) {
                        val images = selected.filterNotNull()
                        if (images.isNotEmpty()) onPicked(images)
                        isLoading(false)
                    }
                }
            }
        }
    }

    override fun imagePickerController(
        picker: UIImagePickerController,
        didFinishPickingMediaWithInfo: Map<Any?, *>,
    ) {
        picker.dismissViewControllerAnimated(true, completion = null)
        val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
            ?: return
        isLoading(true)
        val bytes = UIImageJPEGRepresentation(image, JPEG_QUALITY)?.toByteArray()
        if (bytes != null) {
            onPicked(
                listOf(
                    PickedImage(
                        bytes = bytes,
                        fileName = "photo_${currentEpochMillis()}.jpg",
                        contentType = "image/jpeg",
                    ),
                ),
            )
        }
        isLoading(false)
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        picker.dismissViewControllerAnimated(true, completion = null)
    }
}

private fun imageDataAsJpeg(data: NSData): ByteArray? {
    val image = UIImage(data = data)
    return UIImageJPEGRepresentation(image, JPEG_QUALITY)?.toByteArray()
}

private fun topViewController(): UIViewController? {
    var current = UIApplication.sharedApplication.keyWindow?.rootViewController
    while (current?.presentedViewController != null) {
        current = current.presentedViewController
    }
    return current
}

private fun NSData.toByteArray(): ByteArray {
    if (length == 0UL) return ByteArray(0)
    return ByteArray(length.toInt()).also { destination ->
        destination.usePinned { pinned ->
            memcpy(pinned.addressOf(0), bytes, length)
        }
    }
}

private const val IMAGE_TYPE_IDENTIFIER = "public.image"
private const val JPEG_QUALITY = 0.9
