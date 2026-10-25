package com.coffeepeek.feature.favorites.impl.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.coffeePeekScrollContentPadding
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.favorites.domain.model.FavoriteShop
import com.coffeepeek.feature.favorites.domain.model.FavoriteRoaster
import com.coffeepeek.feature.favorites.domain.model.FavoriteDrink
import com.coffeepeek.feature.favorites.impl.ui.compose.component.FavoriteCard
import com.coffeepeek.feature.favorites.impl.ui.compose.component.FavoriteDrinkCard
import com.coffeepeek.feature.favorites.impl.ui.compose.component.FavoriteRoasterCard
import com.coffeepeek.feature.favorites.impl.ui.compose.model.FavoritesAction
import com.coffeepeek.feature.favorites.impl.ui.compose.model.FavoritesUiState
import com.coffeepeek.feature.favorites.impl.resources.Res
import com.coffeepeek.feature.favorites.impl.resources.favorites_drinks
import com.coffeepeek.feature.favorites.impl.resources.favorites_roasters
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun FavoritesList(
    state: FavoritesUiState,
    onAction: (FavoritesAction) -> Unit,
    distanceForCoordinates: (Double, Double) -> String?,
) {
    LazyColumn(
        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
        contentPadding = coffeePeekScrollContentPadding(PaddingValues(16.dp)),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        favoriteShops(state, onAction, distanceForCoordinates)
        favoriteRoasters(state, onAction)
        favoriteDrinks(state, onAction)
    }
}

private fun LazyListScope.favoriteShops(
    state: FavoritesUiState,
    onAction: (FavoritesAction) -> Unit,
    distanceForCoordinates: (Double, Double) -> String?,
) {
    items(state.shops, key = { "shop:${it.id}" }) { shop ->
        val distance = shop.latitude?.let { latitude ->
            shop.longitude?.let { longitude -> distanceForCoordinates(latitude, longitude) }
        }
        FavoriteCard(shop, shop.id in state.removing, distance,
            onOpen = { onAction(FavoritesAction.OpenShop(shop.id)) },
            onRemove = { onAction(FavoritesAction.Remove(shop.id)) })
    }
}

private fun LazyListScope.favoriteRoasters(state: FavoritesUiState, onAction: (FavoritesAction) -> Unit) {
    if (state.roasters.isNotEmpty()) item(key = "roasters_heading") {
        FavoritesSectionTitle(stringResource(Res.string.favorites_roasters))
    }
    items(state.roasters, key = { "roaster:${it.id}" }) { roaster ->
        FavoriteRoasterCard(roaster, roaster.id in state.removingRoasters,
            onOpen = { onAction(FavoritesAction.OpenRoaster(roaster.id)) },
            onRemove = { onAction(FavoritesAction.RemoveRoaster(roaster.id)) })
    }
}

private fun LazyListScope.favoriteDrinks(state: FavoritesUiState, onAction: (FavoritesAction) -> Unit) {
    if (state.drinks.isNotEmpty()) item(key = "drinks_heading") {
        FavoritesSectionTitle(stringResource(Res.string.favorites_drinks))
    }
    items(state.drinks, key = { "drink:${it.id}" }) { drink ->
        FavoriteDrinkCard(drink, drink.id in state.removingDrinks) {
            onAction(FavoritesAction.RemoveDrink(drink.id))
        }
    }
}

@Composable
private fun FavoritesSectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 12.dp))
}

@Composable
private fun FavoritesListPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    FavoritesList(FavoritesUiState(isLoading = false,
        shops = listOf(FavoriteShop("sample", "Кофейня", rating = 4.7)),
        roasters = listOf(FavoriteRoaster("sample", "Обжарщик", "sample")),
        drinks = listOf(FavoriteDrink("sample", "Капучино", null, true))), {}, { _, _ -> null })
}

@Preview @Composable private fun FavoritesListLightPreview() = FavoritesListPreview(false)
@Preview @Composable private fun FavoritesListDarkPreview() = FavoritesListPreview(true)
