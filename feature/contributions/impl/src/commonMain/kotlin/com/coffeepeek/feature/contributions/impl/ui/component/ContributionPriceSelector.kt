package com.coffeepeek.feature.contributions.impl.ui.component

import androidx.compose.runtime.Composable
import com.coffeepeek.core.designsystem.component.CoffeePeekPriceLevelSlider
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.contributions.domain.model.PriceRange
import com.coffeepeek.feature.contributions.impl.resources.*
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

internal fun contributionPriceValue(level: Int?): String? = when (level) {
    1 -> "< 8 BYN"
    2 -> "8 BYN"
    3 -> "> 8 BYN"
    else -> null
}

@Composable
internal fun ContributionPriceSelector(selected: PriceRange?, onSelect: (PriceRange?) -> Unit) {
    val level = selected?.let { it.ordinal + 1 }
    val title = contributionPriceValue(level)?.let { stringResource(Res.string.contributions_price_cappuccino, it) }
        ?: stringResource(Res.string.contributions_price_title)
    CoffeePeekPriceLevelSlider(level, { onSelect(it?.let { value -> PriceRange.entries.getOrNull(value - 1) }) }, title = title)
}

@Preview @Composable private fun ContributionPriceLightPreview() = CoffeePeekTheme(darkTheme = false) { ContributionPriceSelector(PriceRange.Moderate, {}) }
@Preview @Composable private fun ContributionPriceDarkPreview() = CoffeePeekTheme(darkTheme = true) { ContributionPriceSelector(null, {}) }
