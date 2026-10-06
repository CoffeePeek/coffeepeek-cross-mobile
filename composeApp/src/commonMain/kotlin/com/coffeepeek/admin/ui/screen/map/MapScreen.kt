package com.coffeepeek.admin.ui.screen.map

import com.coffeepeek.admin.ui.icons.CpIcons
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coffeepeek.admin.map.CoffeeMap
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.CoffeeShopImage
import com.coffeepeek.admin.ui.component.CoffeeShopPlaceholderImage
import com.coffeepeek.admin.ui.component.CoffeePeekLoader
import com.coffeepeek.admin.ui.component.CpSearchField
import com.coffeepeek.admin.ui.component.LocalFloatingNavClearance
import com.coffeepeek.admin.ui.component.GlassControlIcon
import com.coffeepeek.admin.ui.component.PlatformMapControlButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.CompositionLocalProvider
import com.coffeepeek.domain.model.CoffeeShop
import com.coffeepeek.domain.model.CoffeeShopDetails
import com.coffeepeek.domain.model.MapShop
import com.coffeepeek.domain.model.MapCoffeeZone
import com.coffeepeek.admin.di.platformViewModel
import com.coffeepeek.admin.utils.formatOneDecimal
import com.coffeepeek.admin.location.rememberPermittedUserLocation
import com.coffeepeek.admin.location.distanceToShopMeters
import com.coffeepeek.admin.location.formatDistance
import com.coffeepeek.domain.model.ShopLocation
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop

