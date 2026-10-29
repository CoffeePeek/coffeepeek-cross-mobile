package com.coffeepeek.admin.di.homecheckin

import androidx.compose.runtime.*
import com.coffeepeek.admin.utils.PickedImage
import com.coffeepeek.admin.utils.rememberPhotoPicker
import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationUpload
import com.coffeepeek.feature.homecheckin.impl.ui.data.HomeCheckInPhotoPicker
import com.coffeepeek.feature.homecheckin.impl.ui.data.HomeCheckInPhotoPickerController

/** Reuses the existing URI/JPEG/camera-permission pipeline, scoped to each launcher request. */
internal class AppHomeCheckInPhotoPicker : HomeCheckInPhotoPicker {
    private class Request(val gallery: Boolean, val limit: Int, val deliver: (List<HomePreparationUpload>) -> Unit)

    @Composable override fun rememberController(): HomeCheckInPhotoPickerController {
        var pending by remember { mutableStateOf<Request?>(null) }
        var preparing by remember { mutableStateOf(false) }
        val request = pending
        // Cancelling an OS picker may produce no callback. A new request replaces its launcher
        // scope, not the old request's delivery closure; late URI work cannot be retargeted.
        key(request) {
            val picker = rememberPhotoPicker(maxSelection = request?.limit ?: 5,
                isLoading = { if (pending === request) preparing = it },
                onPhotosPicked = { photos ->
                    if (request != null && pending === request) {
                        preparing = false
                        pending = null
                        request.deliver(photos.toHomePreparationPhotos(request.limit))
                    }
                })
            LaunchedEffect(request) {
                if (request != null) {
                    if (request.gallery) picker.pickFromGallery() else picker.takePhoto()
                }
            }
        }
        return HomeCheckInPhotoPickerController(preparing, { remaining, deliver ->
            if (!preparing && remaining > 0) pending = Request(true, remaining.coerceAtMost(5), deliver)
        }, { deliver -> if (!preparing) pending = Request(false, 1, deliver) })
    }
}

internal fun List<PickedImage>.toHomePreparationPhotos(remaining: Int) =
    take(remaining.coerceIn(0, 5)).map { HomePreparationUpload(it.fileName, it.contentType, it.bytes.copyOf()) }
