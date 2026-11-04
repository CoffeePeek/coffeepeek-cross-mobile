package com.coffeepeek.admin.map

import android.content.Context
import com.coffeepeek.R
import com.coffeepeek.domain.model.MapCluster
import com.coffeepeek.domain.model.MapShop

internal fun mapMarkerSpecs(
    context: Context, shops: List<MapShop>, clusters: List<MapCluster>, selectedId: String?, dark: Boolean,
    onShopClick: (MapShop) -> Unit, onClusterClick: (MapCluster) -> Unit,
): List<MapMarkerViewSpec> = clusters.map { cluster ->
    MapMarkerViewSpec(
        target = MapMarkerTarget("cluster:${cluster.id}", cluster.latitude, cluster.longitude, cluster.bounds),
        bitmap = MapMarkerIcons.clusterBitmap(context, cluster.count, dark),
        description = context.getString(R.string.map_cluster_description, cluster.count),
        onClick = { onClusterClick(cluster) },
    )
} + shops.map { shop ->
    val selected = shop.id == selectedId
    MapMarkerViewSpec(
        target = MapMarkerTarget("shop:${shop.id}", shop.latitude, shop.longitude),
        bitmap = MapMarkerIcons.pinBitmap(context, shop.type, if (selected) MapPinVisual.Selected else MapPinVisual.Default, dark),
        description = shop.title, selected = selected, onClick = { onShopClick(shop) },
    )
}
