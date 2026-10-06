package com.coffeepeek.admin.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.icons.CpIcons
import com.coffeepeek.admin.utils.CpImage
import com.coffeepeek.admin.utils.PickedImage
import com.coffeepeek.admin.utils.rememberPhotoPicker

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PhotoAttachmentsSection(
    photos: List<PickedImage>,
    maxPhotos: Int,
    onPhotosAdded: (List<PickedImage>) -> Unit,
    onRemovePhoto: (Int) -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Фотографии",
    hint: String = "Добавьте до $maxPhotos фото.",
) {
    var isPhotoLoading by remember { mutableStateOf(false) }
    var showPhotoSourceSheet by remember { mutableStateOf(false) }
    val remaining = (maxPhotos - photos.size).coerceAtLeast(1)
    val photoPicker = rememberPhotoPicker(
        maxSelection = remaining,
        isLoading = { isPhotoLoading = it },
        onPhotosPicked = onPhotosAdded,
    )

    if (showPhotoSourceSheet) {
        PhotoSourceBottomSheet(
            onDismiss = { showPhotoSourceSheet = false },
            onGallery = {
                showPhotoSourceSheet = false
                photoPicker.pickFromGallery()
            },
            onCamera = {
                showPhotoSourceSheet = false
                photoPicker.takePhoto()
            },
        )
    }

    Column(modifier = modifier) {
        Text(title, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(CpDimens.spacing1))
        Text(
            text = hint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(CpDimens.spacing2))
        Text(
            text = "Добавлено: ${photos.size}/$maxPhotos",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(CpDimens.spacing2))

        if (photos.isNotEmpty() || photos.size < maxPhotos) {
            val photoShape = RoundedCornerShape(CpDimens.radiusMd)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing2),
                verticalArrangement = Arrangement.spacedBy(CpDimens.spacing2),
                modifier = Modifier.padding(bottom = CpDimens.spacing2),
            ) {
                photos.forEachIndexed { index, photo ->
                    // Outer box is NOT clipped so the delete badge can overhang the corner and stay visible.
                    Box(modifier = Modifier.size(96.dp)) {
                        CpImage(
                            data = photo.bytes,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(photoShape)
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline,
                                    shape = photoShape,
                                ),
                            contentScale = ContentScale.Crop,
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 6.dp, y = (-6).dp)
                                .size(24.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color.Black.copy(alpha = 0.75f))
                                .border(1.5.dp, Color.White, RoundedCornerShape(50))
                                .clickable { onRemovePhoto(index) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                CpIcons.Close,
                                contentDescription = "Удалить",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }
                if (photos.size < maxPhotos) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(photoShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline,
                                shape = photoShape,
                            )
                            .clickable(
                                enabled = !isPhotoLoading,
                                onClickLabel = "Добавить фото",
                                onClick = { showPhotoSourceSheet = true },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isPhotoLoading) {
                            CoffeePeekLoader(size = CpDimens.loaderButton, strokeWidth = 2.dp)
                        } else {
                            Icon(
                                imageVector = CpIcons.Camera,
                                contentDescription = "Добавить фото",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
