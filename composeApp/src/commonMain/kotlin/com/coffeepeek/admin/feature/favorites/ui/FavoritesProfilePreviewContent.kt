package com.coffeepeek.admin.feature.favorites.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.coffeepeek.admin.feature.catalog.api.RoasterCard
import com.coffeepeek.admin.feature.catalog.api.ShopCard
import com.coffeepeek.admin.feature.favorites.api.FavoritesPreviewCarousel
import com.coffeepeek.admin.feature.favorites.api.roasterFavoriteId
import com.coffeepeek.admin.location.distanceToShopMeters
import com.coffeepeek.admin.location.formatDistance
import com.coffeepeek.admin.location.rememberPermittedUserLocation
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.component.CoffeePeekLoader
import com.coffeepeek.admin.ui.screen.favorites.FavoritesViewModel
import com.coffeepeek.domain.feature.favorites.api.FavoriteEntry
import com.coffeepeek.domain.model.CatalogItem
import com.coffeepeek.domain.model.CoffeeShopDetails
import org.koin.compose.koinInject

@Composable
internal fun FavoritesProfilePreviewContent(
    onCoffeeShopClick: (String) -> Unit,
    onRoasterClick: (String) -> Unit,
    vm: FavoritesViewModel = koinInject(),
    header: @Composable () -> Unit = {},
) {
    val state by vm.state.collectAsState()
    val error = state.error ?: state.roasterError
    val isEmpty = state.shops.isEmpty() && state.roasters.isEmpty() && state.drinks.isEmpty()
    if (isEmpty && !state.isLoading && !state.areRoastersLoading && error == null) return
    val userLocation = rememberPermittedUserLocation()

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(CpDimens.spacing3)) {
        header()
        state.actionMessage?.let { message ->
            Text(message, color = MaterialTheme.colorScheme.error)
            TextButton(onClick = vm::clearActionMessage) { Text("Закрыть") }
        }
        when {
            isEmpty && (state.isLoading || state.areRoastersLoading) -> {
                Box(Modifier.fillMaxWidth().padding(CpDimens.spacing4), contentAlignment = Alignment.Center) {
                    CoffeePeekLoader()
                }
            }
            isEmpty && error != null -> {
                Text(
                    error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = { vm.load(force = true) }) { Text("Повторить") }
            }
            else -> {
                if (error != null) {
                    Text(error, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = { vm.load(force = true) }) { Text("Повторить") }
                }
                FavoritesPreviewCarousel<ProfilePreviewFavorite>(
                    coffeeShops = state.shops.map { ProfilePreviewFavorite.Shop(it) },
                    roasters = state.roasters.map { ProfilePreviewFavorite.Roaster(it) },
                    drinks = state.drinks.map { ProfilePreviewFavorite.Drink(it) },
                    itemKey = { favorite ->
                        when (favorite) {
                            is ProfilePreviewFavorite.Shop -> favorite.details.shop.id
                            is ProfilePreviewFavorite.Roaster -> favorite.roaster.roasterFavoriteId
                            is ProfilePreviewFavorite.Drink -> favorite.drink.id
                        }
                    },
                ) { favorite, modifier ->
                    when (favorite) {
                        is ProfilePreviewFavorite.Shop -> {
                            val details = favorite.details
                            ShopCard(
                                shop = details.shop.copy(isFavorite = true),
                                showCatalogDetails = false,
                                distance = formatDistance(distanceToShopMeters(userLocation, details.location)),
                                onClick = { details.shop.publicAddress?.slug?.let(onCoffeeShopClick) },
                                onToggleFavorite = { vm.removeFavorite(details.shop) },
                                modifier = modifier,
                            )
                        }
                        is ProfilePreviewFavorite.Roaster -> {
                            val roaster = favorite.roaster
                            RoasterCard(
                                roaster = roaster,
                                details = state.roasterDetails[roaster.roasterFavoriteId],
                                isFavorite = true,
                                isFavoriteLoading = roaster.roasterFavoriteId in state.savingRoasterIds,
                                onClick = { roaster.address?.slug?.let(onRoasterClick) },
                                onToggleFavorite = { vm.removeRoaster(roaster) },
                                modifier = modifier,
                            )
                        }
                        is ProfilePreviewFavorite.Drink -> {
                            val drink = favorite.drink
                            Box(modifier) {
                                SavedDrinkCard(
                                    drink = drink,
                                    removing = drink.id in state.removingDrinkIds,
                                    onRemove = { vm.removeDrink(drink) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private sealed interface ProfilePreviewFavorite {
    data class Shop(val details: CoffeeShopDetails) : ProfilePreviewFavorite
    data class Roaster(val roaster: CatalogItem) : ProfilePreviewFavorite
    data class Drink(val drink: FavoriteEntry) : ProfilePreviewFavorite
}
