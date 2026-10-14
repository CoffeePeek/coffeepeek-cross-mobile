package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekBynPriceIndicators
import com.coffeepeek.core.designsystem.component.CoffeePeekSwipeDismissModalBottomSheet
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_price_average_hint
import com.coffeepeek.feature.shop.impl.resources.shop_price_dismiss
import com.coffeepeek.feature.shop.impl.resources.shop_price_high_hint
import com.coffeepeek.feature.shop.impl.resources.shop_price_low_hint
import com.coffeepeek.feature.shop.impl.resources.shop_price_selected
import com.coffeepeek.feature.shop.impl.resources.shop_price_title
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopPriceLevel
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ShopPriceInfoSheet(level: ShopPriceLevel, onDismiss: () -> Unit) {
    CoffeePeekSwipeDismissModalBottomSheet(onDismiss, stringResource(Res.string.shop_price_dismiss)) {
        ShopPriceInfoContent(level)
    }
}

@Composable
private fun ShopPriceInfoContent(selectedLevel: ShopPriceLevel) {
    val selectedDescription = stringResource(Res.string.shop_price_selected)
    Column(Modifier.fillMaxWidth().padding(horizontal = CoffeePeekDimens.spacing4)
        .padding(bottom = CoffeePeekDimens.spacing6), verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
        Text(stringResource(Res.string.shop_price_title), style = MaterialTheme.typography.titleLarge)
        ShopPriceLevel.entries.forEach { level ->
            val selected = selectedLevel == level
            Row(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium)
                .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                .semantics(mergeDescendants = true) { if (selected) stateDescription = selectedDescription }
                .padding(CoffeePeekDimens.spacing3), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
                CoffeePeekBynPriceIndicators(level.indicatorCount, Modifier.width(64.dp), iconWidth = 16.dp,
                    maxCount = ShopPriceLevel.entries.size)
                Text(stringResource(when (level) {
                    ShopPriceLevel.Low -> Res.string.shop_price_low_hint
                    ShopPriceLevel.Average -> Res.string.shop_price_average_hint
                    ShopPriceLevel.High -> Res.string.shop_price_high_hint
                }), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                if (selected) Icon(CoffeePeekIcons.Check, null, Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

// Preview content avoids requiring an interactive modal window in the IDE.
@Preview @Composable private fun ShopPriceInfoSheetLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopPriceInfoContent(ShopPriceLevel.Average)
}

@Preview @Composable private fun ShopPriceInfoSheetDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopPriceInfoContent(ShopPriceLevel.Average)
}
