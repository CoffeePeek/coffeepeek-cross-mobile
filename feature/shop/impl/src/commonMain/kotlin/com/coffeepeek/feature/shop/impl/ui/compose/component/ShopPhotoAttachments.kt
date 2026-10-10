package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CpIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.CpDimens
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_photo_unavailable
import com.coffeepeek.feature.shop.impl.resources.shop_review_photo_add
import com.coffeepeek.feature.shop.impl.resources.shop_review_photo_count
import com.coffeepeek.feature.shop.impl.resources.shop_review_photo_remove
import com.coffeepeek.feature.shop.impl.resources.shop_photo_attachment_description
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ShopPhotoAttachments(
    photos: List<ByteArray>,
    isLoading: Boolean,
    enabled: Boolean,
    title: String,
    hint: String,
    maxPhotos: Int,
    onRemove: (Int) -> Unit,
    onGallery: (Int) -> Unit,
    onCamera: () -> Unit,
) {
    var sourceShown by remember { mutableStateOf(false) }
    if (sourceShown && enabled && !isLoading && photos.size < maxPhotos) ShopPhotoSourceSheet(
        onDismiss = { sourceShown = false },
        onGallery = {
            sourceShown = false
            onGallery((maxPhotos - photos.size).coerceAtLeast(0))
        },
        onCamera = {
            sourceShown = false
            onCamera()
        },
    )

    Column(verticalArrangement = Arrangement.spacedBy(CpDimens.spacing2)) {
        Text(title, style = MaterialTheme.typography.labelLarge)
        Text(hint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(stringResource(Res.string.shop_review_photo_count, photos.size, maxPhotos),
            style = MaterialTheme.typography.labelMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing2),
            verticalArrangement = Arrangement.spacedBy(CpDimens.spacing2)) {
            photos.take(maxPhotos).forEachIndexed { index, photo ->
                Box(Modifier.size(96.dp).clip(RoundedCornerShape(CpDimens.radiusMd))
                    .background(MaterialTheme.colorScheme.surfaceVariant)) {
                    if (photo.isNotEmpty()) {
                        KamelImage(
                            resource = { asyncPainterResource(photo) },
                            contentDescription = stringResource(Res.string.shop_photo_attachment_description, index + 1),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(96.dp),
                            onLoading = { PhotoUnavailable() },
                            onFailure = { PhotoUnavailable() },
                        )
                    } else PhotoUnavailable()
                    IconButton(onClick = { onRemove(index) }, enabled = enabled,
                        modifier = Modifier.align(Alignment.TopEnd)) {
                        Icon(CpIcons.Close,
                            contentDescription = stringResource(Res.string.shop_review_photo_remove, index + 1),
                            tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            if (photos.size < maxPhotos) {
                Box(Modifier.size(96.dp).clip(RoundedCornerShape(CpDimens.radiusMd))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(enabled = enabled && !isLoading) { sourceShown = true },
                    contentAlignment = Alignment.Center) {
                    if (isLoading) CircularProgressIndicator()
                    else Icon(CpIcons.Camera,
                        contentDescription = stringResource(Res.string.shop_review_photo_add))
                }
            }
        }
    }
}

@Composable private fun PhotoUnavailable() {
    Box(Modifier.size(96.dp), contentAlignment = Alignment.Center) {
        Text(stringResource(Res.string.shop_photo_unavailable),
            style = MaterialTheme.typography.labelSmall)
    }
}

@Preview @Composable private fun ShopPhotoAttachmentsLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopPhotoAttachments(emptyList(), false, true, "Фотографии", "Добавьте до 5 фото", 5, {}, {}, {})
}

@Preview @Composable private fun ShopPhotoAttachmentsDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopPhotoAttachments(listOf(byteArrayOf()), false, true, "Фотографии", "Добавьте до 5 фото", 5, {}, {}, {})
}
