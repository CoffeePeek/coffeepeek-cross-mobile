package com.coffeepeek.feature.coffee.impl.ui.compose.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.coffeepeek.core.designsystem.component.CoffeePeekExpandableText
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.coffee.domain.model.CoffeeDetails
import com.coffeepeek.feature.coffee.impl.R
import com.coffeepeek.feature.coffee.impl.ui.data.coffeeProductFormLabel
import com.coffeepeek.feature.coffee.impl.ui.data.coffeeProductKindLabel

@Composable
internal fun CoffeeDetailIntro(
    details: CoffeeDetails,
    labels: Map<String, Map<String, String>>,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val productLabel = listOfNotNull(
        coffeeProductKindLabel(details.productKind),
        coffeeProductFormLabel(details.productForm),
    ).takeIf { it.isNotEmpty() }?.joinToString(" · ")
    val tasteNames = details.coffee.tasteCodes.mapNotNull(labels["taste"].orEmpty()::get)

    Column(verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
        CoffeeDetailHeading(details.coffee.name, details.coffee.countries, productLabel)
        details.description?.let { description ->
            CoffeePeekExpandableText(
                text = description,
                expanded = expanded,
                onToggle = onToggle,
                expandLabel = stringResource(R.string.coffee_read_more),
                collapseLabel = stringResource(R.string.coffee_collapse),
            )
        }
        CoffeeDetailTraits(
            roast = labels["roast"]?.get(details.roastLevel),
            profile = tasteNames.takeIf { it.isNotEmpty() }?.joinToString(", "),
            acidity = labels["acidity"]?.get(details.acidity),
        )
    }
}

@PreviewLightDark @Composable private fun CoffeeDetailIntroPreview() = CoffeePeekTheme {
    Surface { CoffeeDetailIntro(previewCoffeeDetails(), emptyMap(), false, {}) }
}

internal fun previewCoffeeDetails() = CoffeeDetails(previewCoffee(), "/coffees/fixture-coffee", "Сладкий кофе с ягодным послевкусием",
    emptyList(), "fixture-roaster", "roasted_beans", "whole_beans", null, null, listOf("Ягоды"), null)
