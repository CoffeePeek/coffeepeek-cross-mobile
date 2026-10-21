package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekColors
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_coffee_beans
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ShopCoffeeCatalogGroup(title: String, items: List<String>, modifier: Modifier = Modifier) {
    if (items.isEmpty()) return
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
        ShopSectionTitle(title)
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(CoffeePeekDimens.radius2xl),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
            FlowRow(Modifier.padding(CoffeePeekDimens.spacing4),
                horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2),
                verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
                items.forEach { item ->
                    Surface(color = CoffeePeekColors.GoldWarmSoft, shape = RoundedCornerShape(CoffeePeekDimens.radiusSm)) {
                        Text(item, Modifier.padding(horizontal = CoffeePeekDimens.spacing2, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium, color = CoffeePeekColors.GoldWarmHover)
                    }
                }
            }
        }
    }
}

@Preview @Composable private fun ShopCoffeeCatalogGroupLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopCoffeeCatalogGroup(stringResource(Res.string.shop_coffee_beans), listOf("Эфиопия", "Бразилия"))
}
@Preview @Composable private fun ShopCoffeeCatalogGroupDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopCoffeeCatalogGroup(stringResource(Res.string.shop_coffee_beans), listOf("Эфиопия", "Бразилия"))
}
