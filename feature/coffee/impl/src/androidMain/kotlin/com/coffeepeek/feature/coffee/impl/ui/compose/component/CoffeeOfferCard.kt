package com.coffeepeek.feature.coffee.impl.ui.compose.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekButton
import com.coffeepeek.core.designsystem.component.CoffeePeekContentCard
import com.coffeepeek.core.designsystem.theme.CoffeePeekColors
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.coffee.domain.model.CoffeeAvailability
import com.coffeepeek.feature.coffee.domain.model.CoffeeOffer
import com.coffeepeek.feature.coffee.impl.R
import com.coffeepeek.feature.coffee.impl.ui.data.coffeeProductFormLabel

@Composable
internal fun CoffeeOfferCard(offer: CoffeeOffer, brewNames: Map<String, String>, formatCheckedAt: (String) -> String, onOpenSeller: () -> Unit) {
    CoffeePeekContentCard {
        Column(verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing4)) {
            val (label, color) = when (offer.availability) {
                CoffeeAvailability.InStock -> R.string.coffee_in_stock to CoffeePeekColors.Success
                CoffeeAvailability.OutOfStock -> R.string.coffee_out_of_stock to CoffeePeekColors.Error
                CoffeeAvailability.PreOrder -> R.string.coffee_preorder to CoffeePeekColors.Warning
                CoffeeAvailability.Unknown -> R.string.coffee_availability_unknown to MaterialTheme.colorScheme.onSurfaceVariant
            }
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2), itemVerticalAlignment = Alignment.CenterVertically) {
                Text(offer.weightGrams?.let { stringResource(R.string.coffee_weight, it) } ?: stringResource(R.string.coffee_weight_missing),
                    style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(color))
                    Text(stringResource(label), style = MaterialTheme.typography.bodySmall)
                }
            }
            CoffeePrice(offer)
            CoffeePeekButton(stringResource(R.string.coffee_open_seller), onOpenSeller, enabled = offer.sourceUrl != null)
            val fields = listOf(
                stringResource(R.string.coffee_seller) to (offer.sellerName ?: stringResource(R.string.coffee_unspecified)),
                stringResource(R.string.coffee_grind) to (coffeeProductFormLabel(offer.grind) ?: stringResource(R.string.coffee_unspecified)),
                stringResource(R.string.coffee_brew) to (brewNames[offer.brewPurpose] ?: stringResource(R.string.coffee_unconfirmed)),
                stringResource(R.string.coffee_source) to stringResource(when (offer.availabilityScope) {
                    "online" -> R.string.coffee_online
                    "offline" -> R.string.coffee_offline
                    "both" -> R.string.coffee_both
                    else -> R.string.coffee_unspecified
                }),
            ) + listOfNotNull(offer.checkedAtUtc?.let { stringResource(R.string.coffee_checked) to formatCheckedAt(it) })
            fields.forEach { (title, value) ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing4)) {
                    Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(value, Modifier.weight(1.2f), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@PreviewLightDark @Composable private fun CoffeeOfferCardPreview() = CoffeePeekTheme {
    CoffeeOfferCard(previewCoffee().offers.first().copy(availability = CoffeeAvailability.InStock, sellerName = "CoffeePeek"),
        emptyMap(), { it }, {})
}
