package com.coffeepeek.feature.checkinhistory.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CheckInHistoryPageTest {
    @Test fun usesTotalCountNotVisibleItemCountForPagination() {
        val page = CheckInHistoryPage(emptyList(), totalCount = 41, page = 2, pageSize = 20)
        assertEquals(3, page.totalPages)
        assertTrue(page.hasMore)
        assertFalse(page.copy(page = 3).hasMore)
        assertEquals(0, page.copy(totalCount = 0).totalPages)
    }

    @Test fun paginationDoesNotOverflowIntArithmetic() {
        val page = CheckInHistoryPage(emptyList(), Int.MAX_VALUE, Int.MAX_VALUE, 100)
        assertEquals(21474837, page.totalPages)
        assertFalse(page.hasMore)
    }
}
