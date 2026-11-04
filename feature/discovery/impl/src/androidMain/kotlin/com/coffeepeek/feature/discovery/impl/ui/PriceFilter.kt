package com.coffeepeek.feature.discovery.impl.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekBynPriceIndicators
import com.coffeepeek.core.designsystem.component.CoffeePeekDiscreteSlider
import com.coffeepeek.core.designsystem.component.CoffeePeekGroupSection
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.coffeePeekAccentForeground
import com.coffeepeek.feature.discovery.domain.model.PriceLevel
import com.coffeepeek.feature.discovery.impl.R
import com.coffeepeek.feature.discovery.impl.search.priceFilterStops
import com.coffeepeek.feature.discovery.impl.search.priceFilterStop

@Composable
internal fun PriceFilter(selected: PriceLevel?, onSelect: (PriceLevel?) -> Unit) {
    val stop = priceFilterStop(selected)
    val displayed = priceFilterStops[stop]
    val label = priceLabel(displayed)
    val description = displayed?.let { stringResource(R.string.discovery_price_level_description, label, stop) } ?: label
    CoffeePeekGroupSection(title = stringResource(R.string.discovery_section_price), trailing = label) {
        BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
            // Match the slider's first/last stop to the centers of the equally sized label cells.
            val inset = (maxWidth / (priceFilterStops.size * 2) - 14.dp).coerceAtLeast(0.dp)
            Column {
                CoffeePeekDiscreteSlider(stop, priceFilterStops.lastIndex,
                    { onSelect(priceFilterStops[it]) }, stringResource(R.string.discovery_price_label), description,
                    Modifier.fillMaxWidth().padding(horizontal = inset))
                PriceStops(displayed)
            }
        }
    }
}

@Composable
private fun PriceStops(selected: PriceLevel?) {
    Row(Modifier.fillMaxWidth().clearAndSetSemantics {}) {
        priceFilterStops.forEach { level ->
            val tint = if (selected == level) MaterialTheme.colorScheme.coffeePeekAccentForeground else MaterialTheme.colorScheme.onSurfaceVariant
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                if (level == null) Text(stringResource(R.string.discovery_price_any), style = MaterialTheme.typography.labelSmall, color = tint)
                else CoffeePeekBynPriceIndicators(level.level, iconWidth = 9.dp, tint = tint, spacing = 1.dp)
            }
        }
    }
}

@Composable
private fun priceLabel(level: PriceLevel?): String = stringResource(when (level) {
    null -> R.string.discovery_price_any
    PriceLevel.Cheap -> R.string.discovery_price_cheap
    PriceLevel.Moderate -> R.string.discovery_price_moderate
    PriceLevel.Expensive, PriceLevel.Luxury -> R.string.discovery_price_expensive
})

@PreviewLightDark
@Composable
private fun PriceFilterPreview() = CoffeePeekTheme {
    Surface { PriceFilter(PriceLevel.Luxury) {} }
}
