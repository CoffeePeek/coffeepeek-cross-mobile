package com.coffeepeek.feature.coffee.impl.ui.compose.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekBynIcon
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.coffee.domain.model.CoffeeOffer
import com.coffeepeek.feature.coffee.impl.resources.*
import com.coffeepeek.feature.coffee.impl.ui.data.formatCoffeePrice

@Composable
internal fun CoffeePrice(offer: CoffeeOffer?) {
    val price = offer?.price
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing1)) {
        Text(price?.let(::formatCoffeePrice) ?: stringResource(Res.string.coffee_price_missing),
            style = if (price != null) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodySmall,
            fontWeight = if (price != null) FontWeight.Bold else FontWeight.Normal)
        if (price != null) {
            if (offer.currency == "BYN") {
                val description = stringResource(Res.string.coffee_byn)
                CoffeePeekBynIcon(Modifier.size(20.dp).semantics { contentDescription = description },
                    tint = MaterialTheme.colorScheme.onSurface)
            } else Text(offer.currency.orEmpty(), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable private fun CoffeePricePreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Surface { CoffeePrice(CoffeeOffer(24.5, "BYN", 250)) }
}
@Preview @Composable private fun CoffeePriceLightPreview() = CoffeePricePreview(false)
@Preview @Composable private fun CoffeePriceDarkPreview() = CoffeePricePreview(true)
