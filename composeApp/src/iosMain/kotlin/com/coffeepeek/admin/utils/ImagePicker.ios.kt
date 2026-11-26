@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.coffeepeek.admin.utils

import com.coffeepeek.admin.theme.ThemeManager
import com.coffeepeek.admin.theme.uiKitStyle

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.uikit.LocalUIViewController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UIModalPresentationOverFullScreen
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.UIKit.UIViewController
import platform.darwin.NSObject
import platform.posix.memcpy

@Composable
actual fun rememberPhotoPicker(
    maxSelection: Int,
    isLoading: (Boolean) -> Unit,
    onPhotosPicked: (List<PickedImage>) -> Unit,
): PhotoPickerController {
    val host = LocalUIViewController.current
    val scope = rememberCoroutineScope()
    val loadingState = rememberUpdatedState(isLoading)
    val pickedState = rememberUpdatedState(onPhotosPicked)
    val delegate = remember {
        IosImagePickerDelegate(
            scope = scope,
            isLoading = { loadingState.value(it) },
            onPicked = { pickedState.value(it) },
        )
    }
    DisposableEffect(delegate) { onDispose { delegate.cancelWork() } }

    fun presentCamera() {
        val picker = cameraPicker(delegate) ?: return delegate.cancel()
        scope.launch {
            val presenter = awaitPhotoPickerHost(host)
            if (presenter == null) delegate.cancel()
            else presenter.presentViewController(picker, animated = true, completion = null)
        }
    }

    fun presentGallery() {
        val picker = galleryPicker(delegate, maxSelection)
        scope.launch {
            val presenter = awaitPhotoPickerHost(host)
            if (presenter == null) delegate.cancel()
            else presenter.presentViewController(picker, animated = true, completion = null)
        }
    }

    return remember(host, delegate, maxSelection) {
        PhotoPickerController(
            pickFromGallery = ::presentGallery,
            takePhoto = ::presentCamera,
        )
    }
}

/** Plain native launcher for controllers which do not have a composition lifetime. */
internal class IosNativePhotoPicker(private val scope: CoroutineScope,
    private val onPreparing: (Boolean) -> Unit = {}, private val host: () -> UIViewController?) {
    private var closed = false
    private var revision = 0L
    private var delegate: IosImagePickerDelegate? = null
    private var controller: UIViewController? = null
    private var presentation: Job? = null
    var isPreparing = false
        private set

    fun show(camera: Boolean, maximum: Int, receive: (List<PickedImage>) -> Unit) {
        if (closed || isPreparing || controller != null || maximum <= 0) return
        val request = ++revision
        val callback = IosImagePickerDelegate(scope, { if (current(request)) setPreparing(it) },
            { images -> complete(request, maximum, images, receive) })
        delegate = callback
        val picker = if (camera) cameraPicker(callback) else galleryPicker(callback, maximum)
        if (picker == null) return callback.cancel()
        controller = picker
        presentation?.cancel()
        presentation = scope.launch { present(request, picker, callback) }
    }

    private suspend fun present(request: Long, picker: UIViewController, callback: IosImagePickerDelegate) {
        val source = host()
        val presenter = source?.let { awaitPhotoPickerHost(it) }
        if (!current(request)) return
        if (presenter == null) callback.cancel()
        else presenter.presentViewController(picker, animated = true, completion = null)
    }

    private fun complete(request: Long, maximum: Int, images: List<PickedImage>, receive: (List<PickedImage>) -> Unit) {
        var completed: IosImagePickerDelegate? = null
        try {
            if (current(request)) {
                completed = delegate
                revision++
                setPreparing(false)
                delegate = null
                controller = null
                receive(images.take(maximum))
            }
        } finally {
            images.forEach { it.bytes.fill(0) }
            completed?.cancelWork()
        }
    }

    fun close() {
        if (closed) return
        closed = true
        cancel()
    }

    fun cancel() {
        revision++
        presentation?.cancel()
        delegate?.cancelWork()
        controller?.takeIf { it.presentingViewController != null }?.dismissViewControllerAnimated(false, null)
        controller = null
        delegate = null
        setPreparing(false)
    }

    private fun setPreparing(value: Boolean) {
        if (isPreparing == value) return
        isPreparing = value
        onPreparing(value)
    }

    private fun current(request: Long) = !closed && request == revision
}

