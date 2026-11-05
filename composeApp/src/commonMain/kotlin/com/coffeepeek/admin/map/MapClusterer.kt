package com.coffeepeek.admin.map

internal const val CLUSTER_TAP_MAX_ZOOM = 22f
internal const val CLUSTER_TAP_ANIMATION_MS = 350L

internal sealed class MapMarkerItem {
    data class Shop(val shop: MapShop) : MapMarkerItem()
    data class Cluster(
        val shops: List<MapShop>,
        val latitude: Double,
        val longitude: Double,
    ) : MapMarkerItem() {
        val key: String get() = shops.map { it.id }.sorted().joinToString(",")
        val count: Int get() = shops.size
        val label: String get() = clusterCountLabel(count)
    }
}

internal fun clusterCountLabel(count: Int): String =
    if (count > 99) "99+" else count.toString()

internal fun clusterUsesShopPoint(count: Int): Boolean = count == 1

internal fun clusterDiameterDp(count: Int): Float = if (count == 1) 31f else 34.1f

internal fun clusterFitZoom(currentZoom: Float, fittedZoom: Float): Float =
    minOf(CLUSTER_TAP_MAX_ZOOM, maxOf(currentZoom + 1f, minOf(fittedZoom, currentZoom + 2f)))

internal fun clusterPaddingFraction(): Float = 0.25f
