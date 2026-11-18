@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.admin.di.discovery

import coffeepeek.composeapp.generated.resources.Res
import com.coffeepeek.admin.location.PlatformLocation
import com.coffeepeek.admin.map.IosNativeMapRegistry
import com.coffeepeek.admin.map.NativeMapSession
import com.coffeepeek.feature.discovery.api.MapCamera
import com.coffeepeek.feature.discovery.domain.model.Coordinates
import com.coffeepeek.feature.discovery.domain.model.MapBounds
import com.coffeepeek.feature.discovery.impl.ui.DiscoveryIosPresentation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import platform.UIKit.UIView
import platform.UIKit.endEditing

/** Reuses the existing MapLibre session and serialized canvas protocol; only the parent layout becomes SwiftUI. */
internal class IosDiscoveryMap(
    private val scope: CoroutineScope, private val presentation: () -> DiscoveryIosPresentation?,
    private val isCurrent: () -> Boolean,
) {
    private val provider get() = checkNotNull(IosNativeMapRegistry.provider)
    private var view: UIView? = null
    private var dark = false
    private var camera: MapCamera? = null
    private var locationRequest: Int? = null
    private var locationJob: Job? = null
    private val sessionOwner = lazy { NativeMapSession(provider,
        { bounds, zoom -> data()?.canvasCallbacks?.onBoundsChanged?.invoke(
            MapBounds(bounds.minLat, bounds.minLon, bounds.maxLat, bounds.maxLon), zoom) },
        { id -> if (data()?.canvas?.isPreview == false) {
            dismissInput()
            data()?.canvasCallbacks?.onMarkerClick?.invoke(id)
        } },
        { id -> if (data()?.canvas?.isPreview == false) data()?.canvasCallbacks?.onZoneClick?.invoke(id) },
        { _, _ -> },
    ) }

    private val session get() = sessionOwner.value

    fun createView(): UIView {
        val data = data()
        if (data?.canvas?.camera == null) data?.viewportCamera?.let {
            session.moveCamera(it.target.latitude, it.target.longitude, it.zoom, animated = false)
        }
        return provider.createMapView(Res.getUri("drawable/map_cup_fill.png")).also { attach(it, dark) }
    }

    fun attach(next: UIView, dark: Boolean) {
        view = next
        this.dark = dark
        session.attach(next)
        update()
    }

    fun update() {
        val canvas = data()?.canvas ?: return
        if (view == null) return
        session.updateContent(canvas.markers.map { it.toMapShop() }, canvas.clusters.map {
            com.coffeepeek.admin.map.MapCluster(it.id, it.coordinates.latitude, it.coordinates.longitude, it.count,
                com.coffeepeek.admin.map.MapBounds(it.bounds.minLat, it.bounds.minLon, it.bounds.maxLat, it.bounds.maxLon))
        }, canvas.zones.map { it.toMapZone() }, canvas.selectedShopId, dark, !canvas.isPreview,
            PlatformLocation.hasPermission(), canvas.heatmap.map {
                com.coffeepeek.admin.map.MapHeatPoint(it.coordinates.latitude, it.coordinates.longitude, it.weight)
            })
        applyCamera(canvas.camera)
        locate(canvas.myLocationRequest)
    }

    private fun applyCamera(next: MapCamera?) {
        if (next != null && next != camera) {
            camera = next
            session.moveCamera(next.target.latitude, next.target.longitude, next.zoom, animated = true)
            data()?.canvasCallbacks?.onCameraApplied?.invoke()
        }
        camera = next
    }

    private fun locate(request: Int) {
        if (!PlatformLocation.hasPermission() || locationRequest == request) return
        locationRequest = request
        if (request == 0 && (data()?.canvas?.camera != null || session.cameraRevision > 0)) return
        val revision = session.cameraRevision
        locationJob?.cancel()
        locationJob = scope.launch {
            val point = PlatformLocation.getLastKnownLocation()
            if (!isCurrent()) return@launch
            if (point == null) {
                if (request > 0) data()?.canvasCallbacks?.onLocationDenied?.invoke()
            } else if (request > 0 || (data()?.canvas?.camera == null && session.cameraRevision == revision)) {
                session.moveCamera(point.latitude, point.longitude, 15f, animated = true)
                data()?.canvasCallbacks?.onMyLocationFound?.invoke(Coordinates(point.latitude, point.longitude))
            }
        }
    }

    fun dismissInput() { view?.window?.endEditing(true) }

    fun close() {
        locationJob?.cancel()
        if (sessionOwner.isInitialized()) session.detach()
        view = null
    }

    private fun data() = presentation().takeIf { isCurrent() }
}
