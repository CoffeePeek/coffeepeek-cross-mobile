package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coffeepeek.core.designsystem.component.CoffeePeekContentCard
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_description_title
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ShopDescriptionSection(description: String, modifier: Modifier = Modifier) {
    val text = description.trim().takeIf(String::isNotEmpty) ?: return
    CoffeePeekContentCard(modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
            ShopSectionTitle(stringResource(Res.string.shop_description_title))
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Preview @Composable private fun ShopDescriptionSectionLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopDescriptionSection("Небольшая кофейня со свежей обжаркой и спокойной атмосферой.")
}

@Preview @Composable private fun ShopDescriptionSectionDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopDescriptionSection("Небольшая кофейня со свежей обжаркой и спокойной атмосферой.")
}
