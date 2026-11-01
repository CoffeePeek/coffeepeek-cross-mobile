package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_checkins_empty
import com.coffeepeek.feature.shop.impl.resources.shop_maskot_with_book
import org.jetbrains.compose.resources.painterResource
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ShopCheckInsEmptyState(shopTitle: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
        Image(painterResource(Res.drawable.shop_maskot_with_book), null, modifier = Modifier.size(160.dp))
        Text(stringResource(Res.string.shop_checkins_empty, shopTitle), style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Preview @Composable private fun ShopCheckInsEmptyLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopCheckInsEmptyState("Кофейня")
}
@Preview @Composable private fun ShopCheckInsEmptyDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopCheckInsEmptyState("Кофейня")
}
