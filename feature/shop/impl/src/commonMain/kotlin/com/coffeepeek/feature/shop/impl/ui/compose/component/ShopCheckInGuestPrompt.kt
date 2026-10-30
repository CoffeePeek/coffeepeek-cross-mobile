package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.coffeepeek.core.designsystem.component.CoffeePeekTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekButton
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_checkins_join_prompt
import com.coffeepeek.feature.shop.impl.resources.shop_checkins_no_account
import com.coffeepeek.feature.shop.impl.resources.shop_reviews_sign_in
import com.coffeepeek.feature.shop.impl.resources.shop_reviews_register
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ShopCheckInGuestPrompt(onSignIn: () -> Unit, onRegister: () -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(CoffeePeekDimens.cardRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.fillMaxWidth().padding(CoffeePeekDimens.spacing4),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
            Text(stringResource(Res.string.shop_checkins_join_prompt),
                style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            CoffeePeekButton(stringResource(Res.string.shop_reviews_sign_in), onSignIn)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(Res.string.shop_checkins_no_account),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = onRegister, colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface)) {
                    Text(stringResource(Res.string.shop_reviews_register))
                }
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
