package com.coffeepeek.admin.di.checkinhistory

import com.coffeepeek.feature.checkinhistory.domain.model.HistoryModeration
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryVisibility
import com.coffeepeek.feature.checkinhistory.domain.model.HomeHistoryDateRange
import com.coffeepeek.feature.checkinhistory.domain.model.HomeHistoryDay
import com.coffeepeek.feature.checkinhistory.domain.model.HomeHistoryEntry
import com.coffeepeek.feature.checkinhistory.domain.model.HomeHistoryPage
import com.coffeepeek.feature.checkinhistory.domain.model.HomeHistoryPhoto
import com.coffeepeek.feature.checkinhistory.domain.model.HomeHistoryRecipe
import com.coffeepeek.feature.checkinhistory.domain.repository.HomeHistoryReader
import com.coffeepeek.feature.homecheckin.domain.model.HomePreparation
import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationDateRange
import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationModeration
import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationVisibility
import com.coffeepeek.feature.homecheckin.domain.repository.HomePreparationReader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** Only application composition knows both features' contracts. Bind to the history account lease before UI use. */
internal class HomeHistoryReaderAdapter(private val reader: HomePreparationReader) : HomeHistoryReader {
    override suspend fun page(page: Int, pageSize: Int, range: HomeHistoryDateRange?): Result<HomeHistoryPage> = read {
        require(page in 1..10000 && pageSize in 1..100)
        val result = reader.page(page, pageSize, range?.let { HomePreparationDateRange(it.from, it.to) }).getOrThrow()
        check(result.page == page && result.pageSize == pageSize) { "Unexpected Home history page metadata" }
        HomeHistoryPage(result.items.map { it.historyEntry() }, result.totalCount, result.page, result.pageSize)
    }

    override suspend fun calendar(range: HomeHistoryDateRange): Result<List<HomeHistoryDay>> = read {
        reader.calendar(HomePreparationDateRange(range.from, range.to)).getOrThrow()
            .map { day -> HomeHistoryDay(day.preparedOn, day.brewCount,
                day.photos.map { com.coffeepeek.feature.checkinhistory.domain.model.HistoryCalendarPhoto(it.url, it.isCutout, it.preparedAtUtc) }) }
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

private fun HomePreparation.historyEntry() = HomeHistoryEntry(
    id = id, preparedOn = preparedOn, preparedAtUtc = preparedAtUtc, createdAtUtc = createdAtUtc, note = text,
    recipe = HomeHistoryRecipe(brewMethodCode, coffeeProductId, beanName, doseGrams, brewDurationSeconds, tasteScore),
    drinkName = drinkName, drinkSlug = drinkSlug, customDrinkName = customDrinkName,
    photos = photos.map { HomeHistoryPhoto(it.id, it.url) }, drinkPhotoUrl = drinkPhotoUrl,
    visibility = when (visibility) {
        HomePreparationVisibility.Private -> HistoryVisibility.Private
        HomePreparationVisibility.Public -> HistoryVisibility.Public
        null -> null
    },
    moderation = when (moderation) {
        HomePreparationModeration.NotSubmitted -> HistoryModeration.NotSubmitted
        HomePreparationModeration.Pending -> HistoryModeration.Pending
        HomePreparationModeration.Approved -> HistoryModeration.Approved
        HomePreparationModeration.Rejected -> HistoryModeration.Rejected
        HomePreparationModeration.Unknown -> HistoryModeration.Unknown
    },
)
