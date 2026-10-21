package com.coffeepeek.admin.ui.screen.shop.component

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.shop_menu_gallery_open
import com.coffeepeek.admin.theme.CoffeePeekTheme
import org.jetbrains.compose.resources.stringResource

/** Application-owned affordance; routing stays outside the stateless component. */
@Composable
internal fun ShopMenuGalleryLink(onOpen: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(onClick = onOpen, modifier = modifier) {
        Text(stringResource(Res.string.shop_menu_gallery_open))
    }
}

@PreviewLightDark
@Composable
private fun ShopMenuGalleryLinkPreview() {
    CoffeePeekTheme { ShopMenuGalleryLink(onOpen = {}) }
}
