package com.coffeepeek.feature.coffee.impl.ui.compose.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekCapsuleSegmentedControl
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.coffee.domain.model.CoffeeOffer
import com.coffeepeek.feature.coffee.impl.R

@Composable
internal fun CoffeeCardOffers(
    offers: List<CoffeeOffer>,
    selected: CoffeeOffer?,
    onSelected: (CoffeeOffer) -> Unit,
    modifier: Modifier = Modifier,
) {
    val weightLabels = offers.associateWith { offer ->
        offer.weightGrams?.let { stringResource(R.string.coffee_weight, it) }
            ?: stringResource(R.string.coffee_weight_short_missing)
    }
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        CoffeePrice(selected)
        if (offers.size > 1 && selected != null) {
            CoffeePeekCapsuleSegmentedControl(
                options = offers,
                selected = selected,
                label = weightLabels::getValue,
                onSelected = onSelected,
                modifier = Modifier.width((offers.size * 56).dp),
            )
        } else {
            CoffeeLabel(selected?.weightGrams?.let { stringResource(R.string.coffee_weight, it) }
                ?: stringResource(R.string.coffee_weight_missing))
        }
    }
}

@PreviewLightDark @Composable private fun CoffeeCardOffersPreview() = CoffeePeekTheme {
    Surface { CoffeeCardOffers(previewCoffee().offers, previewCoffee().offers.first(), {}) }
}
