package com.coffeepeek.admin.ui.screen.map

import com.coffeepeek.domain.model.MapBounds
import com.coffeepeek.domain.model.MapContent
import com.coffeepeek.domain.model.ShopFilters

/** Composition adapts the shared Discovery map endpoint for the existing iOS presentation. */
fun interface MapContentSource {
    suspend fun load(bounds: MapBounds, zoom: Float, filters: ShopFilters): Result<MapContent>
}
