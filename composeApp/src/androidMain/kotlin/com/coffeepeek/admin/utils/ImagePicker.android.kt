package com.coffeepeek.admin.utils

import com.coffeepeek.core.media.AndroidImageCodec

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.photo_pick_failed
import org.jetbrains.compose.resources.getString
import kotlinx.coroutines.CancellationException

/** Composition-owned media port; live callers keep the existing OS pipeline by default. */
internal val LocalPhotoPickerOverride = androidx.compose.runtime.staticCompositionLocalOf<
    ((Int, (List<PickedImage>) -> Unit) -> PhotoPickerController)?
> { null }

@Composable
actual fun rememberPhotoPicker(
    maxSelection: Int,
    isLoading: (Boolean) -> Unit,
    onPhotosPicked: (List<PickedImage>) -> Unit,
): PhotoPickerController {
    LocalPhotoPickerOverride.current?.let { return it(maxSelection, onPhotosPicked) }
    val activity = LocalActivity.current
    val resolver = activity?.contentResolver
    val scope = rememberCoroutineScope()
    val maxSelectionState = rememberUpdatedState(maxSelection.coerceIn(1, MAX_SHOP_PHOTOS))
    val onPhotosPickedState = rememberUpdatedState(onPhotosPicked)
    val isLoadingState = rememberUpdatedState(isLoading)
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    fun processUris(uris: List<Uri>) {
        if (uris.isEmpty() || resolver == null) return
        scope.launch(Dispatchers.IO) {
            isLoadingState.value(true)
            val limit = maxSelectionState.value
            val outcome = pickOutcome(uris, limit) { uri ->
                val raw = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return@pickOutcome null
                val prepared = AndroidImageCodec.prepareForUpload(raw)
                val fileName = resolver.queryFileName(uri)
                    ?.substringBeforeLast('.')
                    ?.takeIf { it.isNotBlank() }
                    ?.let { "$it.jpg" }
                    ?: "photo_${System.currentTimeMillis()}.jpg"
                PickedImage(bytes = prepared, fileName = fileName, contentType = "image/jpeg")
            }
            val images = outcome.images
            if (outcome.failed > 0) {
                withContext(Dispatchers.Main) { ErrorHandler.showError(getString(Res.string.photo_pick_failed, outcome.failed)) }
            }
            if (images.isNotEmpty()) {
                withContext(Dispatchers.Main) { onPhotosPickedState.value(images) }
            }
            isLoadingState.value(false)
        }
    }

    val pickMultiple = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(MAX_SHOP_PHOTOS),
    ) { uris -> processUris(uris) }

    val takePicture = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { success ->
        val uri = pendingCameraUri
        pendingCameraUri = null
        if (!success || uri == null) {
            // Clean up empty/failed capture file if present.
            if (uri != null) {
                runCatching { activity?.contentResolver?.delete(uri, null, null) }
            }
            return@rememberLauncherForActivityResult
        }
        processUris(listOf(uri))
    }

    val requestCameraPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (!granted) return@rememberLauncherForActivityResult
        val context = activity ?: return@rememberLauncherForActivityResult
        val uri = context.createCameraImageUri() ?: return@rememberLauncherForActivityResult
        pendingCameraUri = uri
        takePicture.launch(uri)
    }

    fun launchCamera() {
        val context = activity ?: return
        when {
            context.checkSelfPermission(Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED -> {
                val uri = context.createCameraImageUri() ?: return
                pendingCameraUri = uri
                takePicture.launch(uri)
            }
            else -> requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    return PhotoPickerController(
        pickFromGallery = {
            pickMultiple.launch(
                PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    .build(),
            )
        },
        takePhoto = ::launchCamera,
    )
}

internal class PickOutcome(val images: List<PickedImage>, val failed: Int)

/** Prepares up to [limit] picked items; one that cannot be read or prepared is counted, not silently dropped. */
internal fun <T> pickOutcome(items: List<T>, limit: Int, prepare: (T) -> PickedImage?): PickOutcome {
    val chosen = items.take(limit)
    val images = chosen.mapNotNull { item ->
        try { prepare(item) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { null }
    }
    return PickOutcome(images, chosen.size - images.size)
}

private fun android.content.Context.createCameraImageUri(): Uri? = runCatching {
    val dir = File(cacheDir, "camera").apply { mkdirs() }
    val file = File(dir, "capture_${System.currentTimeMillis()}.jpg")
    FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
}.getOrNull()

private fun android.content.ContentResolver.queryFileName(uri: Uri): String? =
    query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
    }
