package com.coffeepeek.feature.contributions.impl.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.*
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.contributions.api.ContributionPhotoPicker
import com.coffeepeek.feature.contributions.domain.model.PendingPhoto
import com.coffeepeek.feature.contributions.impl.resources.*
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ContributionPhotos(photos: List<PendingPhoto>, max: Int, title: String, hint: String,
    onAdd: (List<PendingPhoto>) -> Unit, onRemove: (Int) -> Unit, picker: ContributionPhotoPicker) {
    val controller = picker.rememberController()
    var sourceShown by remember { mutableStateOf(false) }
    val remaining by rememberUpdatedState((max - photos.size).coerceAtLeast(0))
    val add by rememberUpdatedState(onAdd)
    val delivery = remember { ContributionPhotoDelivery({ remaining }, { add(it) }) }
    DisposableEffect(delivery) { onDispose { delivery.dispose() } }
    if (sourceShown && !controller.isPreparing && remaining > 0) CoffeePeekPhotoSourceSheet(
        onDismiss = { sourceShown = false },
        onGallery = {
            sourceShown = false
            delivery.request(remaining)?.let { controller.pickFromGallery(remaining, it) }
        },
        onCamera = {
            sourceShown = false
            delivery.request(minOf(1, remaining))?.let(controller.takePhoto)
        },
    )
    ContributionPhotosContent(photos, max, title, hint, controller.isPreparing, onRemove, { sourceShown = true })
}

@Composable
internal fun ContributionPhotosContent(photos: List<PendingPhoto>, max: Int, title: String, hint: String,
    isLoading: Boolean, onRemove: (Int) -> Unit, onAdd: () -> Unit) {
    Column {
        Text(title, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(CoffeePeekDimens.spacing1))
        Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(CoffeePeekDimens.spacing2))
        Text(stringResource(Res.string.contributions_photo_count, photos.size, max), style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(CoffeePeekDimens.spacing2))
        CoffeePeekPhotoAttachments(photos, true, isLoading, max, { _, _ -> }, onRemove, onAdd,
            photo = { item, modifier, scale -> CoffeePeekImage(data = item.bytes, modifier = modifier, contentScale = scale) },
            modifier = Modifier.padding(bottom = CoffeePeekDimens.spacing2), mode = CoffeePeekPhotoAttachmentMode.Simple)
    }
}

@Composable private fun ContributionPhotosPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    ContributionPhotosContent(emptyList(), 5, "Фотографии", "Добавьте до 5 фото.", false, {}, {})
}
@Preview @Composable private fun ContributionPhotosLightPreview() = ContributionPhotosPreview(false)
@Preview @Composable private fun ContributionPhotosDarkPreview() = ContributionPhotosPreview(true)
