package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekLoader
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_detail_refreshing
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ShopDetailRefreshStatus(modifier: Modifier = Modifier) {
    val description = stringResource(Res.string.shop_detail_refreshing)
    Row(modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2),
        verticalAlignment = Alignment.CenterVertically) {
        CoffeePeekLoader(contentDescription = description, size = 18.dp)
        Text(description, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Preview @Composable private fun ShopDetailRefreshLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopDetailRefreshStatus()
}
@Preview @Composable private fun ShopDetailRefreshDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopDetailRefreshStatus()
}
