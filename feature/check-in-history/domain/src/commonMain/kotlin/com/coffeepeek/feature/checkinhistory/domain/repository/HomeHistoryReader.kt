package com.coffeepeek.feature.checkinhistory.domain.repository

import com.coffeepeek.feature.checkinhistory.domain.model.HomeHistoryDateRange
import com.coffeepeek.feature.checkinhistory.domain.model.HomeHistoryDay
import com.coffeepeek.feature.checkinhistory.domain.model.HomeHistoryPage

/** Own read port for combined history. Home writers and editor navigation stay caller-owned. */
interface HomeHistoryReader {
    suspend fun page(page: Int, pageSize: Int, range: HomeHistoryDateRange?): Result<HomeHistoryPage>
    suspend fun calendar(range: HomeHistoryDateRange): Result<List<HomeHistoryDay>>
}
