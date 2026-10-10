package com.coffeepeek.admin.di.shop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.coffeepeek.admin.utils.PickedImage
import com.coffeepeek.admin.utils.rememberPhotoPicker
import com.coffeepeek.feature.shop.domain.model.MAX_SHOP_CHECK_IN_PHOTOS
import com.coffeepeek.feature.shop.domain.model.ShopCheckInPhoto
import com.coffeepeek.feature.shop.impl.ui.data.ShopCheckInPhotoPicker
import com.coffeepeek.feature.shop.impl.ui.data.ShopCheckInPhotoPickerController

/** Reuses the current Android URI/JPEG/camera-permission pipeline; no second decoder. */
internal class AndroidShopCheckInPhotoPicker : ShopCheckInPhotoPicker {
    @Composable
    override fun rememberController(): ShopCheckInPhotoPickerController {
        var preparing by remember { mutableStateOf(false) }
        var pending by remember { mutableStateOf<Pair<Int, (List<ShopCheckInPhoto>) -> Unit>?>(null) }
        val picker = rememberPhotoPicker(
            maxSelection = MAX_SHOP_CHECK_IN_PHOTOS,
            isLoading = { preparing = it },
            onPhotosPicked = { images ->
                val request = pending
                pending = null
                request?.let { (remaining, deliver) -> deliver(images.toCheckInPhotos(remaining)) }
            },
        )
        return ShopCheckInPhotoPickerController(
            isPreparing = preparing,
            pickFromGallery = { remaining, deliver ->
                if (!preparing && remaining > 0) {
                    pending = remaining to deliver
                    picker.pickFromGallery()
                }
            },
            takePhoto = { deliver ->
                if (!preparing) {
                    pending = 1 to deliver
                    picker.takePhoto()
                }
            },
        )
    }
}

internal fun List<PickedImage>.toCheckInPhotos(remaining: Int): List<ShopCheckInPhoto> =
    take(remaining.coerceIn(0, MAX_SHOP_CHECK_IN_PHOTOS)).map {
        ShopCheckInPhoto(bytes = it.bytes, fileName = it.fileName, contentType = it.contentType)
    }