@Composable
fun MapScreen(vm: MapViewModel = platformViewModel()) {
    val state by vm.state.collectAsState()
    val userLocation = rememberPermittedUserLocation()
    LaunchedEffect(userLocation) {
        userLocation?.let { vm.onNearbyOriginChanged(it.latitude, it.longitude) }
    }
    val pendingFocus by Navigator.pendingMapFocus.collectAsState()
    val pendingFocusShop = pendingFocus?.let { focus ->
        MapShop(
            id = focus.shopId,
            title = focus.title,
            latitude = focus.latitude,
            longitude = focus.longitude,
        )
    }
    val mapShops = pendingFocusShop?.let { shop ->
        (state.shops + shop).distinctBy { it.id }
    } ?: state.shops
    val selectedShopId = state.selectedShop?.id ?: pendingFocus?.shopId
    val cameraTarget = state.cameraTarget
        ?: pendingFocus?.let { it.latitude to it.longitude }
    val cameraZoom = state.cameraZoom ?: if (pendingFocus != null) 16f else null
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val navClearance = LocalFloatingNavClearance.current
    val hasShopCarousel = state.selectedShop != null || state.nearbyShops.isNotEmpty()

    LaunchedEffect(pendingFocus) {
        pendingFocus?.let { focus ->
            vm.focusOnShop(focus)
            Navigator.consumeMapFocus()
        }
    }

    Box(Modifier.fillMaxSize()) {
        CoffeeMap(
            shops = mapShops,
            clusters = state.clusters,
            zones = if (state.showZones) state.zones else emptyList(),
            selectedShopId = selectedShopId,
            onBoundsChanged = vm::onBoundsChanged,
            onShopClick = vm::onShopSelected,
            onZoneClick = vm::onZoneSelected,
            modifier = Modifier.fillMaxSize(),
            cameraTarget = cameraTarget,
            cameraZoom = cameraZoom,
            onCameraTargetApplied = vm::onCameraTargetApplied,
            isDarkTheme = isDarkTheme,
            myLocationRequestKey = state.myLocationRequest,
            onMyLocationFound = vm::onMyLocationApplied,
            onLocationPermissionDenied = {},
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .fillMaxWidth()
                .padding(horizontal = CpDimens.spacing4, vertical = CpDimens.spacing3)
                .zIndex(2f),
            verticalArrangement = Arrangement.spacedBy(CpDimens.spacing2),
        ) {
            CpSearchField(
                value = state.query,
                onValueChange = vm::onQueryChange,
                placeholder = "Поиск кофейни…",
                fieldHeight = CpDimens.buttonHeight,
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.showSearchSuggestions) {
                MapSearchSuggestions(
                    results = state.searchResults,
                    isLoading = state.isSearchLoading,
                    failed = state.searchFailed,
                    onSelect = vm::onSearchResultSelected,
                )
            }
        }

        if (state.isTruncated) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 60.dp, start = CpDimens.spacing4, end = CpDimens.spacing4),
                shape = RoundedCornerShape(CpDimens.radiusMd),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                shadowElevation = 3.dp,
            ) {
                Text(
                    text = "Слишком много объектов — приблизьте карту",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(
                        horizontal = CpDimens.spacing3,
                        vertical = CpDimens.spacing2,
                    ),
                )
            }
        }

        MapZoomControl(
            onZoomIn = vm::zoomIn,
            onZoomOut = vm::zoomOut,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = CpDimens.spacing4),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = CpDimens.spacing4,
                    bottom = navClearance + when {
                        state.selectedZone != null -> 180.dp
                        hasShopCarousel -> 132.dp
                        else -> CpDimens.spacing4
                    },
                ),
            verticalArrangement = Arrangement.spacedBy(CpDimens.spacing2),
            horizontalAlignment = Alignment.End,
        ) {
            MapControlButton(
                icon = GlassControlIcon.Zones,
                onClick = vm::toggleZones,
                contentDescription = if (state.showZones) "Скрыть зоны" else "Показать зоны",
                selected = state.showZones,
                contentColor = if (state.showZones) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            ) {
                Icon(
                    CpIcons.Polygon,
                    contentDescription = if (state.showZones) "Скрыть зоны" else "Показать зоны",
                    modifier = Modifier.size(26.dp),
                )
            }
            MapControlButton(
                icon = GlassControlIcon.Location,
                onClick = vm::requestMyLocation,
                contentDescription = "Моё местоположение",
                modifier = Modifier.size(CpDimens.buttonHeight + 4.dp),
            ) {
                Icon(
                    CpIcons.Navigation,
                    contentDescription = "Моё местоположение",
                    modifier = Modifier.size(32.dp),
                )
            }
        }

        if (state.isLoading) {
            CoffeePeekLoader(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = CpDimens.spacing4),
                strokeWidth = 2.dp,
            )
        }

        if (state.showSearchArea) {
            Button(
                onClick = vm::searchCurrentArea,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        bottom = navClearance + if (hasShopCarousel) 148.dp else 32.dp,
                    )
                    .height(CpDimens.buttonHeight),
                shape = RoundedCornerShape(percent = 50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onSurface,
                    contentColor = MaterialTheme.colorScheme.surface,
                ),
                contentPadding = PaddingValues(horizontal = CpDimens.spacing5),
            ) {
                Text("Искать в этой области", style = MaterialTheme.typography.labelLarge)
            }
        }

        if (state.selectedZone == null && hasShopCarousel) {
            MapShopCarousel(
                state = state,
                onSelect = vm::onCarouselShopSelected,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = navClearance + CpDimens.spacing4),
            )
        }

        state.selectedZone?.let { zone ->
            MapZoneCard(
                zone = zone,
                onShowShops = vm::showSelectedZoneShops,
                onDismiss = vm::clearZoneSelection,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        start = CpDimens.spacing4,
                        end = CpDimens.spacing4,
                        bottom = navClearance + CpDimens.spacing4,
                    ),
            )
        }
    }
}

