package com.coffeepeek.feature.discovery.impl.ui.compose.component

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekGroupSection
import com.coffeepeek.core.designsystem.component.CoffeePeekPriceLevelSlider
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.discovery.domain.model.PriceLevel
import com.coffeepeek.feature.discovery.impl.resources.*
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun DiscoveryPriceSection(selected: PriceLevel?, onSelect: (PriceLevel?) -> Unit) {
    val hint = stringResource(when (selected?.level) {
        1 -> Res.string.discovery_price_low
        2 -> Res.string.discovery_price_medium
        3 -> Res.string.discovery_price_high
        else -> Res.string.discovery_price_any
    })
    CoffeePeekGroupSection(stringResource(Res.string.discovery_section_price), trailing = hint) {
        CoffeePeekPriceLevelSlider(selected?.level, { onSelect(PriceLevel.ofLevel(it)) },
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
    }
}

@Preview @Composable private fun DiscoveryPriceSectionLightPreview() = CoffeePeekTheme(darkTheme = false) { DiscoveryPriceSection(null, {}) }
@Preview @Composable private fun DiscoveryPriceSectionDarkPreview() = CoffeePeekTheme(darkTheme = true) { DiscoveryPriceSection(PriceLevel.ofLevel(2), {}) }
