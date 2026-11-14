package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.runtime.Composable
import com.coffeepeek.core.designsystem.component.CoffeePeekLoader
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_detail_loading
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ShopDetailLoading() {
    CoffeePeekLoader(contentDescription = stringResource(Res.string.shop_detail_loading))
}

@Preview @Composable private fun ShopDetailLoadingLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopDetailLoading()
}
@Preview @Composable private fun ShopDetailLoadingDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopDetailLoading()
}
