package com.coffeepeek.admin.di.shelf

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import com.coffeepeek.admin.utils.BitmapUtil
import com.coffeepeek.feature.shelf.api.ShelfGalleryPicker
import com.coffeepeek.feature.shelf.api.ShelfPhoto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

private const val PHOTO_SIDE = 2400
private const val JPEG_QUALITY = 92

/** The system photo picker, one image; no storage permission is needed. */
internal val AndroidShelfGallery: ShelfGalleryPicker = { onPicked, onFailed ->
    val activity = LocalActivity.current
    val scope = rememberCoroutineScope()
    val picked by rememberUpdatedState(onPicked)
    val failed by rememberUpdatedState(onFailed)
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val context = activity?.applicationContext
        if (uri != null && context != null) scope.launch {
            val photo = try { withContext(Dispatchers.IO) { context.uprightPhoto(uri) } }
                catch (cancelled: CancellationException) { throw cancelled } catch (_: Exception) { null }
            if (photo != null) picked(ShelfPhoto(photo, fromCamera = false)) else failed()
        }
    }
    remember(gallery) { { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) } }
}

private fun Context.uprightPhoto(uri: Uri): ByteArray {
    val raw = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: error("Unreadable photo")
    return BitmapUtil.decodeOrientedBitmap(raw, decodeMaxSide = PHOTO_SIDE).shelfPhotoJpeg()
}

/** An upright bitmap capped at the scanner's working size, as the JPEG the scan and the screens read. */
internal fun Bitmap.shelfPhotoJpeg(): ByteArray {
    val factor = PHOTO_SIDE.toFloat() / max(width, height)
    val sized = if (factor < 1f) Bitmap.createScaledBitmap(this, (width * factor).roundToInt(), (height * factor).roundToInt(), true) else this
    return ByteArrayOutputStream().also { sized.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }.toByteArray()
}
