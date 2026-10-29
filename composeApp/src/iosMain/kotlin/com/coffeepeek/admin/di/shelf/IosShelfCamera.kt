@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.coffeepeek.admin.di.shelf

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import com.coffeepeek.feature.shelf.api.ShelfCamera
import com.coffeepeek.feature.shelf.api.ShelfCameraFocus
import com.coffeepeek.feature.shelf.api.ShelfCameraSession
import com.coffeepeek.feature.shelf.api.ShelfCameraStatus
import com.coffeepeek.feature.shelf.api.ShelfPhoto
import com.coffeepeek.feature.shelf.data.scan.shelfPhotoJpeg
import kotlinx.cinterop.cValue
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import platform.AVFoundation.*
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGRect
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSURL
import platform.QuartzCore.CATransaction
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UIKit.UIColor
import platform.UIKit.UIView
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_queue_create
import kotlin.coroutines.resume

/** AVFoundation behind the shelf's camera port: the back wide camera, running only while the camera screen is composed. */
internal object IosShelfCamera : ShelfCamera {
    @Composable
    override fun rememberSession(): ShelfCameraSession {
        val scope = rememberCoroutineScope()
        val session = remember { IosShelfCameraSession(scope) }
        DisposableEffect(session) {
            session.start()
            onDispose { session.stop() }
        }
        return session
    }
}

private class IosShelfCameraSession(private val scope: CoroutineScope) : ShelfCameraSession {
    override var status by mutableStateOf(ShelfCameraStatus.Starting)
        private set
    override var torchAvailable by mutableStateOf(false)
        private set
    override var torch by mutableStateOf(false)
        private set
    override var focus by mutableStateOf<ShelfCameraFocus?>(null)
        private set
    // startRunning and stopRunning block, so they run on a queue of their own, never on the main thread.
    private val queue = dispatch_queue_create("com.coffeepeek.shelf.camera", null)
    private val capture = AVCaptureSession()
    private val photos = AVCapturePhotoOutput()
    private val preview = AVCaptureVideoPreviewLayer(session = capture).apply { videoGravity = AVLayerVideoGravityResizeAspectFill }
    private var device: AVCaptureDevice? = null
    private var active = false
    private var focusPoint = CENTER
    private var focusJob: Job? = null
    private var pendingShot: ShotDelegate? = null

