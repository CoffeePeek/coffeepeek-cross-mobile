package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shop.domain.model.ShopCheckInPhoto
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_drink_photo_title
import com.coffeepeek.feature.shop.impl.resources.shop_drink_photo_help
import com.coffeepeek.feature.shop.impl.resources.shop_drink_photo_processing
import com.coffeepeek.feature.shop.impl.resources.shop_drink_photo_failed
import com.coffeepeek.feature.shop.impl.resources.shop_drink_photo_choose
import com.coffeepeek.feature.shop.impl.resources.shop_drink_photo_skipped
import com.coffeepeek.feature.shop.impl.resources.shop_drink_photo_retry
import com.coffeepeek.feature.shop.impl.resources.shop_drink_photo_skip
import com.coffeepeek.feature.shop.impl.resources.shop_drink_photo_description
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ShopDrinkPhotoField(
    candidates: List<ShopCheckInPhoto>, selectedPhoto: ShopCheckInPhoto?,
    processing: Boolean, failed: Boolean, skipped: Boolean, enabled: Boolean,
    onChoose: (Int) -> Unit, onRetry: () -> Unit, onSkip: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
        Text(stringResource(Res.string.shop_drink_photo_title), style = MaterialTheme.typography.labelLarge)
        Text(stringResource(Res.string.shop_drink_photo_help), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (processing) Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
            CircularProgressIndicator(Modifier.size(24.dp))
            Text(stringResource(Res.string.shop_drink_photo_processing),
                Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        }
        val status = when {
            failed -> Res.string.shop_drink_photo_failed
            skipped -> Res.string.shop_drink_photo_skipped
            candidates.size > 1 && selectedPhoto == null -> Res.string.shop_drink_photo_choose
            else -> null
        }
        status?.let { Text(stringResource(it), Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            color = if (failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant) }
        FlowRow(modifier = Modifier.selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2),
            verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
            candidates.ifEmpty { listOfNotNull(selectedPhoto) }.forEachIndexed { index, photo ->
                val label = stringResource(Res.string.shop_drink_photo_description, index + 1)
                Surface(Modifier.size(96.dp).semantics {
                    contentDescription = label
                }.selectable(selected = photo == selectedPhoto,
                    enabled = enabled && !processing && candidates.isNotEmpty(), role = Role.RadioButton) { onChoose(index) },
                    shape = RoundedCornerShape(CoffeePeekDimens.radiusMd),
                    border = BorderStroke(if (photo == selectedPhoto) 2.dp else 1.dp,
                        if (photo == selectedPhoto) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)) {
                    if (photo.bytes.isEmpty()) CutoutPlaceholder()
                    else KamelImage(resource = { asyncPainterResource(photo.bytes) }, contentDescription = null,
                        contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize(),
                        onLoading = { CutoutPlaceholder() }, onFailure = { CutoutPlaceholder() })
                }
            }
        }
        Row {
            if (failed || skipped) TextButton(onClick = onRetry, enabled = enabled && !processing) {
                Text(stringResource(Res.string.shop_drink_photo_retry))
            }
            if (!skipped) TextButton(onClick = onSkip, enabled = enabled) {
                Text(stringResource(Res.string.shop_drink_photo_skip))
            }
        }
    }
}

@Composable private fun CutoutPlaceholder() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(CoffeePeekIcons.Coffee, null) }
}
@Preview @Composable private fun ShopDrinkPhotoProcessingLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopDrinkPhotoField(emptyList(), null, true, false, false, true, {}, {}, {})
}
@Preview @Composable private fun ShopDrinkPhotoProcessingDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopDrinkPhotoField(emptyList(), null, true, false, false, true, {}, {}, {})
}
@Preview @Composable private fun ShopDrinkPhotoFailureLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopDrinkPhotoField(emptyList(), null, false, true, false, true, {}, {}, {})
}
@Preview @Composable private fun ShopDrinkPhotoFailureDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopDrinkPhotoField(emptyList(), null, false, true, false, true, {}, {}, {})
}
@Preview @Composable private fun ShopDrinkPhotoChoiceLightPreview() = CoffeePeekTheme(darkTheme = false) {
    val candidates = previewDrinkPhotos()
    ShopDrinkPhotoField(candidates, candidates.first(), false, false, false, true, {}, {}, {})
}
@Preview @Composable private fun ShopDrinkPhotoChoiceDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    val candidates = previewDrinkPhotos()
    ShopDrinkPhotoField(candidates, candidates.first(), false, false, false, true, {}, {}, {})
}

/** Empty local image placeholders keep previews independent of image fetching and application DI. */
private fun previewDrinkPhotos() = List(2) { ShopCheckInPhoto(byteArrayOf(), "drink_$it.png", "image/png") }
