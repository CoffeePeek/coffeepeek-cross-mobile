package com.coffeepeek.feature.discovery.data.repository

import com.coffeepeek.feature.discovery.data.backend.DiscoveryBackend
import com.coffeepeek.feature.discovery.data.backend.ShopMapRequestDto
import com.coffeepeek.feature.discovery.data.backend.ShopSearchRequestDto
import com.coffeepeek.feature.discovery.data.mapper.toContent
import com.coffeepeek.feature.discovery.domain.model.CheckInHeatmap
import com.coffeepeek.feature.discovery.domain.model.CheckInHeatPoint
import com.coffeepeek.feature.discovery.domain.model.DiscoveryFailure
import com.coffeepeek.feature.discovery.domain.model.FailureKind
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.TimeSource

internal class DiscoveryHeatmapReader(private val backend: DiscoveryBackend) {
    private val mutex = Mutex()
    private var cachedRequest: ShopSearchRequestDto? = null
    private var cachedCounts: Map<String, Int> = emptyMap()
    private var cachedAt: TimeSource.Monotonic.ValueTimeMark? = null

    suspend fun load(mapRequest: ShopMapRequestDto, searchRequest: ShopSearchRequestDto): CheckInHeatmap {
        val content = backend.map(mapRequest).getOrThrow().toContent(null)
        val markers = content.markers.distinctBy { it.id }
        val counts = if (markers.isEmpty()) emptyMap() else counts(searchRequest)
        val points = markers.mapNotNull { marker ->
            val count = counts[marker.id] ?: return@mapNotNull null
            if (count <= 0 || !marker.coordinates.isValid) return@mapNotNull null
            CheckInHeatPoint(marker.id, marker.coordinates, count)
        }
        return CheckInHeatmap(points, content.isTruncated || content.clusters.isNotEmpty() ||
            markers.any { it.id !in counts || !it.coordinates.isValid })
    }

    private suspend fun counts(request: ShopSearchRequestDto): Map<String, Int> = mutex.withLock {
        val cacheable = !request.filters.visitedOnly && !request.filters.favoritesOnly
        if (cacheable && cachedRequest == request && cachedAt?.elapsedNow()?.inWholeSeconds?.let { it < 60 } == true) {
            return@withLock cachedCounts
        }
        val counts = loadPages(request)
        if (cacheable) {
            cachedRequest = request
            cachedCounts = counts
            cachedAt = TimeSource.Monotonic.markNow()
        }
        counts
    }

    private suspend fun loadPages(request: ShopSearchRequestDto): Map<String, Int> {
        val counts = mutableMapOf<String, Int>()
        var page = 1
        do {
            val result = backend.search(request.copy(page = page)).getOrThrow()
            if (result.currentPage != page || result.items.isEmpty() && page < result.totalPages) {
                throw DiscoveryFailure(FailureKind.Other)
            }
            result.items.forEach { counts[it.address.slug] = it.checkInCount }
            page++
        } while (page <= result.totalPages)
        return counts
    }
}
