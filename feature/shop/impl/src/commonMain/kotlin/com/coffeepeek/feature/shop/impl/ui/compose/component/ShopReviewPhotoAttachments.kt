package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.runtime.Composable
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shop.domain.model.MAX_SHOP_REVIEW_PHOTOS
import com.coffeepeek.feature.shop.domain.model.ShopReviewPhoto
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_review_new_photos
import com.coffeepeek.feature.shop.impl.resources.shop_review_photos
import com.coffeepeek.feature.shop.impl.resources.shop_review_new_photo_hint
import com.coffeepeek.feature.shop.impl.resources.shop_review_photo_hint
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ShopReviewPhotoAttachments(
    photos: List<ShopReviewPhoto>,
    isLoading: Boolean,
    enabled: Boolean,
    existingPhotos: Boolean,
    onRemove: (Int) -> Unit,
    onGallery: (Int) -> Unit,
    onCamera: () -> Unit,
) = ShopPhotoAttachments(
    photos = photos.map { it.bytes }, isLoading = isLoading, enabled = enabled,
    title = stringResource(if (existingPhotos) Res.string.shop_review_new_photos else Res.string.shop_review_photos),
    hint = stringResource(if (existingPhotos) Res.string.shop_review_new_photo_hint else Res.string.shop_review_photo_hint,
        MAX_SHOP_REVIEW_PHOTOS),
    maxPhotos = MAX_SHOP_REVIEW_PHOTOS, onRemove = onRemove, onGallery = onGallery, onCamera = onCamera,
)

@Preview @Composable private fun ShopReviewPhotoAttachmentsLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopReviewPhotoAttachments(emptyList(), false, true, false, {}, {}, {})
}

@Preview @Composable private fun ShopReviewPhotoAttachmentsDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopReviewPhotoAttachments(listOf(ShopReviewPhoto(byteArrayOf(), "photo.jpg")), false, true, true, {}, {}, {})
}
