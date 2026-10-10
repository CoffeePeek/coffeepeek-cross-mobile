package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_checkins_create
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ShopCheckInButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(onClick = onClick, modifier = modifier) { Text(stringResource(Res.string.shop_checkins_create)) }
}

@Preview @Composable private fun ShopCheckInButtonLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopCheckInButton(onClick = {})
}

@Preview @Composable private fun ShopCheckInButtonDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopCheckInButton(onClick = {})
}
