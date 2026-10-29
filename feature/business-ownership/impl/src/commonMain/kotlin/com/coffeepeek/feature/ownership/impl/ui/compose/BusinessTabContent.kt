package com.coffeepeek.feature.ownership.impl.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekImagePlaceholder
import com.coffeepeek.core.designsystem.component.CoffeePeekPullToRefresh
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.ownership.api.OwnershipImage
import com.coffeepeek.feature.ownership.domain.model.BusinessKind
import com.coffeepeek.feature.ownership.domain.model.BusinessPage
import com.coffeepeek.feature.ownership.domain.model.OwnedBusiness
import com.coffeepeek.feature.ownership.impl.resources.Res
import com.coffeepeek.feature.ownership.impl.resources.ownership_business
import com.coffeepeek.feature.ownership.impl.resources.ownership_loading
import com.coffeepeek.feature.ownership.impl.resources.ownership_refresh
import com.coffeepeek.feature.ownership.impl.resources.ownership_roasters
import com.coffeepeek.feature.ownership.impl.resources.ownership_shops
import com.coffeepeek.feature.ownership.impl.ui.compose.model.OwnershipAction
import com.coffeepeek.feature.ownership.impl.ui.compose.model.OwnershipState
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun BusinessTabContent(state: OwnershipState, action: (OwnershipAction) -> Unit,
    image: OwnershipImage, open: (BusinessKind, String) -> Unit) {
    val listState = rememberLazyListState()
    CoffeePeekPullToRefresh(listState, state.loading, { action(OwnershipAction.Refresh) },
        stringResource(Res.string.ownership_refresh), stringResource(Res.string.ownership_loading),
        modifier = Modifier.fillMaxSize().statusBarsPadding()) { scrollModifier ->
        LazyColumn(Modifier.fillMaxSize().then(scrollModifier), state = listState,
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 116.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            businessTabItems(state, action, image, open)
        }
    }
}

private fun LazyListScope.businessTabItems(state: OwnershipState, action: (OwnershipAction) -> Unit,
    image: OwnershipImage, open: (BusinessKind, String) -> Unit) {
    item { Text(stringResource(Res.string.ownership_business), style = MaterialTheme.typography.headlineLarge) }
    if (state.saved || state.problem != null || state.busy) item { OwnershipFeedback(state, action) }
    item { Text(stringResource(Res.string.ownership_shops), style = MaterialTheme.typography.titleLarge) }
    items(state.shops, key = { "shop:${it.id}" }) { business ->
        BusinessShopCard(business, state.cardProfiles[BusinessKind.CoffeeShop to business.id], image,
            onClick = { open(BusinessKind.CoffeeShop, business.id) })
    }
    val roasters = state.roasters?.items.orEmpty()
    if (roasters.isNotEmpty()) {
        item { Text(stringResource(Res.string.ownership_roasters), style = MaterialTheme.typography.titleLarge) }
        items(roasters, key = { "roaster:${it.id}" }) { business ->
            BusinessRoasterCard(business, state.cardProfiles[BusinessKind.Roaster to business.id], image,
                onClick = { open(BusinessKind.Roaster, business.id) })
        }
        state.roasters?.takeIf { it.page > 1 || it.hasNext }?.let { page ->
            item { OwnershipPager(page.page, page.hasNext) { action(OwnershipAction.RoastersPage(it)) } }
        }
    }
}

@Composable private fun BusinessTabPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    val shop = OwnedBusiness("shop", BusinessKind.CoffeeShop, "Why Knot?")
    val roaster = OwnedBusiness("roaster", BusinessKind.Roaster, "CoffeePeek Roasters")
    BusinessTabContent(OwnershipState(account = "preview", shops = listOf(shop),
        roasters = BusinessPage(listOf(roaster), 1, 1, 20)), {},
        { _, _, modifier, _ -> CoffeePeekImagePlaceholder(modifier) }, { _, _ -> })
}

@Preview @Composable private fun BusinessTabLightPreview() = BusinessTabPreview(false)
@Preview @Composable private fun BusinessTabDarkPreview() = BusinessTabPreview(true)
