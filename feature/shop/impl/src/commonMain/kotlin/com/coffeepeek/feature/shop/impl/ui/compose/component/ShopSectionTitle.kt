package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_menu_title
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ShopSectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(title, modifier.semantics { heading() }, style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurface)
}

@Preview @Composable private fun ShopSectionTitleLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopSectionTitle(stringResource(Res.string.shop_menu_title))
}
@Preview @Composable private fun ShopSectionTitleDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopSectionTitle(stringResource(Res.string.shop_menu_title))
}
