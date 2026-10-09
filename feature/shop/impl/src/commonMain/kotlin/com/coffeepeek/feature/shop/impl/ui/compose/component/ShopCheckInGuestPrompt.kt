package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.CpDimens
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_checkins_sign_in_prompt
import com.coffeepeek.feature.shop.impl.resources.shop_reviews_sign_in
import com.coffeepeek.feature.shop.impl.resources.shop_reviews_register
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ShopCheckInGuestPrompt(onSignIn: () -> Unit, onRegister: () -> Unit) {
    Card {
        Column(Modifier.padding(CpDimens.spacing4)) {
            Text(stringResource(Res.string.shop_checkins_sign_in_prompt))
            Row {
                TextButton(onClick = onSignIn) { Text(stringResource(Res.string.shop_reviews_sign_in)) }
                TextButton(onClick = onRegister) { Text(stringResource(Res.string.shop_reviews_register)) }
            }
        }
    }
}

@Preview @Composable private fun ShopCheckInGuestPromptLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopCheckInGuestPrompt(onSignIn = {}, onRegister = {})
}

@Preview @Composable private fun ShopCheckInGuestPromptDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopCheckInGuestPrompt(onSignIn = {}, onRegister = {})
}
