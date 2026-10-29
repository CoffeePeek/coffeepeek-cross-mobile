package com.coffeepeek.admin.di.shelf

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Handler
import android.os.Looper
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.FocusMeteringResult
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.coffeepeek.feature.shelf.api.ShelfCamera
import com.coffeepeek.feature.shelf.api.ShelfCameraFocus
import com.coffeepeek.feature.shelf.api.ShelfCameraSession
import com.coffeepeek.feature.shelf.api.ShelfCameraStatus
import com.coffeepeek.feature.shelf.api.ShelfPhoto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

/** CameraX behind the shelf's camera port: the back camera, bound to the screen's lifecycle only while the camera is shown. */
internal object AndroidShelfCamera : ShelfCamera {
    @Composable
    override fun rememberSession(): ShelfCameraSession {
        val context = LocalContext.current
        val owner = LocalLifecycleOwner.current
        val session = remember(context) { AndroidShelfCameraSession(context) }
        val access = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { session.onAccess(it) }
        session.ask = { access.launch(Manifest.permission.CAMERA) }
        LaunchedEffect(session) { if (!session.granted) session.requestAccess() }
        DisposableEffect(session, owner, session.granted) {
            if (session.granted) session.start(owner)
            onDispose { session.stop() }
        }
        return session
    }
}

private class AndroidShelfCameraSession(private val context: Context) : ShelfCameraSession {
    var granted by mutableStateOf(context.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
        private set
    var ask: () -> Unit = {}
    override var status by mutableStateOf(ShelfCameraStatus.Starting)
        private set
    override var torchAvailable by mutableStateOf(false)
        private set
    private var torchOn by mutableStateOf(false)
    override val torch: Boolean get() = torchOn
    private var focusShown by mutableStateOf<ShelfCameraFocus?>(null)
    override val focus: ShelfCameraFocus? get() = focusShown
    private var focusPoint = CENTER
    private val main = Handler(Looper.getMainLooper())
    private val hideFocus = Runnable { focusShown = null }
    private val viewfinder = PreviewView(context).apply {
        scaleType = PreviewView.ScaleType.FILL_CENTER
        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
    }
    private var provider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var capture: ImageCapture? = null
    private var bound: List<UseCase> = emptyList()
    private var active = false

    fun onAccess(allowed: Boolean) {
        granted = allowed
        if (!allowed) status = ShelfCameraStatus.Denied
    }

    override fun requestAccess() {
        status = ShelfCameraStatus.Starting
        ask()
    }

    fun start(owner: LifecycleOwner) {
        active = true
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({ if (active) bind(owner, future.get()) }, ContextCompat.getMainExecutor(context))
    }

    private fun bind(owner: LifecycleOwner, provider: ProcessCameraProvider) {
        val preview = Preview.Builder().build().also { it.surfaceProvider = viewfinder.surfaceProvider }
        val shot = ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).setResolutionSelector(photoSize).build()
        try {
            camera = provider.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, preview, shot)
        } catch (_: Exception) {
            status = ShelfCameraStatus.Unavailable
            return
        }
        this.provider = provider
        capture = shot
        bound = listOf(preview, shot)
        torchAvailable = camera?.cameraInfo?.hasFlashUnit() == true
        status = ShelfCameraStatus.Ready
        // A bag is close: settle focus on the frame's centre right away instead of waiting for continuous AF to hunt.
        viewfinder.post { meter(CENTER) }
    }

    override fun focusAt(x: Float, y: Float) {
        val point = x to y
        focusPoint = point
        focusShown = ShelfCameraFocus(x, y, sharp = null)
        main.removeCallbacks(hideFocus)
        meter(point) { sharp ->
            if (focusPoint == point) focusShown = ShelfCameraFocus(x, y, sharp)
            main.postDelayed(hideFocus, FOCUS_SHOWN_MILLIS)
        }
    }

    /** Focus and exposure at [point] (shares of the viewfinder); continuous AF takes over again after a few seconds. */
    private fun meter(point: Pair<Float, Float>, onResult: (Boolean) -> Unit = {}): ListenableFuture<FocusMeteringResult>? {
        val control = camera?.cameraControl ?: return null
        if (viewfinder.width == 0 || viewfinder.height == 0) return null
        val spot = viewfinder.meteringPointFactory.createPoint(point.first * viewfinder.width, point.second * viewfinder.height)
        val action = FocusMeteringAction.Builder(spot, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
            .setAutoCancelDuration(4, TimeUnit.SECONDS).build()
        return control.startFocusAndMetering(action).also { future ->
            future.addListener({ onResult(runCatching { future.get().isFocusSuccessful }.getOrDefault(false)) }, ContextCompat.getMainExecutor(context))
        }
    }

    fun stop() {
        active = false
        provider?.unbind(*bound.toTypedArray())
        provider = null
        camera = null
        capture = null
        bound = emptyList()
        torchOn = false
        main.removeCallbacks(hideFocus)
        focusShown = null
        focusPoint = CENTER
        status = if (granted) ShelfCameraStatus.Starting else ShelfCameraStatus.Denied
    }

    override fun setTorch(on: Boolean) {
        val control = camera?.cameraControl ?: return
        control.enableTorch(on)
        torchOn = on
    }

    @Composable
    override fun Viewfinder(modifier: Modifier) = AndroidView({ viewfinder }, modifier)

    override fun snapshot(): ImageBitmap? = viewfinder.bitmap?.asImageBitmap()

    override suspend fun capture(): ShelfPhoto? {
        val shot = capture ?: return null
        // Refocus where the user aimed (or the centre) and wait briefly: a sharp card matters more than a fast shutter.
        withContext(Dispatchers.Main) { meter(focusPoint) }?.let { withTimeoutOrNull(FOCUS_BEFORE_SHOT_MILLIS) { it.settled() } }
        return suspendCancellableCoroutine { continuation ->
            shot.takePicture(Dispatchers.Default.asExecutor(), object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bytes = try { image.uprightJpeg() } catch (_: Exception) { null } finally { image.close() }
                    if (continuation.isActive) continuation.resume(bytes?.let { ShelfPhoto(it, fromCamera = true) })
                }

                override fun onError(exception: ImageCaptureException) {
                    if (continuation.isActive) continuation.resume(null)
                }
            })
        }
    }
}

private val CENTER = 0.5f to 0.5f
private const val FOCUS_SHOWN_MILLIS = 1_200L
private const val FOCUS_BEFORE_SHOT_MILLIS = 1_500L

/** Resumes when the future completes, successfully or not. */
private suspend fun ListenableFuture<*>.settled() = suspendCancellableCoroutine { continuation ->
    addListener({ if (continuation.isActive) continuation.resume(Unit) }, Runnable::run)
}

/** Close to the scanner's 2400 px working size, 4:3 like the sensor; no need for the full 12 MP. */
private val photoSize = ResolutionSelector.Builder()
    .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
    .setResolutionStrategy(ResolutionStrategy(Size(2400, 1800), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER))
    .build()

private fun ImageProxy.uprightJpeg(): ByteArray {
    val frame = toBitmap()
    val degrees = imageInfo.rotationDegrees
    val upright = if (degrees == 0) frame
        else Bitmap.createBitmap(frame, 0, 0, frame.width, frame.height, Matrix().apply { postRotate(degrees.toFloat()) }, true)
    return upright.shelfPhotoJpeg()
}
