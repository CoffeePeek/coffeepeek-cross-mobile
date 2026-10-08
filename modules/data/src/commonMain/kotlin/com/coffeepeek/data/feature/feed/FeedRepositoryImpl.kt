package com.coffeepeek.data.feature.feed

import com.coffeepeek.api.feature.feed.FeedApiException
import com.coffeepeek.api.feature.feed.FeedApiService
import com.coffeepeek.data.mapper.toDomain
import com.coffeepeek.data.util.FileUrlResolver
import com.coffeepeek.domain.feature.feed.FeedFilters
import com.coffeepeek.domain.feature.feed.FeedItem
import com.coffeepeek.domain.feature.feed.FeedLoadException
import com.coffeepeek.domain.feature.feed.FeedPage
import com.coffeepeek.domain.feature.feed.FeedRepository

internal class FeedRepositoryImpl(
    private val api: FeedApiService,
    private val fileUrls: FileUrlResolver,
) : FeedRepository {
    override suspend fun getFeed(pageSize: Int, cursor: String?, filters: FeedFilters): Result<FeedPage> =
        api.getFeed(pageSize, cursor, filters.citySlug, filters.coffeeShopSlug, filters.authorSlug).fold(
            onSuccess = { page -> Result.success(FeedPage(
                page.items.map { FeedItem(it.publishedAtUtc, it.checkIn.toDomain(fileUrls)) }, page.nextCursor,
            )) },
            onFailure = { error -> Result.failure(
                if (error is FeedApiException) FeedLoadException(error.message.orEmpty(), error.restartPagination) else error,
            ) },
        )
}
