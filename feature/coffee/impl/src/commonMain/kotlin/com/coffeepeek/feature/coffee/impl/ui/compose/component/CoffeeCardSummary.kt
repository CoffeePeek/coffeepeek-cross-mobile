package com.coffeepeek.feature.coffee.impl.ui.compose.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.coffee.domain.model.Coffee

@Composable
internal fun CoffeeCardSummary(coffee: Coffee, tasteNames: Map<String, String>) {
    Column(verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
        Text(
            text = coffee.name,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (coffee.countries.isNotEmpty()) {
            Text(
                text = coffee.countries.joinToString(", "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val tastes = coffee.tasteCodes.mapNotNull(tasteNames::get)
        if (tastes.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing1),
                verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing1),
            ) { tastes.forEach { CoffeeLabel(it) } }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable private fun CoffeeCardSummaryPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Surface { CoffeeCardSummary(previewCoffee(), mapOf("berries" to "Ягоды")) }
}
@Preview @Composable private fun CoffeeCardSummaryLightPreview() = CoffeeCardSummaryPreview(false)
@Preview @Composable private fun CoffeeCardSummaryDarkPreview() = CoffeeCardSummaryPreview(true)
