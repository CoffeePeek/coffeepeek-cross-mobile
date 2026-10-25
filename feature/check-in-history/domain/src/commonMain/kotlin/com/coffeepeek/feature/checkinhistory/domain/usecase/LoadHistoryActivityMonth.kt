package com.coffeepeek.feature.checkinhistory.domain.usecase

import com.coffeepeek.feature.checkinhistory.domain.model.*
import com.coffeepeek.feature.checkinhistory.domain.repository.CheckInHistoryRepository
import com.coffeepeek.feature.checkinhistory.domain.repository.HomeHistoryReader
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Instant

/** Combines the two calendar contracts, retaining partial data and choosing the latest daily photo. */
class LoadHistoryActivityMonth(
    private val shops: CheckInHistoryRepository,
    private val homes: HomeHistoryReader,
) {
    suspend operator fun invoke(month: HistoryMonth, period: HistoryPeriod,
        localDate: (String) -> String?): Result<HistoryActivityMonth> {
        val shopResult = shops.period(period).checkCancellation()
        val homeResult = homes.calendar(HomeHistoryDateRange.month(month)).checkCancellation()
        if (shopResult.isFailure && homeResult.isFailure) return Result.failure(shopResult.exceptionOrNull()!!)
        val visits = shopResult.getOrNull().orEmpty().distinctBy { it.id }
            .groupBy { localDate(it.effectiveVisitedAt) }.filterKeys { it != null && month.containsDate(it) }
        val brews = homeResult.getOrNull().orEmpty().filter { month.containsDate(it.preparedOn) }.associateBy { it.preparedOn }
        val days = (visits.keys.filterNotNull() + brews.keys).associateWith { date ->
            val shopDays = visits[date].orEmpty()
            val homeDay = brews[date]
            HistoryActivityDay(shopDays.size.toLong() + (homeDay?.brewCount ?: 0), latestPhoto(shopDays, homeDay))
        }.filterValues { it.count > 0 }
        return Result.success(HistoryActivityMonth(month, days, shopResult.isFailure || homeResult.isFailure))
    }

    private fun latestPhoto(shops: List<PersonalCheckIn>, home: HomeHistoryDay?): HistoryCalendarPhoto? {
        val photos = shops.mapNotNull { visit ->
            (visit.photoCalendarUrls ?: visit.photoUrls).firstOrNull(String::isNotBlank)?.let {
                HistoryCalendarPhoto(it, false, visit.effectiveVisitedAt)
            }
        } + home?.photos.orEmpty()
        return photos.filter { it.url.isNotBlank() }.sortedByDescending { instant(it.occurredAt) }.firstOrNull()
    }

    private fun instant(value: String?): Instant? = try {
        value?.let(Instant::parse)
    } catch (_: IllegalArgumentException) {
        null
    }

    private fun <T> Result<T>.checkCancellation(): Result<T> {
        exceptionOrNull()?.let { if (it is CancellationException) throw it }
        return this
    }
}
