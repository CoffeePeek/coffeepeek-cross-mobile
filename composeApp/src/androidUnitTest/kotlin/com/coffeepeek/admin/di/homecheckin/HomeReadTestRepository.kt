package com.coffeepeek.admin.di.homecheckin

import com.coffeepeek.feature.homecheckin.domain.HomeBeanStats
import com.coffeepeek.feature.homecheckin.domain.HomeCalendarDay
import com.coffeepeek.feature.homecheckin.domain.HomeCheckIn
import com.coffeepeek.feature.homecheckin.domain.HomeCheckInInput
import com.coffeepeek.feature.homecheckin.domain.HomeCheckInModeration
import com.coffeepeek.feature.homecheckin.domain.HomeCheckInPage
import com.coffeepeek.feature.homecheckin.domain.HomeCheckInPhoto
import com.coffeepeek.feature.homecheckin.domain.HomeCheckInRepository
import com.coffeepeek.feature.homecheckin.domain.HomeMethodStats

/** Fake reader; every writer/detail/statistics/photo operation fails if this slice calls it. */
internal class HomeReadTestRepository : HomeCheckInRepository {
    data class Query(val page: Int, val size: Int, val from: String?, val to: String?)
    val queries = mutableListOf<Query>()
    val ranges = mutableListOf<Pair<String, String>>()
    var history: suspend (Query) -> Result<HomeCheckInPage> = {
        Result.success(HomeCheckInPage(listOf(homeReadFixture()), 101, it.page, it.size))
    }
    var days: suspend (String, String) -> Result<List<HomeCalendarDay>> = { from, _ ->
        Result.success(listOf(HomeCalendarDay(from, 2)))
    }
    override suspend fun history(page: Int, pageSize: Int, from: String?, to: String?): Result<HomeCheckInPage> {
        val query = Query(page, pageSize, from, to)
        queries += query
        return history(query)
    }
    override suspend fun calendar(from: String, to: String): Result<List<HomeCalendarDay>> {
        ranges += from to to
        return days(from, to)
    }
    override suspend fun create(input: HomeCheckInInput): Result<HomeCheckIn> = error("No Home write")
    override suspend fun update(id: String, input: HomeCheckInInput): Result<HomeCheckIn> = error("No Home write")
    override suspend fun delete(id: String): Result<Unit> = error("No Home write")
    override suspend fun detail(id: String): Result<HomeCheckIn> = error("No detail request")
    override suspend fun beanStats(): Result<List<HomeBeanStats>> = error("No statistics request")
    override suspend fun methodStats(): Result<List<HomeMethodStats>> = error("No statistics request")
    override suspend fun photo(url: String): Result<ByteArray> = error("No photo request")
}

internal fun homeReadFixture() = HomeCheckIn(
    id = "11111111-1111-4111-8111-111111111111", preparedOn = "2026-10-16",
    preparedAtUtc = "2026-10-15T22:15:00Z", createdAtUtc = "2026-10-16T08:00:00Z", text = "Morning",
    brewMethodCode = "batch_drip", coffeeProductId = "coffee-id", beanName = "Honey Bloom", doseGrams = "75.50",
    brewDurationSeconds = 210, tasteScore = 4, drinkName = "My filter", drinkSlug = "other", customDrinkName = "My filter",
    photos = listOf(HomeCheckInPhoto("photo-id", "https://fixture.invalid/api/v1/check-ins/owned/photos/photo-id?signature=issued")),
    visibility = null,
    moderation = HomeCheckInModeration.Unknown,
)
