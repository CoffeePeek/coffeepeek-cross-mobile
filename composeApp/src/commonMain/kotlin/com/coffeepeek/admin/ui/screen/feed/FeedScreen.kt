package com.coffeepeek.admin.ui.screen.feed

import com.coffeepeek.admin.ui.icons.CpIcons
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TextButton
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coffeepeek.admin.location.NEARBY_RADIUS_METERS
import com.coffeepeek.admin.location.distanceToShopMeters
import com.coffeepeek.admin.location.formatDistance
import com.coffeepeek.admin.location.rememberPermittedUserLocation
import com.coffeepeek.admin.theme.CpColor
import com.coffeepeek.admin.theme.CoffeePeekTheme
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.CoffeeShopImage
import com.coffeepeek.admin.ui.component.brewMethodIcon
import com.coffeepeek.admin.ui.component.CoffeeShopPlaceholderImage
import com.coffeepeek.admin.ui.component.CoffeePeekLoader
import com.coffeepeek.admin.ui.component.CoffeePeekPullToRefresh
import com.coffeepeek.admin.ui.component.CpSearchField
import com.coffeepeek.admin.ui.component.LocalFloatingNavClearance
import com.coffeepeek.admin.ui.component.PriceBynRow
import com.coffeepeek.admin.ui.component.priceRangeLevel
import com.coffeepeek.admin.ui.model.COFFEE_FOCUS_OPTIONS
import com.coffeepeek.admin.utils.formatOneDecimal
import androidx.compose.foundation.lazy.LazyColumn
import com.coffeepeek.domain.model.CoffeeShop
import org.jetbrains.compose.ui.tooling.preview.Preview
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.maskot_with_magnifying_glass
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import com.coffeepeek.admin.di.platformViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(vm: FeedViewModel = platformViewModel()) {
    val state by vm.uiState.collectAsState()
    val listState = rememberLazyListState()
    val userLocation = rememberPermittedUserLocation()
    val displayedShops = if (state.filters.nearbyOnly && userLocation != null) {
        state.visibleShops
            .mapNotNull { shop ->
                val meters = distanceToShopMeters(userLocation, shop.location) ?: return@mapNotNull null
                if (meters > NEARBY_RADIUS_METERS) null else shop to meters
            }
            .sortedBy { it.second }
            .map { it.first }
    } else {
        state.visibleShops
    }
    val fillingNearby = state.filters.nearbyOnly &&
        userLocation != null &&
        displayedShops.size < 8 &&
        state.hasMore &&
        state.shops.isNotEmpty() &&
        !state.isLoading &&
        !state.isRefreshing

    val shouldLoadMore by remember {
        derivedStateOf {
            if (state.shops.isEmpty()) return@derivedStateOf false
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= state.shops.size - 5 &&
                state.hasMore &&
                !state.isLoadingMore &&
                !state.isLoading &&
                !state.isRefreshing
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) vm.loadMore()
    }
    LaunchedEffect(fillingNearby, state.currentPage, state.isLoadingMore) {
        if (fillingNearby && !state.isLoadingMore) vm.loadMore()
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Box(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = CpDimens.spacing4)
                            .padding(top = CpDimens.spacing3, bottom = CpDimens.spacing2),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing2),
                        ) {
                            CpSearchField(
                                value = state.query,
                                onValueChange = vm::onQueryChange,
                                placeholder = "Поиск кофейни…",
                                modifier = Modifier.weight(1f),
                                fieldHeight = CpDimens.buttonHeight,
                            )
                            BadgedBox(
                                badge = {
                                    if (state.activeFilterCount > 0) {
                                        Badge(modifier = Modifier.size(20.dp)) {
                                            Text(state.activeFilterCount.toString())
                                        }
                                    }
                                },
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(CpDimens.buttonHeight)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surface)
                                        .border(
                                            width = 1.dp,
                                            color = MaterialTheme.colorScheme.outline,
                                            shape = CircleShape,
                                        )
                                        .clickable(onClick = vm::toggleFilters),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        CpIcons.Filter,
                                        contentDescription = "Фильтры",
                                        tint = if (state.showFilters || state.activeFilterCount > 0) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier.size(22.dp),
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(CpDimens.spacing2))
                        FeedQuickFilterBar(
                            nearbyOnly = state.filters.nearbyOnly,
                            showNearby = userLocation != null,
                            openOnly = state.filters.openOnly,
                            newOnly = state.filters.newOnly,
                            visitedOnly = state.filters.visitedOnly,
                            favoritesOnly = state.filters.favoritesOnly,
                            onToggleOpen = vm::toggleOpenOnly,
                            onToggleNew = vm::toggleNewOnly,
                            onToggleVisited = vm::toggleVisitedOnly,
                            onToggleFavorites = vm::toggleFavoritesOnly,
                            onToggleNearby = vm::toggleNearbyOnly,
                            coffeeFocusId = state.filters.coffeeFocus,
                            onCoffeeFocusChange = { id ->
                                vm.setCoffeeFocus(
                                    if (state.filters.coffeeFocus == id) null else id,
                                )
                            },
                        )
                    }
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (state.showFilters) {
            ShopFiltersScreen(
                state = state,
                onDismiss = vm::closeFilters,
                onApply = vm::applyFilters,
            )
        }
        val contentModifier = Modifier.fillMaxSize().padding(padding)
        val navClearance = LocalFloatingNavClearance.current
        val listContentPadding = PaddingValues(
            start = CpDimens.spacing4,
            top = CpDimens.spacing4,
            end = CpDimens.spacing4,
            bottom = CpDimens.spacing4 + navClearance,
        )

        when {
            state.isLoading && state.shops.isEmpty() -> {
                Box(contentModifier, contentAlignment = Alignment.Center) {
                    CoffeePeekLoader()
                }
            }
            state.error != null && state.shops.isEmpty() -> {
                CoffeePeekPullToRefresh(
                    listState = listState,
                    isRefreshing = state.isRefreshing,
                    onRefresh = vm::refresh,
                    modifier = contentModifier,
                ) { scrollModifier ->
                    LazyColumn(
                        state = listState,
                        modifier = scrollModifier.fillMaxSize(),
                        contentPadding = listContentPadding,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        item {
                            Column(
                                modifier = Modifier.fillParentMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text(
                                    state.error ?: "Ошибка загрузки",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.height(CpDimens.spacing3))
                    Button(
                        onClick = vm::refresh,
                        modifier = Modifier.height(CpDimens.buttonHeight),
                        shape = RoundedCornerShape(percent = 50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                        ),
                    ) { Text("Попробовать снова") }
                            }
                        }
                    }
                }
            }
            fillingNearby && displayedShops.isEmpty() -> {
                Box(contentModifier, contentAlignment = Alignment.Center) {
                    CoffeePeekLoader()
                }
            }
            else -> {
                CoffeePeekPullToRefresh(
                    listState = listState,
                    isRefreshing = state.isRefreshing,
                    onRefresh = vm::refresh,
                    modifier = contentModifier,
                ) { scrollModifier ->
                    LazyColumn(
                        state = listState,
                        modifier = scrollModifier.fillMaxSize(),
                        contentPadding = listContentPadding,
                        verticalArrangement = Arrangement.spacedBy(CpDimens.spacing3),
                    ) {
                        items(displayedShops, key = { it.id }) { shop ->
                            ShopCard(
                                shop = shop,
                                distance = formatDistance(distanceToShopMeters(userLocation, shop.location)),
                                onClick = { Navigator.navigate(Navigator.Screen.ShopDetail(shop.id)) },
                                isFavoriteUpdating = shop.id in state.favoriteUpdates,
                                onToggleFavorite = { vm.toggleFavorite(shop) },
                            )
                        }
                        if (state.isLoadingMore) {
                            item(key = "loading-more") {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(CpDimens.spacing4),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CoffeePeekLoader(
                                        size = CpDimens.loaderButton,
                                        strokeWidth = 2.dp,
                                    )
                                }
                            }
                        }
                        item(key = "add-missing-shop") {
                            AddMissingShopCard(
                                onAddShop = {
                                    Navigator.navigate(Navigator.Screen.AddShop)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddMissingShopCard(onAddShop: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CpDimens.radiusXl))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .padding(horizontal = CpDimens.spacing4, vertical = CpDimens.spacing6),
        horizontalAlignment = Alignment.Start,
    ) {
        Image(
            painter = painterResource(Res.drawable.maskot_with_magnifying_glass),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(132.dp)
                .align(Alignment.CenterHorizontally),
        )
        Spacer(Modifier.height(CpDimens.spacing3))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 20.dp),
        ) {
            Text(
                text = "Не нашли нужную кофейню?",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(CpDimens.spacing2))
            Text(
                text = "Предложите добавить её в CoffeePeek — после проверки она появится в каталоге.",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(CpDimens.spacing2))
        TextButton(
            onClick = onAddShop,
            contentPadding = PaddingValues(horizontal = 0.dp, vertical = CpDimens.spacing2),
        ) {
            Icon(
                imageVector = CpIcons.Add,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(CpDimens.spacing2))
            Text("Добавить кофейню")
        }
    }
}

@Composable
internal fun ShopCard(
    shop: CoffeeShop,
    distance: String? = null,
    onClick: () -> Unit,
    isFavoriteUpdating: Boolean = false,
    onToggleFavorite: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(CpDimens.radiusXl),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f)
                    .clip(RoundedCornerShape(topStart = CpDimens.radiusXl, topEnd = CpDimens.radiusXl))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                val photoUrl = shop.photoUrl
                if (!photoUrl.isNullOrBlank()) {
                    CoffeeShopImage(
                        imageUrl = photoUrl,
                        contentDescription = shop.title,
                        contentScale = ContentScale.Crop,
                        placeholderLabelSize = 18.sp,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    CoffeeShopPlaceholderImage(
                        labelSize = 18.sp,
                        contentDescription = "Фото ${shop.title} отсутствует",
                    )
                }
                if (shop.isNew) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(CpDimens.spacing3)
                            .clip(RoundedCornerShape(CpDimens.radiusLg))
                            .background(CpColor.DarkSurface.copy(alpha = 0.92f))
                            .padding(horizontal = CpDimens.spacing3, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing1),
                    ) {
                        Icon(
                            imageVector = CpIcons.Sparkle,
                            contentDescription = null,
                            tint = CpColor.Primary,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = "Новое",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(CpDimens.spacing3),
                    horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing2),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val rating = shop.rating
                    if (rating != null && rating > 0) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(CpDimens.radiusLg))
                                .background(Color.Black.copy(alpha = 0.68f))
                                .padding(horizontal = CpDimens.spacing2, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing1),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = CpIcons.StarFilled,
                                contentDescription = null,
                                tint = CpColor.Primary,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = formatOneDecimal(rating),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                            )
                            if (shop.reviewCount > 0) {
                                Text(
                                    text = "(${shop.reviewCount})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.82f),
                                )
                            }
                        }
                    }
                    FavoriteIconBadge(
                        isFavorite = shop.isFavorite,
                        enabled = !isFavoriteUpdating,
                        onClick = onToggleFavorite,
                    )
                }

                val visibleRoasterLogos = shop.roasterPhotoUrls
                    .filter(String::isNotBlank)
                    .distinct()
                    .take(3)
                if (visibleRoasterLogos.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(CpDimens.spacing3)
                            .width(36.dp + 22.dp * (visibleRoasterLogos.size - 1))
                            .height(36.dp),
                    ) {
                        visibleRoasterLogos.forEachIndexed { index, logoUrl ->
                            Box(
                                modifier = Modifier
                                    .offset(x = 22.dp * index)
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                        shape = CircleShape,
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                CoffeeShopImage(
                                    imageUrl = logoUrl,
                                    contentDescription = "Логотип обжарщика ${index + 1}",
                                    contentScale = ContentScale.Crop,
                                    placeholderLabelSize = 5.sp,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.padding(
                    start = CpDimens.spacing4,
                    top = CpDimens.spacing3,
                    end = CpDimens.spacing4,
                    bottom = CpDimens.spacing4,
                ),
                verticalArrangement = Arrangement.spacedBy(CpDimens.spacing2),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = shop.title,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    OpenStatusBadge(
                        isOpen = shop.isOpen,
                        modifier = Modifier.padding(start = CpDimens.spacing2),
                    )
                }

                if (shop.brewMethods.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing1),
                    ) {
                        shop.brewMethods.take(2).forEach { method ->
                            ShopInfoChip(text = method, brewIcon = brewMethodIcon(method))
                        }
                        if (shop.brewMethods.size > 2) {
                            ShopInfoChip(text = "+${shop.brewMethods.size - 2}")
                        }
                    }
                }

                val locationSummary = listOfNotNull(
                    shop.address?.takeIf { it.isNotBlank() },
                    distance?.let { "$it от вас" },
                ).joinToString(" • ")
                if (locationSummary.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            CpIcons.Location,
                            contentDescription = null,
                            tint = CpColor.Primary,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = locationSummary,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = shopTypeLabel(shop.type),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        priceRangeLevel(shop.priceRange)?.let { level ->
                            DetailSeparator()
                            PriceBynRow(
                                level = level,
                                iconSize = 8.dp,
                                activeTint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        shop.tags
                            .filterNot { it in shop.brewMethods }
                            .firstOrNull()
                            ?.let { tag ->
                                DetailSeparator()
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false),
                                )
                            }
                    }
                    Icon(
                        imageVector = CpIcons.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun FavoriteIconBadge(
    isFavorite: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(CpDimens.radiusLg))
            .background(Color.Black.copy(alpha = 0.68f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (isFavorite) CpIcons.FavoriteFilled else CpIcons.Favorite,
            contentDescription = if (isFavorite) "Убрать из избранного" else "Добавить в избранное",
            tint = if (isFavorite) CpColor.Error else Color.White,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun OpenStatusBadge(isOpen: Boolean, modifier: Modifier = Modifier) {
    val color = if (isOpen) CpColor.Success else MaterialTheme.colorScheme.error
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(CpDimens.radiusSm))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = CpDimens.spacing2, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).clip(RoundedCornerShape(50)).background(color))
        Text(
            text = if (isOpen) "ОТКРЫТО" else "ЗАКРЫТО",
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun ShopInfoChip(
    text: String,
    brewIcon: DrawableResource? = null,
) {
    Row(
        modifier = Modifier
            .widthIn(max = 112.dp)
            .clip(RoundedCornerShape(CpDimens.radiusLg))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(CpDimens.radiusLg),
            )
            .padding(horizontal = CpDimens.spacing2, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing1),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        brewIcon?.let {
            Icon(
                painter = painterResource(it),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(13.dp),
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun DetailSeparator() {
    Text(
        text = "·",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun shopTypeLabel(type: String): String = when (type) {
    com.coffeepeek.domain.model.CoffeeShopType.SPECIALTY -> "Specialty"
    com.coffeepeek.domain.model.CoffeeShopType.CAFE -> "Кафе"
    else -> "Кофейня"
}

@Composable
private fun FeedQuickFilterBar(
    nearbyOnly: Boolean,
    showNearby: Boolean,
    openOnly: Boolean,
    newOnly: Boolean,
    visitedOnly: Boolean,
    favoritesOnly: Boolean,
    onToggleOpen: () -> Unit,
    onToggleNew: () -> Unit,
    onToggleVisited: () -> Unit,
    onToggleFavorites: () -> Unit,
    onToggleNearby: () -> Unit,
    coffeeFocusId: String?,
    onCoffeeFocusChange: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing1),
    ) {
        if (showNearby) {
            DesignFilterChip(
                label = "Рядом",
                selected = nearbyOnly,
                onClick = onToggleNearby,
                leadingIcon = CpIcons.Distance,
            )
        }
        DesignFilterChip(
            label = "Открыто",
            selected = openOnly,
            onClick = onToggleOpen,
            leadingIcon = CpIcons.Time,
        )
        DesignFilterChip(
            label = "Новые",
            selected = newOnly,
            onClick = onToggleNew,
            leadingIcon = CpIcons.Sparkle,
        )
        DesignFilterChip(
            label = "Уже был",
            selected = visitedOnly,
            onClick = onToggleVisited,
            leadingIcon = CpIcons.CheckCircle,
        )
        DesignFilterChip(
            label = "Избранное",
            selected = favoritesOnly,
            onClick = onToggleFavorites,
            leadingIcon = CpIcons.Favorite,
        )

        COFFEE_FOCUS_OPTIONS.forEach { option ->
            DesignFilterChip(
                label = option.label,
                selected = coffeeFocusId == option.id,
                onClick = { onCoffeeFocusChange(option.id) },
            )
        }
    }
}

@Composable
private fun DesignFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
) {
    val shape = RoundedCornerShape(999.dp)
    val bg = if (selected) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.surface
    }
    val fg = if (selected) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val borderColor = if (selected) {
        Color.Transparent
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
    }

    Row(
        modifier = Modifier
            .height(CpDimens.buttonHeight)
            .clip(shape)
            .background(bg)
            .border(width = 1.dp, color = borderColor, shape = shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = CpColor.Primary,
                modifier = Modifier.size(16.dp),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = fg,
            maxLines = 1,
        )
        if (trailingIcon != null) {
            Icon(
                imageVector = trailingIcon,
                contentDescription = null,
                tint = fg.copy(alpha = 0.7f),
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

private val previewCoffeeShop = CoffeeShop(
    id = "feed-preview",
    title = "Кофейня с длинным названием",
    rating = 4.8,
    reviewCount = 42,
    cityName = "Минск",
    priceRange = "\$\$",
    photoUrl = null,
    isFavorite = true,
)

@Preview
@Composable
private fun ShopCardLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopCard(previewCoffeeShop, "1 км", {}, isFavoriteUpdating = true, onToggleFavorite = {})
}

@Preview
@Composable
private fun ShopCardDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopCard(previewCoffeeShop.copy(isFavorite = false), "1 км", {}, onToggleFavorite = {})
}
