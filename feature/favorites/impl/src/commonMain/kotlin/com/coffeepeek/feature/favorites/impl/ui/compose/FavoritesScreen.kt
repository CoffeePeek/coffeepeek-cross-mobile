package com.coffeepeek.feature.favorites.impl.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coffeepeek.core.designsystem.component.CoffeePeekButton
import com.coffeepeek.core.designsystem.component.CoffeePeekLoader
import com.coffeepeek.core.designsystem.component.CoffeePeekTopBar
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.favorites.domain.model.FavoriteShop
import com.coffeepeek.feature.favorites.domain.model.FavoriteRoaster
import com.coffeepeek.feature.favorites.impl.ui.FavoritesViewModel
import com.coffeepeek.feature.favorites.impl.ui.compose.component.FavoriteDrinkCard
import com.coffeepeek.feature.favorites.impl.resources.favorites_drinks
import com.coffeepeek.feature.favorites.domain.model.FavoriteDrink
import com.coffeepeek.feature.favorites.impl.ui.compose.component.FavoriteCard
import com.coffeepeek.feature.favorites.impl.ui.compose.component.FavoriteRoasterCard
import com.coffeepeek.feature.favorites.impl.ui.compose.component.FavoritesMessage
import com.coffeepeek.feature.favorites.impl.ui.compose.model.FavoritesAction
import com.coffeepeek.feature.favorites.impl.ui.compose.model.FavoritesEvent
import com.coffeepeek.feature.favorites.impl.ui.compose.model.FavoritesUiState
import com.coffeepeek.feature.favorites.impl.resources.Res
import com.coffeepeek.feature.favorites.impl.resources.favorites_back
import com.coffeepeek.feature.favorites.impl.resources.favorites_empty
import com.coffeepeek.feature.favorites.impl.resources.favorites_load_error
import com.coffeepeek.feature.favorites.impl.resources.favorites_loading
import com.coffeepeek.feature.favorites.impl.resources.favorites_retry
import com.coffeepeek.feature.favorites.impl.resources.favorites_title
import com.coffeepeek.feature.favorites.impl.resources.favorites_update_error
import com.coffeepeek.feature.favorites.impl.resources.favorites_action_error
import com.coffeepeek.feature.favorites.impl.resources.favorites_roasters
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

/** Runtime adapter. The entry supplies the lifecycle-owned ViewModel and navigation callbacks. */
@Composable
internal fun FavoritesScreen(
    viewModel: FavoritesViewModel,
    onOpenShop: (String) -> Unit,
    onBack: () -> Unit,
    distanceForCoordinates: (Double, Double) -> String? = { _, _ -> null },
    onOpenRoaster: (String) -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentOpenShop by rememberUpdatedState(onOpenShop)
    val currentOpenRoaster by rememberUpdatedState(onOpenRoaster)
    val currentBack by rememberUpdatedState(onBack)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is FavoritesEvent.OpenShop -> currentOpenShop(event.shopId)
                is FavoritesEvent.OpenRoaster -> currentOpenRoaster(event.publicSlug)
                FavoritesEvent.Back -> currentBack()
            }
        }
    }
    FavoritesScreenContent(
        state = state,
        onAction = viewModel::onAction,
        distanceForCoordinates = distanceForCoordinates,
    )
}

