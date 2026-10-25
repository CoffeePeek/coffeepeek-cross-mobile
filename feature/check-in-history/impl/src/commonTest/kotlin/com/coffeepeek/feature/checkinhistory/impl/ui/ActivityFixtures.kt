package com.coffeepeek.feature.checkinhistory.impl.ui

import com.coffeepeek.feature.checkinhistory.api.*
import com.coffeepeek.feature.checkinhistory.domain.model.*
import com.coffeepeek.feature.checkinhistory.domain.repository.*

internal class ActivityClock(var today: String = "2026-10-25") {
    var failed = false
    fun snapshot(): CheckInHistoryCalendarSnapshot {
        check(!failed)
        val date = today
        return object : CheckInHistoryCalendarSnapshot {
            override val today = date
            override val zoneId = "UTC"
            override fun localDate(instant: String) = instant.substringBefore('T')
            override fun datePeriod(fromDate: String, toExclusiveDate: String) =
                CheckInHistoryDateBounds("${fromDate}T00:00:00Z", "${toExclusiveDate}T00:00:00Z")
            override fun monthPeriod(year: Int, month: Int) = HistoryMonth(year, month).let {
                datePeriod(it.date(1), it.plusMonths(1).date(1))
            }
        }
    }
}

internal class ActivityShops : CheckInHistoryRepository {
    val periods = mutableListOf<HistoryPeriod>()
    var load: suspend (HistoryPeriod) -> Result<List<PersonalCheckIn>> = { Result.success(emptyList()) }
    override suspend fun period(period: HistoryPeriod): Result<List<PersonalCheckIn>> {
        periods += period
        return load(period)
    }
    override suspend fun page(page: Int, pageSize: Int): Result<CheckInHistoryPage> = error("Activity never loads a shop timeline")
}

internal class ActivityHomes : HomeHistoryReader {
    val ranges = mutableListOf<HomeHistoryDateRange>()
    val pages = mutableListOf<Pair<Int, HomeHistoryDateRange?>>()
    var days: Result<List<HomeHistoryDay>> = Result.success(emptyList())
    var load: suspend (Int, Int, HomeHistoryDateRange?) -> Result<HomeHistoryPage> = { page, size, _ ->
        Result.success(HomeHistoryPage(emptyList(), 0, page, size))
    }
    override suspend fun calendar(range: HomeHistoryDateRange): Result<List<HomeHistoryDay>> {
        ranges += range
        return days
    }
    override suspend fun page(page: Int, pageSize: Int, range: HomeHistoryDateRange?): Result<HomeHistoryPage> {
        pages += page to range
        return load(page, pageSize, range)
    }
}

internal fun activityShop(id: String, stamp: String = "2026-10-25T10:00:00Z", slug: String? = "coffee") = PersonalCheckIn(
    id, slug?.let { HistoryAddress(it, "/shops/$it") }, "CoffeePeek", null, "", "", stamp, stamp, null, emptyList(), null,
    HistoryVisibility.Private, HistoryModeration.NotSubmitted, 0, null, 0, false,
)

internal fun activityHome(id: String, date: String = "2026-10-25", stamp: String = "${date}T09:00:00Z") = HomeHistoryEntry(
    id, date, stamp, stamp, null, HomeHistoryRecipe(null, null, null, null, null, null), null, null, null, emptyList(),
    HistoryVisibility.Private, HistoryModeration.NotSubmitted,
)
