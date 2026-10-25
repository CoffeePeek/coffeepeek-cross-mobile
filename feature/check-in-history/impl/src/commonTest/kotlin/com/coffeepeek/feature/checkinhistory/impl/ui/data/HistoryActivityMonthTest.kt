package com.coffeepeek.feature.checkinhistory.impl.ui.data

import com.coffeepeek.feature.checkinhistory.domain.model.*
import com.coffeepeek.feature.checkinhistory.domain.repository.CheckInHistoryRepository
import com.coffeepeek.feature.checkinhistory.domain.repository.HomeHistoryReader
import com.coffeepeek.feature.checkinhistory.domain.usecase.LoadHistoryActivityMonth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class HistoryActivityMonthTest {
    private val month = HistoryMonth(2026, 10)
    private val day = "2026-10-16"
    private val period = HistoryPeriod("2026-09-30T21:00:00Z", "2026-10-31T21:00:00Z")
    private val visit = PersonalCheckIn("shop", null, "Coffee", null, "Owner", "", "${day}T08:00:00Z",
        "${day}T08:00:00Z", null, listOf("original"), null, HistoryVisibility.Private,
        HistoryModeration.NotSubmitted, 0, null, 0, false, photoCalendarUrls = listOf("thumb"))
    private var visits: Result<List<PersonalCheckIn>> = Result.success(listOf(visit))
    private var brews: Result<List<HomeHistoryDay>> = Result.success(listOf(HomeHistoryDay(day, 2)))
    private var shopBounds: HistoryPeriod? = null
    private var homeBounds: HomeHistoryDateRange? = null
    private val shops = object : CheckInHistoryRepository {
        override suspend fun page(page: Int, pageSize: Int) = error("Widget must use the full month")
        override suspend fun period(period: HistoryPeriod): Result<List<PersonalCheckIn>> {
            shopBounds = period
            return visits
        }
    }
    private val homes = object : HomeHistoryReader {
        override suspend fun page(page: Int, pageSize: Int, range: HomeHistoryDateRange?) = error("Calendar supplies Home photos")
        override suspend fun calendar(range: HomeHistoryDateRange): Result<List<HomeHistoryDay>> {
            homeBounds = range
            return brews
        }
    }
    private suspend fun load(localDate: (String) -> String? = { it.take(10) }) =
        LoadHistoryActivityMonth(shops, homes)(month, period, localDate)

    @Test fun combinesSourcesAndPreservesTheirDistinctDateBounds() = runTest {
        val result = load().getOrThrow()
        assertEquals(3L, result.days[day]?.count)
        assertEquals("thumb", result.days[day]?.photo?.url)
        assertFalse(result.isPartial)
        assertEquals(period, shopBounds)
        assertEquals(HomeHistoryDateRange("2026-10-01", "2026-10-31"), homeBounds)
    }

    @Test fun duplicateVisitsAndOutOfMonthDatesDoNotInflateActivity() = runTest {
        visits = Result.success(listOf(visit, visit, visit.copy(id = "old", visitedAt = "2026-09-30T12:00:00Z")))
        brews = Result.success(listOf(HomeHistoryDay(day, 2), HomeHistoryDay("2026-11-01", 3)))
        val result = load().getOrThrow()
        assertEquals(setOf(day), result.days.keys)
        assertEquals(3L, result.days[day]?.count)
    }

    @Test fun groupsShopInstantsUsingTheCapturedLocalZone() = runTest {
        visits = Result.success(listOf(visit.copy(visitedAt = "2026-09-30T22:00:00Z")))
        brews = Result.success(emptyList())
        val result = load { if (it == "2026-09-30T22:00:00Z") "2026-10-01" else null }.getOrThrow()
        assertEquals(setOf("2026-10-01"), result.days.keys)
    }

    @Test fun latestPhotoUsesInstantOrderAcrossSourcesAndKeepsCutoutFlag() = runTest {
        visits = Result.success(listOf(visit.copy(visitedAt = "${day}T09:30:00+03:00")))
        val photo = HistoryCalendarPhoto("home", true, "${day}T07:00:00Z")
        brews = Result.success(listOf(HomeHistoryDay(day, 1, listOf(photo))))
        assertEquals(photo, load().getOrThrow().days[day]?.photo)
    }

    @Test fun explicitlyUnavailableVariantDoesNotFallBackToOriginal() = runTest {
        visits = Result.success(listOf(visit.copy(photoCalendarUrls = listOf(""))))
        assertNull(load().getOrThrow().days[day]?.photo)
    }

    @Test fun legacyPhotosAndMissingVisitTimeStillWork() = runTest {
        visits = Result.success(listOf(visit.copy(visitedAt = "", photoCalendarUrls = null)))
        assertEquals("original", load().getOrThrow().days[day]?.photo?.url)
    }

    @Test fun oneFailedSourceRetainsTheOtherAndMarksItPartial() = runTest {
        visits = Result.failure(IllegalStateException("offline"))
        val result = load().getOrThrow()
        assertTrue(result.isPartial)
        assertEquals(2L, result.days[day]?.count)
    }

    @Test fun failedHomeSourceRetainsShopActivity() = runTest {
        brews = Result.failure(IllegalStateException("offline"))
        assertTrue(load().getOrThrow().isPartial)
        assertEquals(1L, load().getOrThrow().days[day]?.count)
    }

    @Test fun bothFailuresAreNotAnEmptySuccessfulMonth() = runTest {
        val error = IllegalStateException("offline")
        visits = Result.failure(error)
        brews = Result.failure(error)
        assertSame(error, load().exceptionOrNull())
    }

    @Test fun cancellationInResultIsRethrownBeforeLoadingTheOtherSource() = runTest {
        visits = Result.failure(CancellationException("replaced account"))
        assertFailsWith<CancellationException> { load() }
        assertNull(homeBounds)
    }

    @Test fun homeCancellationIsRethrown() = runTest {
        brews = Result.failure(CancellationException("replaced account"))
        assertFailsWith<CancellationException> { load() }
    }

    @Test fun zeroCountsAndInvalidShopDatesAreNotActiveDays() = runTest {
        brews = Result.success(listOf(HomeHistoryDay(day, 0)))
        assertTrue(load { null }.getOrThrow().days.isEmpty())
    }
}