/** Stateless content: no ViewModel, DI, network or persistence access. */
@Composable
internal fun FavoritesScreenContent(
    state: FavoritesUiState,
    onAction: (FavoritesAction) -> Unit,
    distanceForCoordinates: (Double, Double) -> String? = { _, _ -> null },
) {
    val snackbar = remember { SnackbarHostState() }
    val actionError = stringResource(Res.string.favorites_action_error)
    LaunchedEffect(state.actionFailed) { if (state.actionFailed) snackbar.showSnackbar(actionError) }
    Scaffold(snackbarHost = { SnackbarHost(snackbar) }, topBar = { CoffeePeekTopBar(stringResource(Res.string.favorites_title),
        stringResource(Res.string.favorites_back), onBack = { onAction(FavoritesAction.Back) }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
            when {
                (state.isLoading || state.areRoastersLoading || state.areDrinksLoading) && state.shops.isEmpty() && state.roasters.isEmpty() && state.drinks.isEmpty() -> Box(
                    Modifier.fillMaxSize(), contentAlignment = Alignment.Center,
                ) { CoffeePeekLoader(stringResource(Res.string.favorites_loading)) }
                (state.loadFailed || state.roastersLoadFailed || state.drinksLoadFailed) && state.shops.isEmpty() && state.roasters.isEmpty() && state.drinks.isEmpty() ->
                    FavoritesMessage(stringResource(Res.string.favorites_load_error)) {
                        onAction(FavoritesAction.Retry)
                    }
                state.shops.isEmpty() && state.roasters.isEmpty() && state.drinks.isEmpty() -> FavoritesMessage(stringResource(Res.string.favorites_empty))
                else -> {
                    if (state.loadFailed || state.roastersLoadFailed || state.drinksLoadFailed) {
                        Text(stringResource(Res.string.favorites_update_error), Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.error)
                        CoffeePeekButton(stringResource(Res.string.favorites_retry),
                            { onAction(FavoritesAction.Retry) },
                            Modifier.padding(horizontal = 16.dp))
                    }
                    LazyColumn(
                        Modifier.fillMaxSize().windowInsetsPadding(
                            WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
                        ),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.shops, key = { "shop:${it.id}" }) { shop ->
                            val distance = shop.latitude?.let { latitude ->
                                shop.longitude?.let { longitude ->
                                    distanceForCoordinates(latitude, longitude)
                                }
                            }
                            FavoriteCard(
                                shop = shop,
                                removing = shop.id in state.removing,
                                distance = distance,
                                onOpen = { onAction(FavoritesAction.OpenShop(shop.id)) },
                                onRemove = { onAction(FavoritesAction.Remove(shop.id)) },
                            )
                        }
                        if (state.roasters.isNotEmpty()) item(key = "roasters_heading") {
                            Text(stringResource(Res.string.favorites_roasters),
                                style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 12.dp))
                        }
                        items(state.roasters, key = { "roaster:${it.id}" }) { roaster ->
                            FavoriteRoasterCard(roaster, roaster.id in state.removingRoasters,
                                onOpen = { onAction(FavoritesAction.OpenRoaster(roaster.id)) },
                                onRemove = { onAction(FavoritesAction.RemoveRoaster(roaster.id)) })
                        }
                        if (state.drinks.isNotEmpty()) item(key = "drinks_heading") {
                            Text(stringResource(Res.string.favorites_drinks), style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 12.dp))
                        }
                        items(state.drinks, key = { "drink:${it.id}" }) { drink ->
                            FavoriteDrinkCard(drink, drink.id in state.removingDrinks) { onAction(FavoritesAction.RemoveDrink(drink.id)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewFavorites(state: FavoritesUiState, dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    FavoritesScreenContent(state, {}, { _, _ -> "1 км" })
}

private val previewShop = FavoriteShop(
    id = "sample", title = "Любимая кофейня", rating = 4.8, reviewCount = 12,
    cityName = "Минск", address = "Улица Кофейная, 1", latitude = 53.9, longitude = 27.56,
    isOpen = true, priceRange = "\$\$", tags = listOf("Спешелти"),
    brewMethods = listOf("Эспрессо", "Фильтр"),
)

@Preview @Composable private fun FavoritesContentLightPreview() =
    PreviewFavorites(FavoritesUiState(shops = listOf(previewShop), isLoading = false), false)
@Preview @Composable private fun FavoritesContentDarkPreview() =
    PreviewFavorites(FavoritesUiState(shops = listOf(previewShop), isLoading = false), true)
@Preview @Composable private fun FavoritesClosedLightPreview() =
    PreviewFavorites(FavoritesUiState(shops = listOf(previewShop.copy(
        title = "Кофейня с очень длинным названием без фото", isOpen = false, photoUrl = null,
    )), isLoading = false), false)
@Preview @Composable private fun FavoritesClosedDarkPreview() =
    PreviewFavorites(FavoritesUiState(shops = listOf(previewShop.copy(
        title = "Кофейня с очень длинным названием без фото", isOpen = false, photoUrl = null,
    )), isLoading = false), true)
@Preview @Composable private fun FavoritesLoadingLightPreview() = PreviewFavorites(FavoritesUiState(), false)
@Preview @Composable private fun FavoritesLoadingDarkPreview() = PreviewFavorites(FavoritesUiState(), true)
@Preview @Composable private fun FavoritesEmptyLightPreview() =
    PreviewFavorites(FavoritesUiState(isLoading = false), false)
@Preview @Composable private fun FavoritesEmptyDarkPreview() =
    PreviewFavorites(FavoritesUiState(isLoading = false), true)
@Preview @Composable private fun FavoritesErrorLightPreview() =
    PreviewFavorites(FavoritesUiState(isLoading = false, loadFailed = true), false)
@Preview @Composable private fun FavoritesErrorDarkPreview() =
    PreviewFavorites(FavoritesUiState(isLoading = false, loadFailed = true), true)

@Preview @Composable private fun FavoritesRoastersLightPreview() = PreviewFavorites(FavoritesUiState(
    isLoading = false, roasters = listOf(FavoriteRoaster("sample", "Любимый обжарщик", "sample",
        coffeeShopsCount = 7, coffeeProductsCount = 12))), false)
@Preview @Composable private fun FavoritesRoastersDarkPreview() = PreviewFavorites(FavoritesUiState(
    isLoading = false, roasters = listOf(FavoriteRoaster("sample", "Любимый обжарщик", "sample",
        coffeeShopsCount = 7, coffeeProductsCount = 12))), true)

@Preview @Composable private fun FavoritesMixedLightPreview() = PreviewFavorites(FavoritesUiState(
    shops = listOf(previewShop), isLoading = false,
    roasters = listOf(FavoriteRoaster("roaster", "Любимый обжарщик", "roaster"))), false)
@Preview @Composable private fun FavoritesMixedDarkPreview() = PreviewFavorites(FavoritesUiState(
    shops = listOf(previewShop), isLoading = false,
    roasters = listOf(FavoriteRoaster("roaster", "Любимый обжарщик", "roaster"))), true)

@Preview @Composable private fun FavoritesDrinksLightPreview() = PreviewFavorites(FavoritesUiState(
    isLoading = false, drinks = listOf(FavoriteDrink("sample", "Капучино", null, true))), false)
@Preview @Composable private fun FavoritesDrinksDarkPreview() = PreviewFavorites(FavoritesUiState(
    isLoading = false, drinks = listOf(FavoriteDrink("sample", "Капучино", null, true))), true)
