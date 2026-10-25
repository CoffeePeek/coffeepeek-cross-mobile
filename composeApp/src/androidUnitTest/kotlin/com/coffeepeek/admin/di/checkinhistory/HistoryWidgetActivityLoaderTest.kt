package com.coffeepeek.admin.di.checkinhistory

import com.coffeepeek.admin.di.checkinhistory.widget.loadHistoryWidgetActivity
import com.coffeepeek.feature.checkinhistory.domain.model.*
import com.coffeepeek.feature.checkinhistory.domain.repository.CheckInHistoryRepository
import com.coffeepeek.feature.checkinhistory.domain.repository.HomeHistoryReader
import com.coffeepeek.feature.checkinhistory.domain.usecase.LoadHistoryActivityMonth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlin.test.*

class HistoryWidgetActivityLoaderTest {
    private val periods = mutableListOf<HistoryPeriod>()
    private val ranges = mutableListOf<HomeHistoryDateRange>()
    private var failure: Exception? = null
    private var failAll = false
    private val shops = object : CheckInHistoryRepository {
        override suspend fun page(page: Int, pageSize: Int) = error("Not a page request")
        override suspend fun period(period: HistoryPeriod): Result<List<PersonalCheckIn>> {
            periods += period
            return if (failure != null && (failAll || period.fromInclusive.startsWith("2026-09"))) Result.failure(failure!!)
            else Result.success(emptyList())
        }
    }
    private val homes = object : HomeHistoryReader {
        override suspend fun page(page: Int, pageSize: Int, range: HomeHistoryDateRange?) = error("Not a page request")
        override suspend fun calendar(range: HomeHistoryDateRange): Result<List<HomeHistoryDay>> {
            ranges += range
            if (failure != null && (failAll || range.from == "2026-10-01")) return Result.failure(failure!!)
            return Result.success(listOf("2026-10-20", "2026-10-31", "2026-11-02")
                .filter { it in range.from..range.to }.map { HomeHistoryDay(it, 1) })
        }
    }
    private suspend fun load(today: String = "2026-11-02") = loadHistoryWidgetActivity(
        LoadHistoryActivityMonth(shops, homes), AndroidCheckInHistoryCalendar(
            { ZoneId.of("Europe/Berlin") }, Clock.fixed(Instant.parse("${today}T12:00:00Z"), ZoneId.of("UTC")),
        ).snapshot(),
    )

    @Test fun loadsBoundaryMonthsOnceWithTheirOwnDstBounds() = runTest {
        val result = load()
        assertEquals(2, result.size)
        assertEquals(2, periods.size)
        assertTrue(HistoryPeriod("2026-09-30T22:00:00Z", "2026-10-31T23:00:00Z") in periods)
        assertTrue(HomeHistoryDateRange("2026-10-01", "2026-10-31") in ranges)
        assertEquals(setOf("2026-10-31", "2026-11-02"), result.flatMap { it.days.keys }.toSet())
    }

    @Test fun middleOfMonthDoesNotReadAnUnneededPreviousMonth() = runTest {
        load("2026-10-25")
        assertEquals(1, periods.size)
        assertEquals(listOf(HomeHistoryDateRange("2026-10-01", "2026-10-31")), ranges)
    }

    @Test fun failureOfOneMonthKeepsTheOtherMonthsData() = runTest {
        failure = IllegalStateException("offline")
        val result = load()
        assertTrue(result.single { it.month.month == 10 }.isPartial)
        assertEquals(setOf("2026-11-02"), result.single { it.month.month == 11 }.days.keys)
    }

    @Test fun completeFailureIsNotReportedAsAnEmptyCalendar() = runTest {
        failure = IllegalStateException("offline")
        failAll = true
        assertFailsWith<IllegalStateException> { load() }
    }

    @Test fun cancelledMonthReadDoesNotReturnPartialPrivateData() = runTest {
        failure = CancellationException("session replaced")
        failAll = true
        assertFailsWith<CancellationException> { load() }
        assertEquals(1, periods.size)
        assertTrue(ranges.isEmpty())
    }
}
