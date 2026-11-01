package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.coffeepeek.core.designsystem.resources.*
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Local image rendering belongs to the caller; this component never loads or uploads photos. */
@Composable
fun <T : Any> CoffeePeekPhotoAttachments(
    photos: List<T>,
    enabled: Boolean,
    isLoading: Boolean,
    maxPhotos: Int,
    onMove: (from: Int, to: Int) -> Unit,
    onRemove: (Int) -> Unit,
    onAdd: () -> Unit,
    photo: @Composable (T, Modifier, ContentScale) -> Unit,
    modifier: Modifier = Modifier,
) {
    val drag = remember(photos) { CoffeePeekPhotoDragState() }
    var preview by remember(photos, enabled) { mutableStateOf<T?>(null) }
    val editable = enabled && !isLoading
    Column(modifier, verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
        if (photos.isNotEmpty()) Text(stringResource(Res.string.coffeepeek_photos_manage_hint),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2),
            verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
            photos.forEachIndexed { index, item -> key(item) {
                CoffeePeekPhotoAttachment(index, photos.size, editable, drag,
                    onOpen = { preview = item }, onMove = onMove, onRemove = { onRemove(index) },
                    photo = { imageModifier -> photo(item, imageModifier, ContentScale.Crop) })
            } }
            if (photos.size < maxPhotos) CoffeePeekPhotoAddButton(editable, isLoading, onAdd)
        }
    }
    val previewIndex = photos.indexOf(preview)
    if (enabled && previewIndex >= 0) CoffeePeekPhotoViewer(photos.size, previewIndex, { preview = null }) { index, imageModifier ->
        photo(photos[index], imageModifier, ContentScale.Fit)
    }
}

@Preview @Composable private fun CoffeePeekPhotoAttachmentsLightPreview() = PhotoAttachmentsPreview(false)
@Preview @Composable private fun CoffeePeekPhotoAttachmentsDarkPreview() = PhotoAttachmentsPreview(true)

@Composable
private fun PhotoAttachmentsPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    CoffeePeekPhotoAttachments(listOf(1, 2, 3, 4), true, false, 5, { _, _ -> }, {}, {},
        photo = { _, modifier, _ -> CoffeePeekImagePlaceholder(modifier) })
}
