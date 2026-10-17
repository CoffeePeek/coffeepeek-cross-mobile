package com.coffeepeek.feature.homecheckin.impl.ui.compose.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationUpload
import com.coffeepeek.feature.homecheckin.impl.R

@Composable
internal fun HomeCheckInEditorPhotos(
    photos: List<HomePreparationUpload>, enabled: Boolean, pickerLoading: Boolean,
    photo: @Composable (HomePreparationUpload, Modifier, Boolean) -> Unit,
    onAdd: (remaining: Int) -> Unit, onRemove: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.home_check_in_photos), style = MaterialTheme.typography.labelLarge)
        Text(stringResource(R.string.home_check_in_photos_hint), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(stringResource(R.string.home_check_in_photos_count, photos.size), style = MaterialTheme.typography.labelMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 8.dp)) {
            val shape = RoundedCornerShape(12.dp)
            photos.forEachIndexed { index, upload ->
                Box(Modifier.size(96.dp)) {
                    val description = stringResource(R.string.home_check_in_photo_description, index + 1, photos.size)
                    photo(upload, Modifier.fillMaxSize().clip(shape).border(1.dp, MaterialTheme.colorScheme.outline, shape)
                        .semantics { contentDescription = description }, false)
                    IconButton(onClick = { onRemove(index) }, enabled = enabled && !pickerLoading,
                        modifier = Modifier.align(Alignment.TopEnd).offset(x = 18.dp, y = (-18).dp).size(48.dp)) {
                        Icon(CoffeePeekIcons.Close, stringResource(R.string.home_check_in_remove_photo, index + 1),
                            Modifier.size(24.dp).background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(50))
                                .border(1.5.dp, Color.White, RoundedCornerShape(50)).padding(4.dp),
                            tint = Color.White)
                    }
                }
            }
            if (photos.size < 5) {
                val description = stringResource(R.string.home_check_in_add_photos)
                Box(Modifier.size(96.dp).clip(shape).background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, shape)
                    .clickable(enabled = enabled && !pickerLoading, role = Role.Button) { onAdd(5 - photos.size) }
                    .semantics { contentDescription = description }, contentAlignment = Alignment.Center) {
                    if (pickerLoading) com.coffeepeek.core.designsystem.component.CoffeePeekLoader(description, size = 20.dp)
                    else Icon(CoffeePeekIcons.Camera, null, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@PreviewLightDark @Composable private fun HomeCheckInEditorPhotosPreview() = CoffeePeekTheme {
    Surface { HomeCheckInEditorPhotos(emptyList(), true, false, { _, modifier, _ -> Box(modifier) }, {}, {}) }
}
