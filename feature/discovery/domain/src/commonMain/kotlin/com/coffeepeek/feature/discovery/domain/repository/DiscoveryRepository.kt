package com.coffeepeek.feature.discovery.domain.repository

import com.coffeepeek.feature.discovery.domain.model.DiscoveryCatalogs
import com.coffeepeek.feature.discovery.domain.model.DiscoveryFilters
import com.coffeepeek.feature.discovery.domain.model.MapBounds
import com.coffeepeek.feature.discovery.domain.model.MapContent
import com.coffeepeek.feature.discovery.domain.model.Paged
import com.coffeepeek.feature.discovery.domain.model.ShopQuery
import com.coffeepeek.feature.discovery.domain.model.ShopSummary

/** Reads of the shop search and the map. Failures are [com.coffeepeek.feature.discovery.domain.model.DiscoveryFailure]. */
interface DiscoveryRepository {
    /**
     * A page of shops. A query with an origin is the nearby search (sorted by distance, the switches are applied by the server); any other query is the plain search,
     * which does not send open/new/visited/favorites: those are applied to the loaded page by [com.coffeepeek.feature.discovery.domain.rules.visibleShops].
     */
    suspend fun search(query: ShopQuery): Result<Paged<ShopSummary>>

    /** The shops and the zones of [bounds] at [zoom]; the filters of the map are the ones of the search that the map understands. */
    suspend fun mapContent(bounds: MapBounds, zoom: Float, filters: DiscoveryFilters): Result<MapContent>

    /** The lists the filters choose from, read once and kept. */
    suspend fun catalogs(): Result<DiscoveryCatalogs>
}
