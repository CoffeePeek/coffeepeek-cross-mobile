package com.coffeepeek.feature.homecheckin.impl.ui.compose.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekPhotoAttachments
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationUpload
import com.coffeepeek.feature.homecheckin.impl.R

@Composable
internal fun HomeCheckInEditorPhotos(
    photos: List<HomePreparationUpload>, enabled: Boolean, pickerLoading: Boolean,
    photo: @Composable (HomePreparationUpload, Modifier, Boolean) -> Unit,
    onAdd: (remaining: Int) -> Unit, onRemove: (Int) -> Unit, onMove: (Int, Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.home_check_in_photos), style = MaterialTheme.typography.labelLarge)
        Text(stringResource(R.string.home_check_in_photos_hint), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(stringResource(R.string.home_check_in_photos_count, photos.size), style = MaterialTheme.typography.labelMedium)
        CoffeePeekPhotoAttachments(photos, enabled, pickerLoading, 5, onMove, onRemove, { onAdd(5 - photos.size) },
            photo = { upload, modifier, scale -> photo(upload, modifier, scale == ContentScale.Fit) },
            modifier = Modifier.padding(bottom = 8.dp))
    }
}

@PreviewLightDark @Composable private fun HomeCheckInEditorPhotosPreview() = CoffeePeekTheme {
    val photos = List(4) { HomePreparationUpload("photo-$it.jpg", "image/jpeg", byteArrayOf()) }
    Surface { HomeCheckInEditorPhotos(photos, true, false, { _, modifier, _ -> Box(modifier) }, {}, {}, { _, _ -> }) }
}
