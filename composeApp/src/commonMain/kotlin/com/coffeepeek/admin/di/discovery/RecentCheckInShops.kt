package com.coffeepeek.admin.di.discovery

import com.coffeepeek.feature.checkinhistory.domain.repository.CheckInHistoryRepository
import com.coffeepeek.feature.discovery.api.DiscoveryRecentShops
import com.coffeepeek.feature.discovery.domain.model.Coordinates
import com.coffeepeek.feature.discovery.domain.model.ShopSummary
import com.coffeepeek.feature.shop.domain.repository.ShopDetailsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.time.Instant

/** Converts personal visits into place choices without exposing history to Discovery. */
internal class RecentCheckInShops(
    private val history: CheckInHistoryRepository,
    private val details: ShopDetailsRepository,
) : DiscoveryRecentShops {
    override suspend fun load(): Result<List<ShopSummary>> {
        val visits = history.page(1, 30)
        visits.exceptionOrNull()?.let { if (it is CancellationException) throw it }
        return visits.map { page -> coroutineScope {
            page.items.sortedByDescending { it.effectiveVisitedAt.visitEpochMillis() }
                .filter { !it.shop?.slug.isNullOrBlank() }
                .distinctBy { it.shop!!.slug }
                .take(5)
                .map { visit -> async { summary(checkNotNull(visit.shop).slug, visit.shopName) } }
                .awaitAll()
        } }
    }

    private suspend fun summary(slug: String, historyTitle: String): ShopSummary {
        val detail = details.getDetails(slug)
        detail.exceptionOrNull()?.let { if (it is CancellationException) throw it }
        val overview = detail.getOrNull()?.overview
        return ShopSummary(id = slug, title = overview?.title ?: historyTitle.ifBlank { slug },
            address = overview?.address, photoUrl = overview?.photos?.firstOrNull()?.previewUrl,
            coordinates = overview?.latitude?.let { lat -> overview.longitude?.let { lon ->
                Coordinates(lat, lon).takeIf(Coordinates::isValid)
            } })
    }
}

private fun String.visitEpochMillis(): Long = try {
    Instant.parse(this).toEpochMilliseconds()
} catch (_: IllegalArgumentException) {
    Long.MIN_VALUE
}
