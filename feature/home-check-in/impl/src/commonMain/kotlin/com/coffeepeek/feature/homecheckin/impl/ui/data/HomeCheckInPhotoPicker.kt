package com.coffeepeek.feature.homecheckin.impl.ui.data

import androidx.compose.runtime.Composable
import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationUpload

/** Platform launcher/URI decoding only. Callbacks must return on the UI thread; cancelled picks may return nothing. */
interface HomeCheckInPhotoPicker {
    @Composable fun rememberController(): HomeCheckInPhotoPickerController
}

class HomeCheckInPhotoPickerController(
    val isPreparing: Boolean,
    val pickFromGallery: (remaining: Int, deliver: (List<HomePreparationUpload>) -> Unit) -> Unit,
    val takePhoto: (deliver: (List<HomePreparationUpload>) -> Unit) -> Unit,
)