    fun start() {
        active = true
        when (AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)) {
            AVAuthorizationStatusAuthorized -> configure()
            AVAuthorizationStatusNotDetermined -> ask()
            else -> status = ShelfCameraStatus.Denied
        }
    }

    private fun ask() = AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
        dispatch_async(dispatch_get_main_queue()) { if (granted) configure() else status = ShelfCameraStatus.Denied }
    }

    private fun configure() {
        if (!active || device != null) return
        val camera = AVCaptureDevice.defaultDeviceWithDeviceType(AVCaptureDeviceTypeBuiltInWideAngleCamera, AVMediaTypeVideo,
            AVCaptureDevicePositionBack)
        val input = camera?.let { AVCaptureDeviceInput.deviceInputWithDevice(it, null) }
        if (camera == null || input == null || !capture.canAddInput(input) || !capture.canAddOutput(photos)) {
            status = ShelfCameraStatus.Unavailable
            return
        }
        capture.beginConfiguration()
        capture.sessionPreset = AVCaptureSessionPresetPhoto
        capture.addInput(input)
        capture.addOutput(photos)
        capture.commitConfiguration()
        device = camera
        torchAvailable = camera.hasTorch
        dispatch_async(queue) {
            capture.startRunning()
            dispatch_async(dispatch_get_main_queue()) { if (active) onRunning() }
        }
    }

    private fun onRunning() {
        // The app is portrait: preview and photo are both upright for a phone held upright.
        listOfNotNull(preview.connection, photos.connectionWithMediaType(AVMediaTypeVideo)).forEach {
            if (it.isVideoRotationAngleSupported(PORTRAIT)) it.videoRotationAngle = PORTRAIT
        }
        status = ShelfCameraStatus.Ready
        // A bag is close: settle focus on the frame's centre right away instead of waiting for continuous AF to hunt.
        meter(CENTER)
    }

    fun stop() {
        active = false
        focusJob?.cancel()
        if (torch) setTorch(false)
        dispatch_async(queue) { capture.stopRunning() }
    }

    /** iOS asks only once; after a refusal access is given back in the app's Settings page. */
    override fun requestAccess() {
        if (AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo) == AVAuthorizationStatusNotDetermined) return ask()
        val settings = NSURL.URLWithString(UIApplicationOpenSettingsURLString) ?: return
        UIApplication.sharedApplication.openURL(settings, options = emptyMap<Any?, Any>(), completionHandler = null)
    }

    override fun setTorch(on: Boolean) {
        val camera = device?.takeIf { it.hasTorch } ?: return
        camera.locked { torchMode = if (on) AVCaptureTorchModeOn else AVCaptureTorchModeOff }
        torch = on
    }

    override fun focusAt(x: Float, y: Float) {
        val point = x to y
        focusPoint = point
        focus = ShelfCameraFocus(x, y, sharp = null)
        focusJob?.cancel()
        focusJob = scope.launch {
            meter(point)
            val sharp = withTimeoutOrNull(FOCUS_SETTLE_MILLIS) { settled() } != null
            if (focusPoint == point) focus = ShelfCameraFocus(x, y, sharp)
            delay(FOCUS_SHOWN_MILLIS)
            if (focusPoint == point) focus = null
        }
    }

    /** Continuous focus and exposure around [point] (shares of the viewfinder). */
    private fun meter(point: Pair<Float, Float>) {
        val camera = device ?: return
        val target = devicePoint(point)
        camera.locked {
            if (focusPointOfInterestSupported && isFocusModeSupported(AVCaptureFocusModeContinuousAutoFocus)) {
                focusPointOfInterest = target
                focusMode = AVCaptureFocusModeContinuousAutoFocus
            }
            if (exposurePointOfInterestSupported && isExposureModeSupported(AVCaptureExposureModeContinuousAutoExposure)) {
                exposurePointOfInterest = target
                exposureMode = AVCaptureExposureModeContinuousAutoExposure
            }
        }
    }

    private fun devicePoint(point: Pair<Float, Float>) = preview.bounds.useContents {
        if (size.width <= 0.0 || size.height <= 0.0) return@useContents CGPointMake(point.second.toDouble(), 1.0 - point.first)
        preview.captureDevicePointOfInterestForPoint(CGPointMake(point.first * size.width, point.second * size.height))
    }

    /** Waits until the lens has started and finished moving; iOS does not say whether it found focus. */
    private suspend fun settled() {
        val camera = device ?: return
        withTimeoutOrNull(FOCUS_START_MILLIS) { while (!camera.adjustingFocus) delay(POLL_MILLIS) }
        while (camera.adjustingFocus) delay(POLL_MILLIS)
    }

    @Composable
    override fun Viewfinder(modifier: Modifier) = UIKitView(
        factory = { PreviewView(preview) },
        modifier = modifier,
        // Taps belong to the Compose overlay, which reports them through focusAt.
        properties = UIKitInteropProperties(interactionMode = null),
    )

    /** The preview layer cannot be read back; the screen keeps the live picture while the shot is taken. */
    override fun snapshot(): ImageBitmap? = null

    override suspend fun capture(): ShelfPhoto? {
        if (status != ShelfCameraStatus.Ready) return null
        // Refocus where the user aimed (or the centre) and wait briefly: a sharp card matters more than a fast shutter.
        meter(focusPoint)
        withTimeoutOrNull(FOCUS_BEFORE_SHOT_MILLIS) { settled() }
        val data = shoot() ?: return null
        return withContext(Dispatchers.Default) { shelfPhotoJpeg(data.toByteArray()) }?.let { ShelfPhoto(it, fromCamera = true) }
    }

    private suspend fun shoot(): NSData? = suspendCancellableCoroutine { continuation ->
        val delegate = ShotDelegate { data ->
            pendingShot = null
            if (continuation.isActive) continuation.resume(data)
        }
        pendingShot = delegate
        photos.capturePhotoWithSettings(AVCapturePhotoSettings.photoSettings(), delegate)
    }
}

/** Hands over the encoded photo with its orientation metadata, or null when the shot failed. */
private class ShotDelegate(private val done: (NSData?) -> Unit) : NSObject(), AVCapturePhotoCaptureDelegateProtocol {
    override fun captureOutput(output: AVCapturePhotoOutput, didFinishProcessingPhoto: AVCapturePhoto, error: NSError?) {
        done(if (error == null) didFinishProcessingPhoto.fileDataRepresentation() else null)
    }
}

/** Keeps the preview layer the size of its view; the layer is not animated as Compose resizes the view. */
private class PreviewView(private val preview: AVCaptureVideoPreviewLayer) : UIView(frame = cValue<CGRect> { }) {
    init {
        backgroundColor = UIColor.blackColor
        layer.addSublayer(preview)
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        CATransaction.begin()
        CATransaction.setDisableActions(true)
        preview.frame = bounds
        CATransaction.commit()
    }
}

/** Runs [change] with the camera locked for configuration; a camera another app holds is left as it is. */
private fun AVCaptureDevice.locked(change: AVCaptureDevice.() -> Unit) {
    if (!lockForConfiguration(null)) return
    try {
        change()
    } finally {
        unlockForConfiguration()
    }
}

private val CENTER = 0.5f to 0.5f
private const val PORTRAIT = 90.0
private const val POLL_MILLIS = 50L
private const val FOCUS_START_MILLIS = 300L
private const val FOCUS_SETTLE_MILLIS = 2_000L
private const val FOCUS_SHOWN_MILLIS = 1_200L
private const val FOCUS_BEFORE_SHOT_MILLIS = 1_500L
