package com.coffeepeek.domain.feature.feed

import com.coffeepeek.domain.model.CheckIn

data class FeedFilters(
    val citySlug: String? = null,
    val coffeeShopSlug: String? = null,
    val authorSlug: String? = null,
)

data class FeedItem(val publishedAtUtc: String, val checkIn: CheckIn)
data class FeedPage(val items: List<FeedItem>, val nextCursor: String?)

class FeedLoadException(message: String, val restartPagination: Boolean) : Exception(message)

interface FeedRepository {
    suspend fun getFeed(pageSize: Int, cursor: String? = null, filters: FeedFilters = FeedFilters()): Result<FeedPage>
}
