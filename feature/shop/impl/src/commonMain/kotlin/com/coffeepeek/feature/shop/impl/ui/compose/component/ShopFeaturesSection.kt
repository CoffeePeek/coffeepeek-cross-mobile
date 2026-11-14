package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import com.coffeepeek.core.designsystem.text.coffeePeekPluralForm
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.reducedMotionDuration
import com.coffeepeek.feature.shop.domain.model.ShopFeature
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_features_collapse
import com.coffeepeek.feature.shop.impl.resources.shop_features_show_all_count_few
import com.coffeepeek.feature.shop.impl.resources.shop_features_show_all_count_many
import com.coffeepeek.feature.shop.impl.resources.shop_features_show_all_count_one
import com.coffeepeek.feature.shop.impl.resources.shop_features_title
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
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
                features.take(PREVIEW_FEATURE_COUNT).forEach { ShopFeatureRow(it) }
                if (features.size > PREVIEW_FEATURE_COUNT) {
                    ShopAdditionalFeatures(features.drop(PREVIEW_FEATURE_COUNT), expanded)
                    ShopFeaturesToggle(features.size, expanded, onToggle)
                }
            }
        }
    }
}

@Composable
private fun ShopFeatureRow(feature: ShopFeature) {
    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
        ShopFeatureIcon(feature, Modifier.size(28.dp))
        Text(feature.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ShopAdditionalFeatures(features: List<ShopFeature>, expanded: Boolean) {
    val duration = reducedMotionDuration(250)
    AnimatedVisibility(expanded,
        enter = expandVertically(tween(duration), expandFrom = Alignment.Top) + fadeIn(tween(duration)),
        exit = shrinkVertically(tween(duration), shrinkTowards = Alignment.Top) + fadeOut(tween(duration))) {
        Column(verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing1)) {
            features.forEach { ShopFeatureRow(it) }
        }
    }
}

@Composable
private fun ShopFeaturesToggle(count: Int, expanded: Boolean, onToggle: () -> Unit) {
    val label = if (expanded) stringResource(Res.string.shop_features_collapse)
        else stringResource(coffeePeekPluralForm(count, Res.string.shop_features_show_all_count_one,
            Res.string.shop_features_show_all_count_few, Res.string.shop_features_show_all_count_many), count)
    Surface(Modifier.fillMaxWidth().heightIn(min = 52.dp)
        .clickable(role = Role.Button, onClickLabel = label, onClick = onToggle),
        shape = RoundedCornerShape(CoffeePeekDimens.radiusLg),
        color = MaterialTheme.colorScheme.surfaceVariant) {
        Text(label, Modifier.padding(CoffeePeekDimens.spacing3), style = MaterialTheme.typography.titleMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
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
