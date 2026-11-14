package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.coffeepeek.core.designsystem.component.CoffeePeekTextButton
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_form_change_shop
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ShopCheckInFormHeader(shopName: String, shopAddress: String?, enabled: Boolean, onChange: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2),
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing1)) {
            Text(shopName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            shopAddress?.takeIf(String::isNotBlank)?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        CoffeePeekTextButton(onClick = onChange, enabled = enabled) {
            Text(stringResource(Res.string.shop_checkin_form_change_shop))
        }
    }
}
@Preview @Composable private fun ShopCheckInFormHeaderLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopCheckInFormHeader("The Coffee Room", "ул. Петра Мстиславца, 20", true, {})
}
@Preview @Composable private fun ShopCheckInFormHeaderDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopCheckInFormHeader("The Coffee Room", "ул. Петра Мстиславца, 20", true, {})
}
