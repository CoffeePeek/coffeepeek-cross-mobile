package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import com.coffeepeek.core.designsystem.component.CoffeePeekOutlinedButton as OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekIconBadgePalette
import com.coffeepeek.core.designsystem.component.CoffeePeekSettingsRow
import com.coffeepeek.core.designsystem.component.CoffeePeekSettingsDivider
import com.coffeepeek.core.designsystem.component.CoffeePeekSwipeDismissModalBottomSheet
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_review_photo_source_camera
import com.coffeepeek.feature.shop.impl.resources.shop_review_photo_source_cancel
import com.coffeepeek.feature.shop.impl.resources.shop_review_photo_source_gallery
import com.coffeepeek.feature.shop.impl.resources.shop_review_photo_source_title
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ShopPhotoSourceSheet(
    onDismiss: () -> Unit,
    onGallery: () -> Unit,
    onCamera: () -> Unit,
) {
    CoffeePeekSwipeDismissModalBottomSheet(
        onDismissRequest = onDismiss,
        dismissDescription = stringResource(Res.string.shop_review_photo_source_cancel),
    ) {
        ShopPhotoSourceSheetContent(onDismiss, onGallery, onCamera)
    }
}

@Composable
internal fun ShopPhotoSourceSheetContent(onDismiss: () -> Unit, onGallery: () -> Unit, onCamera: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = CoffeePeekDimens.spacing4)
        .padding(top = CoffeePeekDimens.spacing6, bottom = CoffeePeekDimens.spacing6),
        verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing4)) {
        Text(stringResource(Res.string.shop_review_photo_source_title), style = MaterialTheme.typography.titleLarge)
        Surface(shape = RoundedCornerShape(CoffeePeekDimens.radiusXl),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)
                .compositeOver(MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
            Column {
                CoffeePeekSettingsRow(CoffeePeekIcons.Gallery,
                    stringResource(Res.string.shop_review_photo_source_gallery), onClick = onGallery,
                    iconColors = CoffeePeekIconBadgePalette.Gold)
                CoffeePeekSettingsDivider()
                CoffeePeekSettingsRow(CoffeePeekIcons.Camera,
                    stringResource(Res.string.shop_review_photo_source_camera), onClick = onCamera,
                    iconColors = CoffeePeekIconBadgePalette.Gold)
            }
        }
        OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            shape = RoundedCornerShape(CoffeePeekDimens.buttonRadius),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)) {
            Text(stringResource(Res.string.shop_review_photo_source_cancel), style = MaterialTheme.typography.labelLarge)
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

@Preview @Composable private fun ShopPhotoSourceContentLightPreview() = CoffeePeekTheme(darkTheme = false) {
    Surface { ShopPhotoSourceSheetContent({}, {}, {}) }
}
@Preview @Composable private fun ShopPhotoSourceContentDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    Surface { ShopPhotoSourceSheetContent({}, {}, {}) }
}