private fun galleryPicker(delegate: IosImagePickerDelegate, maximum: Int): PHPickerViewController {
    val configuration = PHPickerConfiguration().apply {
        selectionLimit = maximum.coerceAtLeast(1).toLong()
        filter = PHPickerFilter.imagesFilter
    }
    return PHPickerViewController(configuration).apply {
        overrideUserInterfaceStyle = ThemeManager.themeMode.value.uiKitStyle
        this.delegate = delegate
    }
}

private fun cameraPicker(delegate: IosImagePickerDelegate): UIImagePickerController? {
    val camera = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
    if (!UIImagePickerController.isSourceTypeAvailable(camera)) return null
    return UIImagePickerController().apply {
        overrideUserInterfaceStyle = ThemeManager.themeMode.value.uiKitStyle
        sourceType = camera
        modalPresentationStyle = UIModalPresentationOverFullScreen
        allowsEditing = false
        this.delegate = delegate
    }
}

private class IosImagePickerDelegate(
    scope: CoroutineScope,
    isLoading: (Boolean) -> Unit,
    onPicked: (List<PickedImage>) -> Unit,
) : NSObject(),
    UIImagePickerControllerDelegateProtocol,
    UINavigationControllerDelegateProtocol,
    PHPickerViewControllerDelegateProtocol {
    private val imports = NativePhotoImportSession(scope, isLoading, onPicked)

    override fun picker(
        picker: PHPickerViewController,
        didFinishPicking: List<*>,
    ) {
        if (!imports.active) return
        picker.dismissViewControllerAnimated(true, completion = null)
        val results = didFinishPicking.filterIsInstance<PHPickerResult>()
        if (results.isEmpty()) return cancel()

        imports.start { loadGalleryImages(results) }
    }

    private suspend fun loadGalleryImages(results: List<PHPickerResult>): List<PickedImage> {
        val selected = mutableListOf<PickedImage>()
        try {
            results.forEachIndexed { index, result ->
                val data = loadGalleryData(result)
                val bytes = withContext(Dispatchers.Default) { data?.let(::imageDataAsJpeg) }
                bytes?.let { selected.add(PickedImage(it, "photo_${currentEpochMillis()}_$index.jpg")) }
            }
            return selected
        } catch (error: Throwable) {
            selected.forEach { it.bytes.fill(0) }
            throw error
        }
    }

    private suspend fun loadGalleryData(result: PHPickerResult): NSData? = suspendCancellableCoroutine { continuation ->
        val progress = result.itemProvider.loadDataRepresentationForTypeIdentifier(IMAGE_TYPE_IDENTIFIER) { data, _ ->
            if (continuation.isActive) continuation.resume(data)
        }
        continuation.invokeOnCancellation { progress.cancel() }
    }

    fun cancelWork() = imports.close()

    override fun imagePickerController(
        picker: UIImagePickerController,
        didFinishPickingMediaWithInfo: Map<Any?, *>,
    ) {
        if (!imports.active) return
        val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
        picker.dismissViewControllerAnimated(true) {
            if (image != null) deliverCameraImage(image) else cancel()
        }
    }

    private fun deliverCameraImage(image: UIImage) {
        imports.start {
            val bytes = withContext(Dispatchers.Default) { UIImageJPEGRepresentation(image, JPEG_QUALITY)?.toByteArray() }
            bytes?.let { listOf(PickedImage(it, "photo_${currentEpochMillis()}.jpg")) }.orEmpty()
        }
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        if (!imports.active) return
        picker.dismissViewControllerAnimated(true) { cancel() }
    }

    fun cancel() = imports.cancelSelection()
}

private fun imageDataAsJpeg(data: NSData): ByteArray? {
    val image = UIImage(data = data)
    return UIImageJPEGRepresentation(image, JPEG_QUALITY)?.toByteArray()
}

private suspend fun awaitPhotoPickerHost(host: UIViewController): UIViewController? {
    // The source sheet is removed from Compose before its UIKit dismissal finishes.
    // Presenting from that outgoing sheet silently loses the picker request.
    delay(16)
    val closing = host.presentedViewController
    while (host.view.window != null && !host.isBeingDismissed()) {
        if (host.presentedViewController == null && (closing == null || closing.view.window == null)) {
            return host
        }
        delay(16)
    }
    return null
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
