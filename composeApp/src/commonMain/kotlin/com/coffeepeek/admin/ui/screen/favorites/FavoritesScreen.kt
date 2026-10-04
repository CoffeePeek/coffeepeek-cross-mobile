package com.coffeepeek.admin.ui.screen.favorites

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.favorites_empty
import coffeepeek.composeapp.generated.resources.favorites_roasters
import com.coffeepeek.admin.di.platformViewModel
import com.coffeepeek.admin.feature.favorites.api.roasterFavoriteId
import com.coffeepeek.admin.location.distanceToShopMeters
import com.coffeepeek.admin.location.formatDistance
import com.coffeepeek.admin.location.rememberPermittedUserLocation
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.CoffeePeekLoader
import com.coffeepeek.admin.ui.component.CoffeeShopImage
import com.coffeepeek.admin.ui.component.CoffeeShopPlaceholderImage
import com.coffeepeek.admin.ui.component.CpTopBar
import com.coffeepeek.admin.ui.component.FavoriteButton
import com.coffeepeek.admin.ui.icons.CpIcons
import com.coffeepeek.admin.utils.formatOneDecimal
import com.coffeepeek.domain.model.CatalogItem
import com.coffeepeek.domain.model.CoffeeShopDetails
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
            ) {
                state.error?.let { error ->
                    item(key = "error") {
                        Text(error, color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = { vm.load(force = true) }) { Text("Повторить") }
                    }
                }
                items(state.shops, key = { "shop:${it.shop.id}" }) { details ->
                    FavoriteShopCard(
                        details = details,
                        distance = formatDistance(distanceToShopMeters(userLocation, details.location)),
                        onClick = {
                            details.shop.publicAddress?.slug?.let { Navigator.navigate(Navigator.Screen.ShopDetail(it)) }
                        },
                        onRemoveFavorite = { vm.removeFavorite(details.shop) },
                    )
                    Spacer(Modifier.height(CpDimens.spacing6))
                }
                if (state.roasters.isNotEmpty()) {
                    item(key = "roasters_heading") {
                        Text(
                            text = stringResource(Res.string.favorites_roasters),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = CpDimens.spacing3),
                        )
                    }
                    items(state.roasters, key = { "roaster:${it.roasterFavoriteId}" }) { roaster ->
                        FavoriteRoasterRow(
                            roaster = roaster,
                            isRemoving = roaster.roasterFavoriteId in state.savingRoasterIds,
                            onRemove = { vm.removeRoaster(roaster) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoriteShopCard(
    details: CoffeeShopDetails,
    distance: String?,
    onClick: () -> Unit,
    onRemoveFavorite: () -> Unit,
) {
    val shop = details.shop
    Column(
        modifier = Modifier.fillMaxWidth().clickable(enabled = shop.publicAddress != null, onClick = onClick),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f)
                .clip(RoundedCornerShape(CpDimens.radius2xl))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            val photoUrl = shop.photoUrl?.takeIf(String::isNotBlank)
            if (photoUrl != null) {
                CoffeeShopImage(
                    imageUrl = photoUrl,
                    contentDescription = shop.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                CoffeeShopPlaceholderImage(contentDescription = "Фото ${shop.title} отсутствует")
            }
            FavoriteButton(
                isFavorite = true,
                onClick = onRemoveFavorite,
                overImage = true,
                modifier = Modifier.align(Alignment.TopEnd).padding(CpDimens.spacing3),
            )
        }
        Text(
            text = shop.title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = CpDimens.spacing3, bottom = CpDimens.spacing1),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing1),
        ) {
            val rating = shop.rating?.takeIf { it > 0 }
            if (rating != null) {
                Icon(CpIcons.StarFilled, null, modifier = Modifier.size(16.dp))
                Text(formatOneDecimal(rating), style = MaterialTheme.typography.bodyLarge)
            }
            if (distance != null) {
                if (rating != null) Text("·", style = MaterialTheme.typography.bodyLarge)
                Text(distance, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun FavoriteRoasterRow(roaster: CatalogItem, isRemoving: Boolean, onRemove: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(CpDimens.radiusLg))
            .clickable(enabled = roaster.address != null) {
                roaster.address?.slug?.let { Navigator.navigate(Navigator.Screen.RoasterDetail(it)) }
            }
            .padding(vertical = CpDimens.spacing2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing3),
    ) {
        Box(
            modifier = Modifier.size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            val photoUrl = roaster.photoUrl?.takeIf(String::isNotBlank)
            if (photoUrl != null) {
                CoffeeShopImage(
                    imageUrl = photoUrl,
                    contentDescription = roaster.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(CpIcons.Factory, null, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(
            text = roaster.name,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        FavoriteButton(isFavorite = true, onClick = onRemove, enabled = !isRemoving)
    }
}
