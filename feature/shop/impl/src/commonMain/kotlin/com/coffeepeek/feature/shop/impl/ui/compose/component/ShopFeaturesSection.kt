package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.CpDimens
import com.coffeepeek.feature.shop.domain.model.ShopFeature
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_features_collapse
import com.coffeepeek.feature.shop.impl.resources.shop_features_show_all
import com.coffeepeek.feature.shop.impl.resources.shop_features_title
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

private const val PREVIEW_FEATURE_COUNT = 3

@Composable
internal fun ShopFeaturesSection(
    features: List<ShopFeature>,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (features.isEmpty()) return
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(CpDimens.spacing4), verticalArrangement = Arrangement.spacedBy(CpDimens.spacing2)) {
            Text(stringResource(Res.string.shop_features_title), style = MaterialTheme.typography.titleMedium)
            val visible = if (expanded) features else features.take(PREVIEW_FEATURE_COUNT)
            visible.forEach { feature ->
                Row(Modifier.fillMaxWidth()) {
                    Text(feature.name, style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (features.size > PREVIEW_FEATURE_COUNT) {
                TextButton(onClick = onToggle) {
                    Text(if (expanded) stringResource(Res.string.shop_features_collapse)
                        else stringResource(Res.string.shop_features_show_all, features.size))
                }
            }
        }
    }
}

@Preview @Composable private fun ShopFeaturesSectionLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopFeaturesSection(previewFeatures(), expanded = false, onToggle = {})
}

@Preview @Composable private fun ShopFeaturesSectionDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopFeaturesSection(previewFeatures(), expanded = true, onToggle = {})
}

private fun previewFeatures() = listOf(
    ShopFeature("Эспрессо", "espresso", true),
    ShopFeature("Фильтр", "filter", true),
    ShopFeature("Wi-Fi", "wifi", false),
    ShopFeature("Можно с животными", "pets", false),
)
