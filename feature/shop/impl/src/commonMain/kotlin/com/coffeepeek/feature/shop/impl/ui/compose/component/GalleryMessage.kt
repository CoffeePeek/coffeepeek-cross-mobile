package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.coffeepeek.core.designsystem.component.CoffeePeekButton
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_menu_gallery_empty
import com.coffeepeek.feature.shop.impl.resources.shop_menu_gallery_error
import com.coffeepeek.feature.shop.impl.resources.shop_menu_gallery_retry
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun GalleryMessage(hasError: Boolean, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.padding(CoffeePeekDimens.spacing4),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3),
    ) {
        Text(
            stringResource(if (hasError) Res.string.shop_menu_gallery_error else Res.string.shop_menu_gallery_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (hasError) CoffeePeekButton(stringResource(Res.string.shop_menu_gallery_retry), onRetry)
    }
}

@Preview @Composable private fun GalleryMessageLightPreview() = CoffeePeekTheme(darkTheme = false) {
    GalleryMessage(true, {})
}

@Preview @Composable private fun GalleryMessageDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    GalleryMessage(false, {})
}
