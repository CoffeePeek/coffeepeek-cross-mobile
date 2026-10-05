package com.coffeepeek.admin.ui.screen.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.favorites_empty
import coffeepeek.composeapp.generated.resources.favorites_roasters
import com.coffeepeek.admin.feature.catalog.api.RoasterCard
import com.coffeepeek.admin.feature.catalog.api.ShopCard
import com.coffeepeek.admin.di.platformViewModel
import com.coffeepeek.admin.feature.favorites.api.roasterFavoriteId
import com.coffeepeek.admin.location.distanceToShopMeters
import com.coffeepeek.admin.location.formatDistance
import com.coffeepeek.admin.location.rememberPermittedUserLocation
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.CoffeePeekLoader
import com.coffeepeek.admin.ui.component.CpTopBar
import org.jetbrains.compose.resources.stringResource

@Composable
fun FavoritesScreen(vm: FavoritesViewModel = platformViewModel()) {
    val state by vm.state.collectAsState()
    val userLocation = rememberPermittedUserLocation()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.actionMessage) {
        state.actionMessage?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearActionMessage()
        }
    }

    Scaffold(
        topBar = { CpTopBar("Избранное") },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        when {
            state.areRoastersLoading || (state.isLoading && state.shops.isEmpty() && state.roasters.isEmpty()) -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CoffeePeekLoader()
                }
            }
            state.shops.isEmpty() && state.roasters.isEmpty() -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(
                        modifier = Modifier.padding(CpDimens.spacing4),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            state.error ?: stringResource(Res.string.favorites_empty),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (state.error != null) {
                            Spacer(Modifier.height(CpDimens.spacing3))
                            Button(onClick = { vm.load(force = true) }) { Text("Повторить") }
                        }
                    }
                }
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(CpDimens.spacing4),
                verticalArrangement = Arrangement.spacedBy(CpDimens.spacing3),
            ) {
                state.error?.let { error ->
                    item(key = "error") {
                        Text(error, color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = { vm.load(force = true) }) { Text("Повторить") }
                    }
                }
                items(state.shops, key = { "shop:${it.shop.id}" }) { details ->
                    ShopCard(
                        shop = details.shop.copy(isFavorite = true),
                        showCatalogDetails = false,
                        distance = formatDistance(distanceToShopMeters(userLocation, details.location)),
                        onClick = {
                            details.shop.publicAddress?.slug?.let { Navigator.navigate(Navigator.Screen.ShopDetail(it)) }
                        },
                        onToggleFavorite = { vm.removeFavorite(details.shop) },
                    )
                }
                if (state.roasters.isNotEmpty()) {
                    item(key = "roasters_heading") {
                        Text(
                            text = stringResource(Res.string.favorites_roasters),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = CpDimens.spacing3),
                        )
                    }
                    items(state.roasters, key = { "roaster:${it.roasterFavoriteId}" }) { roaster ->
                        RoasterCard(
                            roaster = roaster,
                            details = state.roasterDetails[roaster.roasterFavoriteId],
                            isFavorite = true,
                            isFavoriteLoading = roaster.roasterFavoriteId in state.savingRoasterIds,
                            onClick = { roaster.address?.slug?.let { Navigator.navigate(Navigator.Screen.RoasterDetail(it)) } },
                            onToggleFavorite = { vm.removeRoaster(roaster) },
                        )
                    }
                }
            }
        }
    }
}
