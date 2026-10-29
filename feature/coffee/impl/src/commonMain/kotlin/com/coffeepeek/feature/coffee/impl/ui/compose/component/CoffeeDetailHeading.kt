package com.coffeepeek.feature.coffee.impl.ui.compose.component

import androidx.compose.foundation.layout.Arrangement
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme

@Composable
internal fun CoffeeDetailHeading(name: String, countries: List<String>, productLabel: String?) {
    Column(verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
        Text(
            text = name,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2),
            verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2),
        ) {
            countries.forEach { CoffeeLabel(it) }
            productLabel?.let { CoffeeLabel(it) }
        }
    }
}

@Composable private fun CoffeeDetailHeadingPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Surface { CoffeeDetailHeading("Эфиопия", listOf("Эфиопия"), "Обжаренный кофе · В зёрнах") }
}
@Preview @Composable private fun CoffeeDetailHeadingLightPreview() = CoffeeDetailHeadingPreview(false)
@Preview @Composable private fun CoffeeDetailHeadingDarkPreview() = CoffeeDetailHeadingPreview(true)
