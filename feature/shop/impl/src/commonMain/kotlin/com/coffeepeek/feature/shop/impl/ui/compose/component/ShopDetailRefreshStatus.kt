package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.coffeepeek.core.designsystem.component.CoffeePeekTextButton as TextButton
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
import com.coffeepeek.feature.shop.impl.resources.shop_detail_refresh_error
import com.coffeepeek.feature.shop.impl.resources.shop_detail_retry
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Refresh never replaces already loaded content with a full-screen spinner/error. */
@Composable
internal fun ShopDetailRefreshStatus(isLoading: Boolean, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val description = stringResource(if (isLoading) Res.string.shop_detail_refreshing
        else Res.string.shop_detail_refresh_error)
    Row(modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2),
        verticalAlignment = Alignment.CenterVertically) {
        if (isLoading) CoffeePeekLoader(contentDescription = description, size = 18.dp)
        Text(description, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (!isLoading) TextButton(onClick = onRetry) { Text(stringResource(Res.string.shop_detail_retry)) }
    }
}

@Preview @Composable private fun ShopDetailRefreshLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopDetailRefreshStatus(true, {})
}
@Preview @Composable private fun ShopDetailRefreshDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopDetailRefreshStatus(true, {})
}
@Preview @Composable private fun ShopDetailRefreshErrorLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopDetailRefreshStatus(false, {})
}
@Preview @Composable private fun ShopDetailRefreshErrorDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopDetailRefreshStatus(false, {})
}
