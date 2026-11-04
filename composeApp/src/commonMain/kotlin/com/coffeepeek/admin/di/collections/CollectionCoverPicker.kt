package com.coffeepeek.admin.di.collections

import androidx.compose.runtime.*
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.photo_pick_failed
import com.coffeepeek.admin.utils.ErrorHandler
import com.coffeepeek.admin.utils.rememberPhotoPicker
import com.coffeepeek.feature.collections.api.CollectionCoverImage
import com.coffeepeek.feature.collections.api.CollectionCoverPicker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString

/** Local gallery selection only; collection data owns uploads after the user saves. */
internal val collectionCoverPicker: CollectionCoverPicker = { show, onDismiss, onLoading, onPicked ->
    val scope = rememberCoroutineScope()
    var reading by remember { mutableStateOf(false) }
    var preparing by remember { mutableStateOf(false) }
    LaunchedEffect(reading, preparing) { onLoading(reading || preparing) }
    val picker = rememberPhotoPicker(1, isLoading = { reading = it }, onPhotosPicked = { images ->
        images.firstOrNull()?.let { image -> scope.launch {
            preparing = true
            try {
                val bytes = withContext(Dispatchers.Default) { prepareCollectionCover(image.bytes) }
                onPicked(CollectionCoverImage(bytes, "image/jpeg", image.fileName.substringBeforeLast('.') + ".jpg"))
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { ErrorHandler.showError(getString(Res.string.photo_pick_failed, 1)) }
            finally { preparing = false }
        } }
    })
    LaunchedEffect(show) {
        if (show) {
            onDismiss()
            picker.pickFromGallery()
        }
    }
}

/** Strip metadata, bound dimensions and bytes before passing media to the feature. */
internal expect suspend fun prepareCollectionCover(bytes: ByteArray): ByteArray
