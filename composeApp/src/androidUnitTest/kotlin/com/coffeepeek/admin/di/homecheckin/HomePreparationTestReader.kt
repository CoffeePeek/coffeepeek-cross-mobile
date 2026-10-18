package com.coffeepeek.admin.di.homecheckin

import com.coffeepeek.feature.homecheckin.domain.model.HomePreparation
import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationDateRange
import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationDay
import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationModeration
import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationPage
import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationPhoto
import com.coffeepeek.feature.homecheckin.domain.repository.HomePreparationReader

/** Read-only feature port: no legacy repository, writer, statistics or photo transport. */
internal class HomePreparationTestReader : HomePreparationReader {
    data class Query(val page: Int, val size: Int, val from: String?, val to: String?)
    val queries = mutableListOf<Query>()
    val ranges = mutableListOf<Pair<String, String>>()
    var history: suspend (Query) -> Result<HomePreparationPage> = {
        Result.success(HomePreparationPage(listOf(homeReadFixture()), 101, it.page, it.size))
    }
    var days: suspend (String, String) -> Result<List<HomePreparationDay>> = { from, _ ->
        Result.success(listOf(HomePreparationDay(from, 2)))
    }
    override suspend fun page(page: Int, pageSize: Int, range: HomePreparationDateRange?): Result<HomePreparationPage> {
        val query = Query(page, pageSize, range?.from, range?.to)
        queries += query
        return history(query)
    }
    override suspend fun calendar(range: HomePreparationDateRange): Result<List<HomePreparationDay>> {
        ranges += range.from to range.to
        return days(range.from, range.to)
    }
}

internal fun homeReadFixture() = HomePreparation(
    id = "11111111-1111-4111-8111-111111111111", preparedOn = "2026-10-16",
    preparedAtUtc = "2026-10-15T22:15:00Z", createdAtUtc = "2026-10-16T08:00:00Z", text = "Morning",
    brewMethodCode = "batch_drip", coffeeProductId = "coffee-id", beanName = "Honey Bloom", doseGrams = "75.50",
    brewDurationSeconds = 210, tasteScore = 4, drinkName = "My filter", drinkSlug = "other", customDrinkName = "My filter",
    photos = listOf(HomePreparationPhoto("photo-id", "https://fixture.invalid/api/v1/check-ins/owned/photos/photo-id?signature=issued")),
    drinkPhotoUrl = "https://fixture.invalid/api/v1/check-ins/owned/photos/cutout", visibility = null,
    moderation = HomePreparationModeration.Unknown,
)
