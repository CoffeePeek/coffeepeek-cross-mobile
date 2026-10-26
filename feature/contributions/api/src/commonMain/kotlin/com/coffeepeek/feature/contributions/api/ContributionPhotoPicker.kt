package com.coffeepeek.feature.contributions.api

import androidx.compose.runtime.Composable
import com.coffeepeek.feature.contributions.domain.model.PendingPhoto

/** Platform launcher/URI decoding only. Delivery is on the UI thread; an OS cancellation may return nothing. */
fun interface ContributionPhotoPicker {
    @Composable fun rememberController(): ContributionPhotoPickerController
}

class ContributionPhotoPickerController(
    val isPreparing: Boolean,
    val pickFromGallery: (remaining: Int, deliver: (List<PendingPhoto>) -> Unit) -> Unit,
    val takePhoto: (deliver: (List<PendingPhoto>) -> Unit) -> Unit,
)

/** Preview host without external services. Runtime composition always supplies the real platform adapter. */
internal val previewContributionPhotoPicker = ContributionPhotoPicker { ContributionPhotoPickerController(false, { _, _ -> }, {}) }
