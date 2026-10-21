package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import org.jetbrains.compose.ui.tooling.preview.Preview

/** No hidden author/text/photo is composed, including on platforms without blur support. */
@Composable
internal fun ShopCheckInGuestPlaceholder(modifier: Modifier = Modifier) {
    Card(modifier.clearAndSetSemantics {}, shape = RoundedCornerShape(CoffeePeekDimens.radiusLg),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
        Column(Modifier.padding(CoffeePeekDimens.spacing4).height(160.dp),
            verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
            repeat(4) { index ->
                Surface(Modifier.fillMaxWidth(if (index == 0) 0.5f else 0.85f).height(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small) {}
            }
        }
    }
}

@Preview @Composable private fun ShopCheckInGuestPlaceholderLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopCheckInGuestPlaceholder()
}

@Preview @Composable private fun ShopCheckInGuestPlaceholderDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopCheckInGuestPlaceholder()
}
