package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.coffeepeek.core.designsystem.component.SwipeDismissModalBottomSheet
import com.coffeepeek.core.designsystem.icons.CpIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.CpDimens
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_review_photo_source_camera
import com.coffeepeek.feature.shop.impl.resources.shop_review_photo_source_cancel
import com.coffeepeek.feature.shop.impl.resources.shop_review_photo_source_gallery
import com.coffeepeek.feature.shop.impl.resources.shop_review_photo_source_title
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ShopPhotoSourceSheet(
    onDismiss: () -> Unit,
    onGallery: () -> Unit,
    onCamera: () -> Unit,
) {
    SwipeDismissModalBottomSheet(
        onDismissRequest = onDismiss,
        dismissDescription = stringResource(Res.string.shop_review_photo_source_cancel),
    ) {
        Column(Modifier.fillMaxWidth().padding(CpDimens.spacing4)) {
            Text(stringResource(Res.string.shop_review_photo_source_title),
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
            TextButton(onClick = onGallery, modifier = Modifier.fillMaxWidth()) {
                Icon(CpIcons.Gallery, contentDescription = null)
                Text(stringResource(Res.string.shop_review_photo_source_gallery))
            }
            TextButton(onClick = onCamera, modifier = Modifier.fillMaxWidth()) {
                Icon(CpIcons.Camera, contentDescription = null)
                Text(stringResource(Res.string.shop_review_photo_source_camera))
            }
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.shop_review_photo_source_cancel))
            }
        }
    }
}

// Modal rendering may require Interactive/Run Preview in the IDE.
@Composable private fun ShopPhotoSourceSheetPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    var shown by remember { mutableStateOf(true) }
    if (shown) ShopPhotoSourceSheet({ shown = false }, {}, {})
}

@Preview @Composable private fun ShopPhotoSourceSheetLightPreview() = ShopPhotoSourceSheetPreview(false)
@Preview @Composable private fun ShopPhotoSourceSheetDarkPreview() = ShopPhotoSourceSheetPreview(true)
