package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekBynPriceIndicators
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shop.domain.model.ShopMenuItem
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_menu_byn_price_description
import com.coffeepeek.feature.shop.impl.ui.data.formatShopMenuAmount
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ShopMenuDrinkRow(item: ShopMenuItem, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(vertical = CoffeePeekDimens.spacing1),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
        Text(item.nameRu.ifBlank { item.nameEn.ifBlank { item.slug } }, Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge)
        item.price?.takeIf(Double::isFinite)?.let { price ->
            val amount = formatShopMenuAmount(price)
            val isByn = item.currency.isBlank() || item.currency.equals("BYN", ignoreCase = true)
            val priceDescription = if (isByn) stringResource(Res.string.shop_menu_byn_price_description, amount)
                else "$amount ${item.currency}"
            Row(Modifier.clearAndSetSemantics { contentDescription = priceDescription },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(amount, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                if (isByn) CoffeePeekBynPriceIndicators(count = 1, iconWidth = 16.dp,
                    tint = MaterialTheme.colorScheme.onSurface)
                else Text(item.currency, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Preview @Composable private fun ShopMenuDrinkRowLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopMenuDrinkRow(previewMenuDrink())
}
@Preview @Composable private fun ShopMenuDrinkRowDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopMenuDrinkRow(previewMenuDrink())
}
private fun previewMenuDrink() = ShopMenuItem("flat-white", "Флэт уайт", "Flat white",
    "Espresso", "Present", 5.5, "BYN", 250)
