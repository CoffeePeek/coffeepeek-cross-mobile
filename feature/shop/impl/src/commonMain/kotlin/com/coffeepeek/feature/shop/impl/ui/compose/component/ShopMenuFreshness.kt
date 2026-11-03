package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_menu_captured
import com.coffeepeek.feature.shop.impl.resources.shop_menu_updated
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ShopMenuFreshness(
    capturedLabel: String?,
    updatedLabel: String?,
    modifier: Modifier = Modifier,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    if (capturedLabel.isNullOrBlank() && updatedLabel.isNullOrBlank()) return
    Column(modifier) {
        if (!capturedLabel.isNullOrBlank()) {
            Text(stringResource(Res.string.shop_menu_captured, capturedLabel),
                style = MaterialTheme.typography.labelSmall, color = contentColor)
        }
        if (!updatedLabel.isNullOrBlank() && updatedLabel != capturedLabel) {
            Text(stringResource(Res.string.shop_menu_updated, updatedLabel),
                style = MaterialTheme.typography.labelSmall, color = contentColor)
        }
    }
}

@Preview @Composable private fun ShopMenuFreshnessLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopMenuFreshness("02.10.2026", "03.10.2026")
}
@Preview @Composable private fun ShopMenuFreshnessDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopMenuFreshness("02.10.2026", "03.10.2026")
}
