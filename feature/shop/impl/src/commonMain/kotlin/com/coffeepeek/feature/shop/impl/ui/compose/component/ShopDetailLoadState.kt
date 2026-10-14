package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.coffeepeek.core.designsystem.component.CoffeePeekButton
import com.coffeepeek.core.designsystem.component.CoffeePeekLoader
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_detail_error
import com.coffeepeek.feature.shop.impl.resources.shop_detail_loading
import com.coffeepeek.feature.shop.impl.resources.shop_detail_retry
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ShopDetailLoadState(isLoading: Boolean, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.padding(CoffeePeekDimens.spacing6),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
        if (isLoading) {
            CoffeePeekLoader(contentDescription = stringResource(Res.string.shop_detail_loading))
        } else {
            Text(stringResource(Res.string.shop_detail_error), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            CoffeePeekButton(stringResource(Res.string.shop_detail_retry), onRetry)
        }
    }
}

@Preview @Composable private fun ShopDetailLoadingLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopDetailLoadState(true, {})
}
@Preview @Composable private fun ShopDetailLoadingDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopDetailLoadState(true, {})
}
@Preview @Composable private fun ShopDetailErrorLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopDetailLoadState(false, {})
}
@Preview @Composable private fun ShopDetailErrorDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopDetailLoadState(false, {})
}
