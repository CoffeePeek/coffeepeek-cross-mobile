package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.CpDimens
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_description_title
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ShopDescriptionSection(description: String, modifier: Modifier = Modifier) {
    val text = description.trim().takeIf(String::isNotEmpty) ?: return
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(CpDimens.spacing4), verticalArrangement = Arrangement.spacedBy(CpDimens.spacing2)) {
            Text(stringResource(Res.string.shop_description_title), style = MaterialTheme.typography.titleMedium)
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Preview @Composable private fun ShopDescriptionSectionLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopDescriptionSection("Небольшая кофейня со свежей обжаркой и спокойной атмосферой.")
}

@Preview @Composable private fun ShopDescriptionSectionDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopDescriptionSection("Небольшая кофейня со свежей обжаркой и спокойной атмосферой.")
}
