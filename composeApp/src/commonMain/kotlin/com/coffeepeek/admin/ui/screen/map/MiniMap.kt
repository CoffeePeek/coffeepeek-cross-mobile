package com.coffeepeek.admin.ui.screen.map

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.di.platformViewModel
import com.coffeepeek.admin.location.rememberPermittedUserLocation
import com.coffeepeek.admin.map.CoffeeMap
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.CoffeePeekLoader
import com.coffeepeek.admin.ui.component.GlassControlIcon
import com.coffeepeek.admin.ui.component.GlassIconButton
import com.coffeepeek.admin.ui.icons.CpIcons

@Composable
internal fun MiniMap(
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
    vm: MapViewModel = platformViewModel(),
) {
    val state by vm.state.collectAsState()
    val userLocation = rememberPermittedUserLocation()
    LaunchedEffect(userLocation) {
        userLocation?.let { vm.onNearbyOriginChanged(it.latitude, it.longitude) }
    }
    val shape = RoundedCornerShape(CpDimens.radius2xl)
    Box(
        modifier = modifier
            .height(240.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline, shape),
    ) {
        CoffeeMap(
            shops = state.shops,
            clusters = state.clusters,
            selectedShopId = null,
            onBoundsChanged = vm::onBoundsChanged,
            onShopClick = { Navigator.navigate(Navigator.Screen.ShopDetail(it.id)) },
            modifier = Modifier.fillMaxSize(),
            cameraTarget = state.cameraTarget,
            cameraZoom = state.cameraZoom,
            onCameraTargetApplied = vm::onCameraTargetApplied,
            isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f,
            myLocationRequestKey = state.myLocationRequest,
            onMyLocationFound = vm::onMyLocationApplied,
            requestLocationPermissionOnLoad = false,
        )
        GlassIconButton(
            onClick = onExpand,
            contentDescription = "Развернуть карту на весь экран",
            modifier = Modifier.align(Alignment.TopEnd).padding(CpDimens.spacing2),
            hazeState = null,
        ) {
            Icon(CpIcons.Expand, contentDescription = null, modifier = Modifier.size(22.dp))
        }
        MapZoomControl(
            onZoomIn = vm::zoomIn,
            onZoomOut = vm::zoomOut,
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = CpDimens.spacing2),
        )
        MapControlButton(
            icon = GlassControlIcon.Location,
            onClick = vm::requestMyLocation,
            contentDescription = "Моё местоположение",
            modifier = Modifier.align(Alignment.BottomEnd).padding(CpDimens.spacing2),
        ) {
            Icon(CpIcons.Navigation, contentDescription = null, modifier = Modifier.size(24.dp))
        }
        if (state.isLoading) {
            CoffeePeekLoader(
                size = 22.dp,
                strokeWidth = 2.dp,
                modifier = Modifier.align(Alignment.TopStart).padding(CpDimens.spacing3),
            )
        }
    }
}
