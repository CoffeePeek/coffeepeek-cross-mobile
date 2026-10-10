package com.coffeepeek.feature.shop.impl.ui.compose

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.coffeepeek.core.designsystem.component.SwipeDismissModalBottomSheet
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_review_sheet_dismiss
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopReviewFormAction
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopReviewFormState
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Modal shell retains the current editor height; all durable form state stays outside Compose. */
@Composable
internal fun ShopReviewEditorSheet(
    state: ShopReviewFormState,
    onAction: (ShopReviewFormAction) -> Unit,
    onDismiss: () -> Unit,
    onPickFromGallery: (Int) -> Unit,
    onTakePhoto: () -> Unit,
    onOpenExistingPhoto: (List<String>, Int) -> Unit,
) {
    SwipeDismissModalBottomSheet(
        onDismissRequest = onDismiss,
        dismissDescription = stringResource(Res.string.shop_review_sheet_dismiss),
    ) {
        ShopReviewEditorContent(
            state = state,
            onAction = onAction,
            onPickFromGallery = onPickFromGallery,
            onTakePhoto = onTakePhoto,
            onOpenExistingPhoto = onOpenExistingPhoto,
            modifier = Modifier.fillMaxHeight(0.76f),
        )
    }
}

// Modal rendering may require Interactive/Run Preview in the IDE.
@Composable private fun ShopReviewEditorSheetPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    var shown by remember { mutableStateOf(true) }
    if (shown) ShopReviewEditorSheet(ShopReviewFormState(shopName = "Кофейня"), {},
        { shown = false }, {}, {}, { _, _ -> })
}

@Preview @Composable private fun ShopReviewEditorSheetLightPreview() = ShopReviewEditorSheetPreview(false)
@Preview @Composable private fun ShopReviewEditorSheetDarkPreview() = ShopReviewEditorSheetPreview(true)
