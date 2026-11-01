package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.layout.padding
import com.coffeepeek.core.designsystem.component.CoffeePeekFilledButton as Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_detail_route
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ShopRouteButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(onClick = onClick, modifier = modifier) {
        Icon(CoffeePeekIcons.Navigation, contentDescription = null)
        Text(stringResource(Res.string.shop_detail_route), Modifier.padding(start = CoffeePeekDimens.spacing2))
    }
}

@Preview @Composable private fun ShopRouteButtonLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopRouteButton(onClick = {})
}

@Preview @Composable private fun ShopRouteButtonDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopRouteButton(onClick = {})
}
