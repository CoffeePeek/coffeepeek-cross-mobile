package com.coffeepeek.feature.shop.impl.ui.compose.component

import com.coffeepeek.feature.shop.impl.resources.shop_review_photo_hint

import com.coffeepeek.feature.shop.impl.resources.shop_review_photos

import com.coffeepeek.core.designsystem.component.CoffeePeekLoader

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import com.coffeepeek.core.designsystem.component.CoffeePeekIconButton as IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
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

    Column(verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
        Text(hint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(stringResource(Res.string.shop_review_photo_count, photos.size, maxPhotos),
            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2),
            verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
            photos.take(maxPhotos).forEachIndexed { index, photo ->
                val shape = RoundedCornerShape(CoffeePeekDimens.radiusMd)
                Box(Modifier.size(96.dp)) {
                    if (photo.isNotEmpty()) {
                        KamelImage(
                            resource = { asyncPainterResource(photo) },
                            contentDescription = stringResource(Res.string.shop_photo_attachment_description, index + 1),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(shape).border(1.dp, MaterialTheme.colorScheme.outline, shape),
                            onLoading = { PhotoUnavailable() },
                            onFailure = { PhotoUnavailable() },
                        )
                    } else PhotoUnavailable()
                    Box(Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = (-6).dp)
                        .size(24.dp).clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = 0.75f))
                        .border(1.5.dp, Color.White, RoundedCornerShape(50))
                        .clickable(enabled = enabled, role = Role.Button) { onRemove(index) },
                        contentAlignment = Alignment.Center) {
                        Icon(CoffeePeekIcons.Close,
                            contentDescription = stringResource(Res.string.shop_review_photo_remove, index + 1),
                            tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            }
            if (photos.size < maxPhotos) {
                Box(Modifier.size(96.dp).clip(RoundedCornerShape(CoffeePeekDimens.radiusMd))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(CoffeePeekDimens.radiusMd))
                    .clickable(enabled = enabled && !isLoading, role = Role.Button) { sourceShown = true },
                    contentAlignment = Alignment.Center) {
                    if (isLoading) CoffeePeekLoader(size = 40.dp, color = MaterialTheme.colorScheme.primary, strokeWidth = 4.dp)
                    else Icon(CoffeePeekIcons.Camera,
                        contentDescription = stringResource(Res.string.shop_review_photo_add),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(28.dp))
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

@Composable
private fun ShopPhotoAttachmentsLoadingPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    ShopPhotoAttachments(emptyList(), true, false, stringResource(Res.string.shop_review_photos),
        stringResource(Res.string.shop_review_photo_hint, 5), 5, {}, {}, {})
}
@Preview @Composable private fun ShopPhotoAttachmentsLoadingLightPreview() = ShopPhotoAttachmentsLoadingPreview(false)
@Preview @Composable private fun ShopPhotoAttachmentsLoadingDarkPreview() = ShopPhotoAttachmentsLoadingPreview(true)
