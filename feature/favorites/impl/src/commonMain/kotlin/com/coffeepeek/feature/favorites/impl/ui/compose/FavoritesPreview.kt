package com.coffeepeek.feature.favorites.impl.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coffeepeek.core.designsystem.component.CoffeePeekLoader
import com.coffeepeek.core.designsystem.component.CoffeePeekStackedCardCarousel
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.favorites.domain.model.FavoriteShop
import com.coffeepeek.feature.favorites.domain.model.FavoriteRoaster
import com.coffeepeek.feature.favorites.domain.model.FavoriteDrink
import com.coffeepeek.feature.favorites.impl.ui.FavoritesViewModel
import com.coffeepeek.feature.favorites.impl.ui.compose.component.FavoriteCard
import com.coffeepeek.feature.favorites.impl.ui.compose.component.FavoriteRoasterCard
import com.coffeepeek.feature.favorites.impl.ui.compose.component.FavoriteDrinkCard
import com.coffeepeek.feature.favorites.impl.ui.compose.model.FavoritesAction
import com.coffeepeek.feature.favorites.impl.ui.compose.model.FavoritesUiState
import com.coffeepeek.feature.favorites.impl.resources.*
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun FavoritesPreviewScreen(
    viewModel: FavoritesViewModel,
    onOpenShop: (String) -> Unit,
    onOpenRoaster: (String) -> Unit,
    distanceForCoordinates: (Double, Double) -> String?,
    header: @Composable () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveFavoritesEvents(viewModel, onOpenShop, onOpenRoaster)
    FavoritesPreviewContent(state, viewModel::onAction, distanceForCoordinates, header)
}

@Composable
internal fun FavoritesPreviewContent(
    state: FavoritesUiState,
    onAction: (FavoritesAction) -> Unit,
    distanceForCoordinates: (Double, Double) -> String?,
    header: @Composable () -> Unit,
) {
    if (!state.shouldShowPreview()) return
    val empty = state.shops.isEmpty() && state.roasters.isEmpty() && state.drinks.isEmpty()
    val loading = state.isLoading || state.areRoastersLoading || state.areDrinksLoading
    val failed = state.loadFailed || state.roastersLoadFailed || state.drinksLoadFailed
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.padding(horizontal = 16.dp)) { header() }
        if (state.actionFailed) PreviewMessage(stringResource(Res.string.favorites_action_error),
            stringResource(Res.string.favorites_dismiss)) { onAction(FavoritesAction.DismissActionFailure) }
        when {
            empty && loading -> Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                CoffeePeekLoader(stringResource(Res.string.favorites_loading))
            }
            else -> {
                if (failed) PreviewMessage(stringResource(Res.string.favorites_load_error),
                    stringResource(Res.string.favorites_retry)) { onAction(FavoritesAction.Retry) }
                FavoritesPreviewCards(state, onAction, distanceForCoordinates)
            }
        }
    }
}

@Composable
private fun PreviewMessage(message: String, action: String, onClick: () -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp)) {
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick = onClick) { Text(action) }
    }
}

@Composable
private fun FavoritesPreviewCards(
    state: FavoritesUiState,
    onAction: (FavoritesAction) -> Unit,
    distanceForCoordinates: (Double, Double) -> String?,
) {
    CoffeePeekStackedCardCarousel<PreviewFavorite>(
        state.shops.map { PreviewFavorite.Shop(it) },
        state.roasters.map { PreviewFavorite.Roaster(it) },
        state.drinks.map { PreviewFavorite.Drink(it) },
        itemKey = { it.id },
    ) { item, modifier ->
        when (item) {
            is PreviewFavorite.Shop -> PreviewShopCard(item.value, state, modifier, onAction, distanceForCoordinates)
            is PreviewFavorite.Roaster -> FavoriteRoasterCard(item.value, item.id in state.removingRoasters,
                { onAction(FavoritesAction.OpenRoaster(item.id)) },
                { onAction(FavoritesAction.RemoveRoaster(item.id)) }, modifier)
            is PreviewFavorite.Drink -> FavoriteDrinkCard(item.value, item.id in state.removingDrinks, modifier) {
                onAction(FavoritesAction.RemoveDrink(item.id))
            }
        }
    }
}

@Composable
private fun PreviewShopCard(
    shop: FavoriteShop,
    state: FavoritesUiState,
    modifier: Modifier,
    onAction: (FavoritesAction) -> Unit,
    distanceForCoordinates: (Double, Double) -> String?,
) {
    val distance = shop.latitude?.let { latitude -> shop.longitude?.let { distanceForCoordinates(latitude, it) } }
    FavoriteCard(shop, shop.id in state.removing, distance,
        { onAction(FavoritesAction.OpenShop(shop.id)) }, { onAction(FavoritesAction.Remove(shop.id)) }, modifier)
}

internal fun FavoritesUiState.shouldShowPreview() =
    shops.isNotEmpty() || roasters.isNotEmpty() || drinks.isNotEmpty() ||
        isLoading || areRoastersLoading || areDrinksLoading || loadFailed ||
        roastersLoadFailed || drinksLoadFailed || actionFailed

private sealed interface PreviewFavorite {
    val id: String
    data class Shop(val value: FavoriteShop) : PreviewFavorite { override val id get() = value.id }
    data class Roaster(val value: FavoriteRoaster) : PreviewFavorite { override val id get() = value.id }
    data class Drink(val value: FavoriteDrink) : PreviewFavorite { override val id get() = value.id }
}

@Composable
private fun FavoritesPreviewSample(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    FavoritesPreviewContent(FavoritesUiState(isLoading = false,
        shops = listOf(FavoriteShop("same", "Кофейня", rating = 4.7)),
        roasters = listOf(FavoriteRoaster("same", "Обжарщик", "sample"), FavoriteRoaster("next", "Ещё обжарщик", "next")),
        drinks = listOf(FavoriteDrink("same", "Капучино", null, true))), {}, { _, _ -> null }) { Text("Избранное") }
}

@Preview @Composable private fun FavoritesPreviewLightPreview() = FavoritesPreviewSample(false)
@Preview @Composable private fun FavoritesPreviewDarkPreview() = FavoritesPreviewSample(true)
