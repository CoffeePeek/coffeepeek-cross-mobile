package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.coffeepeek.core.designsystem.component.CoffeePeekLoader
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_list_loading
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_list_refreshing
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_list_load_error
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_list_refresh_error
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_list_vote_error
import com.coffeepeek.feature.shop.impl.resources.shop_detail_retry
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ShopCheckInListStatus(
    isLoading: Boolean,
    hasLoaded: Boolean,
    hasLoadError: Boolean,
    hasVoteError: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.semantics { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
        if (isLoading) {
            CoffeePeekLoader(contentDescription = stringResource(if (hasLoaded)
                Res.string.shop_checkin_list_refreshing else Res.string.shop_checkin_list_loading))
        }
        if (hasLoadError) {
            Text(stringResource(if (hasLoaded) Res.string.shop_checkin_list_refresh_error
                else Res.string.shop_checkin_list_load_error), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = onRetry) { Text(stringResource(Res.string.shop_detail_retry)) }
        }
        if (hasVoteError) {
            Text(stringResource(Res.string.shop_checkin_list_vote_error), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error)
        }
    }
}

@Preview @Composable private fun ShopCheckInListStatusLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopCheckInListStatus(false, true, true, true, {})
}
@Preview @Composable private fun ShopCheckInListStatusDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopCheckInListStatus(false, true, true, true, {})
}
@Preview @Composable private fun ShopCheckInListLoadingLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopCheckInListStatus(true, false, false, false, {})
}
@Preview @Composable private fun ShopCheckInListLoadingDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopCheckInListStatus(true, false, false, false, {})
}
