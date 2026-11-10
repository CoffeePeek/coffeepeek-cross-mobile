package com.coffeepeek.admin.di.contributions

import com.coffeepeek.admin.location.LocationPermissionEffect
import com.coffeepeek.admin.map.CoffeeMap
import com.coffeepeek.admin.ui.component.HapticEvent
import com.coffeepeek.admin.ui.component.platformHaptic
import com.coffeepeek.feature.contributions.api.*

private fun haptic(success: Boolean) = platformHaptic(if (success) HapticEvent.Success else HapticEvent.Error)

internal fun addShopHost() = AddShopHost(
    locationPermission = { onGranted, onDenied -> LocationPermissionEffect(requestKey = 1, onGranted = onGranted, onDenied = onDenied) },
    mapCanvas = contributionMapCanvas(),
    photoPicker = AppContributionPhotoPicker(),
    feedback = ::haptic,
)

internal fun addRoasterHost() = AddRoasterHost(photoPicker = AppContributionPhotoPicker(), feedback = ::haptic)
internal fun changeEditorHost() = ChangeEditorHost(photoPicker = AppContributionPhotoPicker(), feedback = ::haptic)

/** Reuses the existing platform map/provider; dialog state, pin, controls and geocoding belong to Contributions. */
private fun contributionMapCanvas() = ContributionMapCanvas { state, callbacks, modifier ->
    CoffeeMap(shops = emptyList(), selectedShopId = null, onBoundsChanged = { _, _ -> }, onShopClick = {}, modifier = modifier,
        cameraTarget = state.cameraTarget?.let { it.latitude to it.longitude }, cameraZoom = state.zoom,
        onCameraTargetApplied = callbacks.onCameraApplied, isDarkTheme = state.isDarkTheme, myLocationRequestKey = state.locationRequest,
        onMyLocationFound = { lat, lon -> callbacks.onMyLocationFound(GeoPoint(lat, lon)) },
        onCameraCenterChanged = { lat, lon -> callbacks.onCenterChanged(GeoPoint(lat, lon)) })
}
