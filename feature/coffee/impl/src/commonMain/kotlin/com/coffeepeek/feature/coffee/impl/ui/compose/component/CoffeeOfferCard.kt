package com.coffeepeek.feature.coffee.impl.ui.compose.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekButton
import com.coffeepeek.core.designsystem.component.CoffeePeekContentCard
import com.coffeepeek.core.designsystem.theme.CoffeePeekColors
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.coffee.domain.model.CoffeeAvailability
import com.coffeepeek.feature.coffee.domain.model.CoffeeOffer
import com.coffeepeek.feature.coffee.impl.resources.*
import com.coffeepeek.feature.coffee.impl.ui.data.coffeeProductFormLabel

@Composable
internal fun CoffeeOfferCard(offer: CoffeeOffer, brewNames: Map<String, String>, formatCheckedAt: (String) -> String, onOpenSeller: () -> Unit) {
    CoffeePeekContentCard {
        Column(verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing4)) {
            val (label, color) = when (offer.availability) {
                CoffeeAvailability.InStock -> Res.string.coffee_in_stock to CoffeePeekColors.Success
                CoffeeAvailability.OutOfStock -> Res.string.coffee_out_of_stock to CoffeePeekColors.Error
                CoffeeAvailability.PreOrder -> Res.string.coffee_preorder to CoffeePeekColors.Warning
                CoffeeAvailability.Unknown -> Res.string.coffee_availability_unknown to MaterialTheme.colorScheme.onSurfaceVariant
            }
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2), itemVerticalAlignment = Alignment.CenterVertically) {
                Text(offer.weightGrams?.let { stringResource(Res.string.coffee_weight, it) } ?: stringResource(Res.string.coffee_weight_missing),
                    style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(color))
                    Text(stringResource(label), style = MaterialTheme.typography.bodySmall)
                }
            }
            CoffeePrice(offer)
            CoffeePeekButton(stringResource(Res.string.coffee_open_seller), onOpenSeller, enabled = offer.sourceUrl != null)
            val fields = listOf(
                stringResource(Res.string.coffee_seller) to (offer.sellerName ?: stringResource(Res.string.coffee_unspecified)),
                stringResource(Res.string.coffee_grind) to (coffeeProductFormLabel(offer.grind) ?: stringResource(Res.string.coffee_unspecified)),
                stringResource(Res.string.coffee_brew) to (brewNames[offer.brewPurpose] ?: stringResource(Res.string.coffee_unconfirmed)),
                stringResource(Res.string.coffee_source) to stringResource(when (offer.availabilityScope) {
                    "online" -> Res.string.coffee_online
                    "offline" -> Res.string.coffee_offline
                    "both" -> Res.string.coffee_both
                    else -> Res.string.coffee_unspecified
                }),
            ) + listOfNotNull(offer.checkedAtUtc?.let { stringResource(Res.string.coffee_checked) to formatCheckedAt(it) })
            fields.forEach { (title, value) ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing4)) {
                    Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(value, Modifier.weight(1.2f), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable private fun CoffeeOfferCardPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    CoffeeOfferCard(previewCoffee().offers.first().copy(availability = CoffeeAvailability.InStock, sellerName = "CoffeePeek"),
        emptyMap(), { it }, {})
}
@Preview @Composable private fun CoffeeOfferCardLightPreview() = CoffeeOfferCardPreview(false)
@Preview @Composable private fun CoffeeOfferCardDarkPreview() = CoffeeOfferCardPreview(true)
