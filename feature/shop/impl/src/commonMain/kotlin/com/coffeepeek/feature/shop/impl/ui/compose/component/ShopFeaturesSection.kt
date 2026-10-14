package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.feature.shop.domain.model.ShopFeature
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_features_collapse
import com.coffeepeek.feature.shop.impl.resources.shop_features_show_all_count
import com.coffeepeek.feature.shop.impl.resources.shop_features_title
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

private const val PREVIEW_FEATURE_COUNT = 5

@Composable
internal fun ShopFeaturesSection(
    features: List<ShopFeature>,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (features.isEmpty()) return
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
        ShopSectionTitle(stringResource(Res.string.shop_features_title))
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(CoffeePeekDimens.radius2xl),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
            Column(Modifier.padding(CoffeePeekDimens.spacing4), verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing1)) {
                val visible = if (expanded) features else features.take(PREVIEW_FEATURE_COUNT)
                visible.forEach { feature ->
                    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
                        ShopFeatureIcon(feature, Modifier.size(28.dp))
                        Text(feature.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    }
                }
                if (features.size > PREVIEW_FEATURE_COUNT) {
                    val label = if (expanded) stringResource(Res.string.shop_features_collapse)
                        else pluralStringResource(Res.plurals.shop_features_show_all_count, features.size, features.size)
                    Surface(Modifier.fillMaxWidth().heightIn(min = 52.dp)
                        .clickable(role = Role.Button, onClickLabel = label, onClick = onToggle),
                        shape = RoundedCornerShape(CoffeePeekDimens.radiusLg),
                        color = MaterialTheme.colorScheme.surfaceVariant) {
                        Text(label, Modifier.padding(CoffeePeekDimens.spacing3), style = MaterialTheme.typography.titleMedium,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
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
    ShopFeature("Можно с животными", "pet_friendly", false),
    ShopFeature("Зерно с собой", "beans-for-sale", false),
    ShopFeature("Растительное молоко", "plant-based-milk", false),
    ShopFeature("Выпечка", "bakery", false),
)
