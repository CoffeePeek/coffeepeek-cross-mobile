package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ShopCheckInFormHeader(shopName: String, shopAddress: String?) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing1)) {
        Text(shopName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        shopAddress?.takeIf(String::isNotBlank)?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
@Preview @Composable private fun ShopCheckInFormHeaderLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopCheckInFormHeader("The Coffee Room", "ул. Петра Мстиславца, 20")
}
@Preview @Composable private fun ShopCheckInFormHeaderDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopCheckInFormHeader("The Coffee Room", "ул. Петра Мстиславца, 20")
}
