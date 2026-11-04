package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.coffeepeek.core.designsystem.component.CoffeePeekImagePlaceholder
import com.coffeepeek.core.designsystem.component.CoffeePeekPhotoAttachments
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shop.domain.model.MAX_SHOP_CHECK_IN_PHOTOS
import com.coffeepeek.feature.shop.impl.resources.*
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import androidx.compose.ui.tooling.preview.Preview

@Composable
internal fun ShopCheckInPhotoAttachments(
    photos: List<ByteArray>, isLoading: Boolean, enabled: Boolean,
    onMove: (Int, Int) -> Unit, onRemove: (Int) -> Unit,
    onGallery: (Int) -> Unit, onCamera: () -> Unit,
) {
    var sourceShown by remember { mutableStateOf(false) }
    if (sourceShown && enabled && !isLoading && photos.size < MAX_SHOP_CHECK_IN_PHOTOS) ShopPhotoSourceSheet(
        onDismiss = { sourceShown = false },
        onGallery = {
            sourceShown = false
            onGallery((MAX_SHOP_CHECK_IN_PHOTOS - photos.size).coerceAtLeast(0))
        },
        onCamera = {
            sourceShown = false
            onCamera()
        },
    )
    Column(verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
        Text(stringResource(Res.string.shop_checkin_form_photos), style = MaterialTheme.typography.labelLarge)
        Text(stringResource(Res.string.shop_checkin_form_photos_hint, MAX_SHOP_CHECK_IN_PHOTOS),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(stringResource(Res.string.shop_review_photo_count, photos.size, MAX_SHOP_CHECK_IN_PHOTOS),
            style = MaterialTheme.typography.labelMedium)
        CoffeePeekPhotoAttachments(photos, enabled, isLoading, MAX_SHOP_CHECK_IN_PHOTOS,
            onMove, onRemove, { sourceShown = true }, photo = { bytes, modifier, scale ->
                AttachmentImage(bytes, modifier, scale)
            })
    }
}

@Composable
private fun AttachmentImage(bytes: ByteArray, modifier: Modifier, scale: ContentScale) {
    if (bytes.isEmpty()) CoffeePeekImagePlaceholder(modifier)
    else KamelImage(resource = { asyncPainterResource(bytes) }, contentDescription = null,
        contentScale = scale, modifier = modifier,
        onLoading = { CoffeePeekImagePlaceholder(Modifier.fillMaxSize()) },
        onFailure = { CoffeePeekImagePlaceholder(Modifier.fillMaxSize()) })
}

@Preview @Composable private fun ShopCheckInPhotoAttachmentsLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopCheckInPhotoAttachments(emptyList(), false, true, { _, _ -> }, {}, {}, {})
}
@Preview @Composable private fun ShopCheckInPhotoAttachmentsDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopCheckInPhotoAttachments(List(4) { byteArrayOf() }, false, true, { _, _ -> }, {}, {}, {})
}
