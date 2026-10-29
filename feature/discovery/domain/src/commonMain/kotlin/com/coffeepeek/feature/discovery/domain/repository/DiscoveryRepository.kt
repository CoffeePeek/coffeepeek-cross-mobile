package com.coffeepeek.feature.discovery.domain.repository

import com.coffeepeek.feature.discovery.domain.model.DiscoveryCatalogs
import com.coffeepeek.feature.discovery.domain.model.DiscoveryCriteria
import com.coffeepeek.feature.discovery.domain.model.MapBounds
import com.coffeepeek.feature.discovery.domain.model.MapContent
import com.coffeepeek.feature.discovery.domain.model.Paged
import com.coffeepeek.feature.discovery.domain.model.ShopQuery
import com.coffeepeek.feature.discovery.domain.model.ShopSummary

/** Reads of the shop search and the map. Failures are [com.coffeepeek.feature.discovery.domain.model.DiscoveryFailure]. */
interface DiscoveryRepository {
    /** A page of shops selected by the server; an origin additionally scopes and sorts nearby results. */
    suspend fun search(query: ShopQuery): Result<Paged<ShopSummary>>

    /** The same shop selection as search, grouped for the visible map at [zoom]. */
    suspend fun mapContent(bounds: MapBounds, zoom: Float, criteria: DiscoveryCriteria): Result<MapContent>

    /** The lists the filters choose from, read once and kept. */
    suspend fun catalogs(): Result<DiscoveryCatalogs>
}
