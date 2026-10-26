package com.coffeepeek.admin.di.contributions

import androidx.compose.runtime.*
import com.coffeepeek.admin.utils.PickedImage
import com.coffeepeek.admin.utils.PhotoPickerController
import com.coffeepeek.admin.utils.rememberPhotoPicker
import com.coffeepeek.feature.contributions.api.ContributionPhotoPicker
import com.coffeepeek.feature.contributions.api.ContributionPhotoPickerController
import com.coffeepeek.feature.contributions.domain.model.PendingPhoto
import com.coffeepeek.feature.contributions.domain.rules.ContributionLimits

/** Native launcher/URI/JPEG/camera permission only; no attachment UI or feature state. */
internal class AppContributionPhotoPicker : ContributionPhotoPicker {
    private class Request(val gallery: Boolean, val limit: Int, val deliver: (List<PendingPhoto>) -> Unit)

    @Composable
    override fun rememberController(): ContributionPhotoPickerController {
        var pending by remember { mutableStateOf<Request?>(null) }
        var preparing by remember { mutableStateOf(false) }
        val request = pending
        key(request) {
            val picker = rememberPhotoPicker(maxSelection = request?.limit ?: ContributionLimits.SHOP_PHOTOS,
                isLoading = { if (pending === request) preparing = it },
                onPhotosPicked = { photos ->
                    if (request != null && pending === request) {
                        pending = null
                        preparing = false
                        request.deliver(photos.toContributionPhotos(request.limit))
                    }
                })
            LaunchPhotoRequest(request, picker)
        }
        return ContributionPhotoPickerController(preparing,
            { remaining, deliver -> if (!preparing && remaining > 0) pending = Request(true, remaining.coerceAtMost(ContributionLimits.SHOP_PHOTOS), deliver) },
            { deliver -> if (!preparing) pending = Request(false, 1, deliver) })
    }

    @Composable
    private fun LaunchPhotoRequest(request: Request?, picker: PhotoPickerController) {
        LaunchedEffect(request) {
            if (request != null) {
                if (request.gallery) picker.pickFromGallery() else picker.takePhoto()
            }
        }
    }
}

internal fun List<PickedImage>.toContributionPhotos(remaining: Int): List<PendingPhoto> =
    take(remaining.coerceIn(0, ContributionLimits.SHOP_PHOTOS)).map { PendingPhoto(it.fileName, it.contentType, it.bytes.copyOf()) }