@Composable
private fun MapSearchSuggestions(
    results: List<CoffeeShop>,
    isLoading: Boolean,
    failed: Boolean,
    onSelect: (CoffeeShop) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CpDimens.radiusLg),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 6.dp,
    ) {
        when {
            isLoading -> Box(
                modifier = Modifier.fillMaxWidth().height(56.dp),
                contentAlignment = Alignment.Center,
            ) {
                CoffeePeekLoader(size = 24.dp)
            }

            failed -> Text(
                text = "Не удалось выполнить поиск",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(CpDimens.spacing4),
            )

            results.isEmpty() -> Text(
                text = "Кофейни не найдены",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(CpDimens.spacing4),
            )

            else -> Column {
                results.forEachIndexed { index, shop ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .clickable { onSelect(shop) }
                            .padding(horizontal = CpDimens.spacing4, vertical = CpDimens.spacing2),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing3),
                    ) {
                        Icon(
                            imageVector = CpIcons.Location,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = shop.title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            shop.address?.takeIf(String::isNotBlank)?.let { address ->
                                Text(
                                    text = address,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
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
                    if (index != results.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 56.dp),
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MapZoneCard(
    zone: MapCoffeeZone,
    onShowShops: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CpDimens.cardRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Column(
            modifier = Modifier.padding(CpDimens.spacing4),
            verticalArrangement = Arrangement.spacedBy(CpDimens.spacing2),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = zone.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "${zone.shopCount} ${mapShopCountLabel(zone.shopCount)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(CpIcons.Close, contentDescription = "Закрыть")
                }
            }
            if (zone.description.isNotBlank()) {
                Text(
                    text = zone.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Button(
                onClick = onShowShops,
                modifier = Modifier.fillMaxWidth().height(CpDimens.buttonHeight),
                shape = RoundedCornerShape(percent = 50),
            ) {
                Text("Показать кофейни")
            }
        }
    }
}

private fun mapShopCountLabel(count: Int): String {
    val mod100 = count % 100
    val mod10 = count % 10
    return when {
        mod100 in 11..14 -> "кофеен"
        mod10 == 1 -> "кофейня"
        mod10 in 2..4 -> "кофейни"
        else -> "кофеен"
    }
}

@Composable
private fun MapControlButton(
    icon: GlassControlIcon,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    content: @Composable () -> Unit,
) {
    PlatformMapControlButton(
        icon = icon,
        onClick = onClick,
        contentDescription = contentDescription,
        modifier = modifier,
        selected = selected,
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) { content() }
    }
}

@Composable
private fun MapZoomControl(
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(CpDimens.radiusLg)
    Column(
        modifier = modifier
            .width(CpDimens.buttonHeight)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.94f))
            .border(1.dp, MaterialTheme.colorScheme.outline, shape),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(CpDimens.buttonHeight)
                .clip(
                    RoundedCornerShape(
                        topStart = CpDimens.radiusLg,
                        topEnd = CpDimens.radiusLg,
                    ),
                )
                .clickable(onClick = onZoomIn),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = CpIcons.Add,
                contentDescription = "Приблизить карту",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(22.dp),
            )
        }
        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outline,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(CpDimens.buttonHeight)
                .clip(
                    RoundedCornerShape(
                        bottomStart = CpDimens.radiusLg,
                        bottomEnd = CpDimens.radiusLg,
                    ),
                )
                .clickable(onClick = onZoomOut),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = CpIcons.Minus,
                contentDescription = "Отдалить карту",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun MapShopCarousel(
    state: MapUiState,
    onSelect: (MapShop) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selected = state.selectedShop
    val selectedShopId by rememberUpdatedState(selected?.id)
    val shops = if (selected == null || state.nearbyShops.any { it.id == selected.id }) state.nearbyShops else
        listOf(selected) + state.nearbyShops.take(9)
    if (shops.isEmpty()) return
    key(shops.map { it.id }) {
        val selectedIndex = shops.indexOfFirst { it.id == selected?.id }.coerceAtLeast(0)
        val pager = rememberPagerState(
            initialPage = carouselStartPage(shops.size, selectedIndex),
            pageCount = { if (shops.size > 1) Int.MAX_VALUE else 1 },
        )
        LaunchedEffect(selected?.id) {
            if (selected == null) return@LaunchedEffect
            val currentIndex = pager.currentPage % shops.size
            if (currentIndex != selectedIndex) {
                val right = (selectedIndex - currentIndex + shops.size) % shops.size
                val delta = if (right <= shops.size / 2) right else right - shops.size
                pager.animateScrollToPage(pager.currentPage + delta)
            }
        }
        LaunchedEffect(pager) {
            snapshotFlow { pager.settledPage }.distinctUntilChanged().drop(1).collect { page ->
                val shop = shops[page % shops.size]
                if (shop.id != selectedShopId) onSelect(shop)
            }
        }
        HorizontalPager(
            state = pager,
            modifier = modifier.fillMaxWidth().semantics {
                collectionInfo = CollectionInfo(1, shops.size)
                stateDescription = "Кофейня ${pager.currentPage % shops.size + 1} из ${shops.size}"
            },
            contentPadding = PaddingValues(horizontal = 36.dp),
            pageSpacing = 12.dp,
            userScrollEnabled = shops.size > 1,
            beyondViewportPageCount = 1,
        ) { page ->
            val shop = shops[page % shops.size]
            val isSelected = shop.id == selected?.id
            MapShopBottomSheet(
                shop = shop,
                details = state.selectedShopDetails.takeIf { isSelected },
                isLoadingDetails = isSelected && state.isLoadingShopDetails,
                distance = formatDistance(distanceToShopMeters(state.nearbyOrigin, ShopLocation("", shop.latitude, shop.longitude))),
                onOpen = {
                    if (isSelected) Navigator.navigate(Navigator.Screen.ShopDetail(shop.id)) else onSelect(shop)
                },
            )
        }
    }
}

@Composable
private fun MapShopBottomSheet(
    shop: MapShop,
    details: CoffeeShopDetails?,
    isLoadingDetails: Boolean,
    onOpen: () -> Unit,
    distance: String?,
    modifier: Modifier = Modifier,
) {
    val photoUrl = details?.shop?.photoUrl ?: details?.photos?.firstOrNull()
    val rating = details?.shop?.rating
    val reviewCount = details?.shop?.reviewCount ?: 0
    val hours = details?.schedules?.let { formatMapHoursSummary(it) }
    val isOpen = details?.shop?.isOpen == true

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CpDimens.cardRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpen)
                .padding(CpDimens.spacing3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(CpDimens.radiusMd))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    !photoUrl.isNullOrBlank() -> {
                        CoffeeShopImage(
                            imageUrl = photoUrl,
                            contentDescription = shop.title,
                            contentScale = ContentScale.Crop,
                            placeholderLabelSize = 7.sp,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    isLoadingDetails -> {
                        CoffeePeekLoader(
                            size = CpDimens.loaderButton,
                            strokeWidth = 2.dp,
                        )
                    }
                    else -> {
                        CoffeeShopPlaceholderImage(
                            labelSize = 7.sp,
                            contentDescription = "Фото ${shop.title} отсутствует",
                        )
                    }
                }
            }

            Spacer(Modifier.width(CpDimens.spacing3))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (rating != null && rating > 0) {
                        Icon(
                            CpIcons.StarFilled,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = formatOneDecimal(rating),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (reviewCount > 0) {
                            Text(
                                text = " ($reviewCount)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        Text(
                            text = if (details == null) "Кофейня рядом" else if (reviewCount > 0) "$reviewCount отзывов" else "Нет отзывов",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Text(
                    text = shop.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
                distance?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                when {
                    hours != null -> {
                        Text(
                            text = buildString {
                                append(if (isOpen) "Открыто" else "Закрыто")
                                append(" · ")
                                append(hours)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    isLoadingDetails -> {
                        Text(
                            text = "Загрузка…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }

        }
    }
}
