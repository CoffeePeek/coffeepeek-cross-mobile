package com.coffeepeek.feature.coffee.impl.ui.compose.component

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekCard
import com.coffeepeek.core.designsystem.component.CoffeePeekImagePlaceholder
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.coffee.domain.model.Coffee
import com.coffeepeek.feature.coffee.domain.model.CoffeeOffer

/** Image port owns loading/failure rendering. Fit product photos, preserving transparency. */
@Composable
internal fun CoffeeCard(
    coffee: Coffee,
    tasteNames: Map<String, String>,
    onOpen: () -> Unit,
    imageContent: @Composable (String, String, Modifier, ContentScale) -> Unit,
    modifier: Modifier = Modifier,
) {
    val offers = remember(coffee.offers) {
        coffee.offers.distinctBy { it.weightGrams }.sortedBy { it.weightGrams ?: Int.MAX_VALUE }
    }
    var selectedWeight by rememberSaveable(coffee.slug) {
        mutableStateOf(coffee.offers.firstOrNull()?.weightGrams)
    }
    val offer = offers.firstOrNull { it.weightGrams == selectedWeight } ?: offers.firstOrNull()
    CoffeePeekCard(
        modifier = modifier,
        onClick = onOpen,
        header = { CoffeeCardPhoto(coffee, imageContent) },
        contentPadding = PaddingValues(
            start = CoffeePeekDimens.spacing4,
            top = CoffeePeekDimens.spacing4,
            end = CoffeePeekDimens.spacing4,
            bottom = CoffeePeekDimens.spacing2,
        ),
        footer = {
            CoffeeCardOffers(
                offers = offers,
                selected = offer,
                onSelected = { selectedWeight = it.weightGrams },
                modifier = Modifier.padding(horizontal = CoffeePeekDimens.spacing4)
                    .padding(bottom = CoffeePeekDimens.spacing4),
            )
        },
    ) {
        CoffeeCardSummary(coffee, tasteNames)
    }
}

internal fun previewCoffee() = Coffee("fixture-coffee", "Эфиопия", null, "CoffeePeek", null,
    listOf("Эфиопия"), listOf("berries"), listOf(CoffeeOffer(24.5, "BYN", 250), CoffeeOffer(48.0, "BYN", 500)))

@Composable private fun CoffeeCardPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    CoffeeCard(previewCoffee(), mapOf("berries" to "Ягоды"), {}, { _, _, modifier, _ -> CoffeePeekImagePlaceholder(modifier) })
}
@Preview @Composable private fun CoffeeCardLightPreview() = CoffeeCardPreview(false)
@Preview @Composable private fun CoffeeCardDarkPreview() = CoffeeCardPreview(true)
