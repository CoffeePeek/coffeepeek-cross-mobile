package com.coffeepeek.feature.shop.impl.ui.data

import androidx.compose.runtime.Composable
import com.coffeepeek.feature.shop.domain.model.ShopCheckInPhoto

/** Composition supplies platform launchers; presentation never reads URIs or bitmaps. */
interface ShopCheckInPhotoPicker {
    @Composable
    fun rememberController(): ShopCheckInPhotoPickerController
}

class ShopCheckInPhotoPickerController(
    val isPreparing: Boolean,
    val pickFromGallery: (remaining: Int, onPhotos: (List<ShopCheckInPhoto>) -> Unit) -> Unit,
    val takePhoto: (onPhotos: (List<ShopCheckInPhoto>) -> Unit) -> Unit,
)
