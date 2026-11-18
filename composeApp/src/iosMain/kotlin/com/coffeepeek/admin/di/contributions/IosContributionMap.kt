package com.coffeepeek.admin.di.contributions

import coffeepeek.composeapp.generated.resources.Res
import com.coffeepeek.admin.location.PlatformLocation
import com.coffeepeek.admin.map.IosNativeMapCallbacks
import com.coffeepeek.admin.map.IosNativeMapRegistry
import com.coffeepeek.admin.map.nativeMapStateJson
import com.coffeepeek.feature.contributions.impl.NativeContributionFormSession
import platform.UIKit.UIView

class IosContributionMap internal constructor(private val session: NativeContributionFormSession) : IosNativeMapCallbacks {
    private val provider get() = checkNotNull(IosNativeMapRegistry.provider)

    fun createView(): UIView = provider.createMapView(Res.getUri("drawable/map_cup_fill.png"))

    fun update(view: UIView, dark: Boolean) {
        provider.updateMapView(view, nativeMapStateJson(emptyList(), emptyList(), emptyList(), null, dark, true),
            PlatformLocation.hasPermission(), this)
        val state = session.current().map ?: return
        val latitude = state.cameraLatitude ?: return
        val longitude = state.cameraLongitude ?: return
        provider.moveCamera(view, latitude, longitude, 16f, false)
        session.cameraApplied()
    }

    override fun onCameraCenterChanged(latitude: Double, longitude: Double) = session.moveMap(latitude, longitude)
    override fun onShopClick(shopId: String) = Unit
    override fun onZoneClick(zoneId: String) = Unit
    override fun onBoundsChanged(minLat: Double, minLon: Double, maxLat: Double, maxLon: Double, zoom: Float) = Unit
}

