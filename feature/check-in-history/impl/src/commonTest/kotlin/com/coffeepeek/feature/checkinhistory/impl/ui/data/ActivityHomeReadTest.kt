package com.coffeepeek.feature.checkinhistory.impl.ui.data

import com.coffeepeek.feature.checkinhistory.domain.model.*
import com.coffeepeek.feature.checkinhistory.impl.ui.ActivityHomes
import com.coffeepeek.feature.checkinhistory.impl.ui.activityHome
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class ActivityHomeReadTest {
    private val range = HomeHistoryDateRange("2026-10-01", "2026-10-31")

    @Test fun everyPageIsReadAndDuplicateIdsKeepTheirFirstOccurrence() = runTest {
        val homes = ActivityHomes()
        homes.load = { page, size, _ ->
            val items = if (page == 1) (1..100).map { activityHome("$it") }
                else listOf(activityHome("100", "2026-10-20"), activityHome("101"))
            Result.success(HomeHistoryPage(items, 102, page, size))
        }
        val rows = readActivityHomeRows(homes, range)
        assertEquals(101, rows.size)
        assertEquals("2026-10-25", rows.single { it.id == "100" }.preparedOn)
        assertEquals(listOf(1, 2), homes.pages.map { it.first })
        assertTrue(homes.pages.all { it.second == range })
    }

    @Test fun entriesOutsideTheCivilRangeAreExcluded() = runTest {
        val homes = ActivityHomes()
        homes.load = { page, size, _ -> Result.success(HomeHistoryPage(
            listOf(activityHome("outside", "2026-11-01"), activityHome("inside")), 2, page, size)) }
        assertEquals(listOf("inside"), readActivityHomeRows(homes, range).map { it.id })
    }

    @Test fun emptyNonterminalPageCannotCertifyACompleteCalendar() = runTest {
        val homes = ActivityHomes()
        homes.load = { page, size, _ -> Result.success(HomeHistoryPage(emptyList(), 101, page, size)) }
        assertFailsWith<IllegalStateException> { readActivityHomeRows(homes, range) }
    }

    @Test fun unexpectedPageMetadataIsRejected() = runTest {
        val homes = ActivityHomes()
        homes.load = { _, size, _ -> Result.success(HomeHistoryPage(emptyList(), 0, 2, size)) }
        assertFailsWith<IllegalStateException> { readActivityHomeRows(homes, range) }
    }

    @Test fun resultCancellationIsRethrown() = runTest {
        val homes = ActivityHomes()
        homes.load = { _, _, _ -> Result.failure(CancellationException("cancelled")) }
        assertFailsWith<CancellationException> { readActivityHomeRows(homes, range) }
    }
}
