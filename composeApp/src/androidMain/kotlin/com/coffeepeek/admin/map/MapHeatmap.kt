package com.coffeepeek.admin.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression.*
import org.maplibre.android.style.layers.HeatmapLayer
import org.maplibre.android.style.layers.PropertyFactory.*
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point

private const val HEATMAP_SOURCE = "cp-check-in-heatmap"
private const val HEATMAP_LAYER = "cp-check-in-heatmap-layer"

@Composable
internal fun MapHeatmapEffect(map: MapLibreMap?, styleGeneration: Int, points: List<MapHeatPoint>) {
    LaunchedEffect(points, styleGeneration, map) {
        if (styleGeneration > 0) map?.style?.let { updateMapHeatmap(it, points) }
    }
}

private fun updateMapHeatmap(style: Style, points: List<MapHeatPoint>) {
    val features = points.map { point ->
        Feature.fromGeometry(Point.fromLngLat(point.longitude, point.latitude)).apply {
            addNumberProperty("weight", point.weight)
        }
    }
    val collection = FeatureCollection.fromFeatures(features)
    val source = style.getSourceAs<GeoJsonSource>(HEATMAP_SOURCE)
    if (source != null) source.setGeoJson(collection)
    else if (points.isNotEmpty()) {
        style.addSource(GeoJsonSource(HEATMAP_SOURCE, collection))
        style.addLayer(heatmapLayer())
    }
}

private fun heatmapLayer() = HeatmapLayer(HEATMAP_LAYER, HEATMAP_SOURCE).withProperties(
    heatmapWeight(get("weight")),
    heatmapIntensity(1f),
    heatmapRadius(32f),
    heatmapOpacity(0.8f),
    heatmapColor(interpolate(linear(), heatmapDensity(),
        stop(0, rgba(71, 183, 199, 0)),
        stop(0.2, rgb(71, 183, 199)),
        stop(0.45, rgb(228, 184, 84)),
        stop(0.7, rgb(228, 119, 50)),
        stop(1, rgb(200, 62, 54)),
    )),
)
