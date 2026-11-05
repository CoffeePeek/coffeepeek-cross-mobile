package com.coffeepeek.feature.favorites.impl.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.coffeePeekScrollContentPadding
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.favorites.domain.model.FavoriteRoaster
import com.coffeepeek.feature.favorites.domain.model.FavoriteShop
import com.coffeepeek.feature.favorites.impl.resources.*
import com.coffeepeek.feature.favorites.impl.ui.compose.component.*
import com.coffeepeek.feature.favorites.impl.ui.compose.model.FavoritesAction
import com.coffeepeek.feature.favorites.impl.ui.compose.model.FavoritesUiState
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

/** State belongs to the destination, so returning from a detail preserves the user's place. */
@Composable
internal fun FavoritesList(state: FavoritesUiState, onAction: (FavoritesAction) -> Unit,
    distance: (Double, Double) -> String?, listState: LazyListState) {
    LazyColumn(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
        state = listState, contentPadding = coffeePeekScrollContentPadding(PaddingValues(16.dp)), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        favoriteShops(state, onAction, distance)
        favoriteRoasters(state, onAction)
        favoriteDrinks(state, onAction)
    }
}

private fun LazyListScope.favoriteShops(state: FavoritesUiState, action: (FavoritesAction) -> Unit, distance: (Double, Double) -> String?) {
    if (state.isLoading && state.shops.isEmpty()) item(key = "shops_loading") { FavoriteShopSkeleton() }
    items(state.shops, key = { "shop:${it.id}" }) { shop -> FavoriteShopRow(shop, shop.id in state.removing, action, distance) }
}

@Composable
private fun FavoriteShopRow(shop: FavoriteShop, removing: Boolean, action: (FavoritesAction) -> Unit, distance: (Double, Double) -> String?) {
    val label = shop.latitude?.let { latitude -> shop.longitude?.let { longitude -> distance(latitude, longitude) } }
    FavoriteCard(shop, removing, label,
        onOpen = { action(FavoritesAction.OpenShop(shop.id)) }, onRemove = { action(FavoritesAction.Remove(shop.id)) })
}

private fun LazyListScope.favoriteRoasters(state: FavoritesUiState, action: (FavoritesAction) -> Unit) {
    if (state.roasters.isNotEmpty() || state.areRoastersLoading) item(key = "roasters_heading") {
        FavoritesSectionHeading(stringResource(Res.string.favorites_roasters))
    }
    if (state.areRoastersLoading && state.roasters.isEmpty()) item(key = "roasters_loading") { FavoriteRoasterSkeleton() }
    items(state.roasters, key = { "roaster:${it.id}" }) { roaster ->
        FavoriteRoasterCard(roaster, roaster.id in state.removingRoasters,
            onOpen = { action(FavoritesAction.OpenRoaster(roaster.id)) }, onRemove = { action(FavoritesAction.RemoveRoaster(roaster.id)) })
    }
}

private fun LazyListScope.favoriteDrinks(state: FavoritesUiState, action: (FavoritesAction) -> Unit) {
    if (state.drinks.isNotEmpty()) item(key = "drinks_heading") { FavoritesSectionHeading(stringResource(Res.string.favorites_drinks)) }
    items(state.drinks, key = { "drink:${it.id}" }) { drink ->
        FavoriteDrinkCard(drink, drink.id in state.removingDrinks) { action(FavoritesAction.RemoveDrink(drink.id)) }
    }
}

@Composable
private fun FavoritesSectionHeading(title: String) {
    Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 12.dp))
}

@Composable
private fun FavoritesListSample(dark: Boolean, shopsLoading: Boolean = false) = CoffeePeekTheme(darkTheme = dark) {
    FavoritesList(FavoritesUiState(shops = if (shopsLoading) emptyList() else listOf(FavoriteShop("shop", "Любимая кофейня")),
        isLoading = shopsLoading, roasters = listOf(FavoriteRoaster("roaster", "Любимый обжарщик")), hasSettledInitialLoad = true),
        {}, { _, _ -> null }, rememberLazyListState())
}
@Preview @Composable private fun FavoritesListLightPreview() = FavoritesListSample(false)
@Preview @Composable private fun FavoritesListDarkPreview() = FavoritesListSample(true)
@Preview @Composable private fun FavoritesListRetryLightPreview() = FavoritesListSample(false, true)
@Preview @Composable private fun FavoritesListRetryDarkPreview() = FavoritesListSample(true, true)
