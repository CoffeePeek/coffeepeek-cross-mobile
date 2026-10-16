package com.coffeepeek.admin.di.homecheckin

import com.coffeepeek.feature.homecheckin.domain.HomeCheckIn
import com.coffeepeek.feature.homecheckin.domain.HomeCheckInModeration
import com.coffeepeek.feature.homecheckin.domain.HomeCheckInRepository
import com.coffeepeek.feature.homecheckin.domain.HomeCheckInVisibility
import com.coffeepeek.feature.homecheckin.domain.model.HomePreparation
import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationDateRange
import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationDay
import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationModeration
import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationPage
import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationPhoto
import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationCalendarPhoto
import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationVisibility
import com.coffeepeek.feature.homecheckin.domain.repository.HomePreparationReader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** Temporary app-owned compatibility bridge. No new HTTP client, writer or feature dependency. */
internal class LegacyHomePreparationReader(private val repository: HomeCheckInRepository) : HomePreparationReader {
    override suspend fun page(page: Int, pageSize: Int, range: HomePreparationDateRange?): Result<HomePreparationPage> = read {
        require(page in 1..10000 && pageSize in 1..100)
        val result = repository.history(page, pageSize, range?.from, range?.to).getOrThrow()
        check(result.page == page && result.pageSize == pageSize) { "Unexpected Home page metadata" }
        HomePreparationPage(result.items.map { it.snapshot() }, result.totalCount, result.page, result.pageSize)
    }

    override suspend fun calendar(range: HomePreparationDateRange): Result<List<HomePreparationDay>> = read {
        val days = repository.calendar(range.from, range.to).getOrThrow()
            .map { HomePreparationDay(it.preparedOn, it.brewCount) }
        currentCoroutineContext().ensureActive()
        val entries = mutableListOf<HomeCheckIn>()
        if (days.any { it.brewCount > 0 }) {
            var page = 1
            do {
                currentCoroutineContext().ensureActive()
                check(page <= 10000) { "Home month exceeds page limit" }
                val result = repository.history(page, 100, range.from, range.to).getOrThrow()
                currentCoroutineContext().ensureActive()
                check(result.page == page && result.pageSize == 100) { "Unexpected Home month page metadata" }
                check(result.items.isNotEmpty() || result.totalCount <= (page - 1).toLong() * 100) { "Missing Home month page" }
                entries += result.items
                page++
            } while (result.hasMore)
        }
        val photos = entries.distinctBy(HomeCheckIn::id).filter { it.preparedOn in range.from..range.to }
            .groupBy(HomeCheckIn::preparedOn).mapValues { (_, rows) -> rows.mapNotNull { row ->
                row.drinkPhotoUrl?.takeIf(String::isNotBlank)?.let { HomePreparationCalendarPhoto(it, true) }
                    ?: row.photos.firstOrNull { it.url.isNotBlank() }?.let { HomePreparationCalendarPhoto(it.url, false) }
            } }
        days.map { it.copy(photos = photos[it.preparedOn].orEmpty()) }
    }

    private suspend fun <T> read(block: suspend () -> T): Result<T> = try {
        currentCoroutineContext().ensureActive()
        val result = block()
        currentCoroutineContext().ensureActive()
        Result.success(result)
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Exception) {
        currentCoroutineContext().ensureActive()
        Result.failure(error)
    }
}

private fun HomeCheckIn.snapshot() = HomePreparation(
    id = id, preparedOn = preparedOn, preparedAtUtc = preparedAtUtc, createdAtUtc = createdAtUtc,
    text = text, brewMethodCode = brewMethodCode, coffeeProductId = coffeeProductId, beanName = beanName,
    doseGrams = doseGrams, brewDurationSeconds = brewDurationSeconds, tasteScore = tasteScore,
    drinkName = drinkName, drinkSlug = drinkSlug, customDrinkName = customDrinkName,
    photos = photos.map { HomePreparationPhoto(it.id, it.url) }, drinkPhotoUrl = drinkPhotoUrl,
    visibility = when (visibility) {
        HomeCheckInVisibility.Private -> HomePreparationVisibility.Private
        HomeCheckInVisibility.Public -> HomePreparationVisibility.Public
        null -> null
    },
    moderation = when (moderation) {
        HomeCheckInModeration.NotSubmitted -> HomePreparationModeration.NotSubmitted
        HomeCheckInModeration.Pending -> HomePreparationModeration.Pending
        HomeCheckInModeration.Approved -> HomePreparationModeration.Approved
        HomeCheckInModeration.Rejected -> HomePreparationModeration.Rejected
        HomeCheckInModeration.Unknown -> HomePreparationModeration.Unknown
    },
)
