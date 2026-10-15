package com.coffeepeek.feature.checkinhistory.domain.repository

import com.coffeepeek.feature.checkinhistory.domain.model.CheckInHistoryPage
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryPeriod
import com.coffeepeek.feature.checkinhistory.domain.model.PersonalCheckIn

/** Read-only personal shop visits; authentication/session lifetime belongs to composition. */
interface CheckInHistoryRepository {
    suspend fun page(page: Int, pageSize: Int): Result<CheckInHistoryPage>

    /** All pages in the period, with stable first-occurrence ID deduplication. */
    suspend fun period(period: HistoryPeriod): Result<List<PersonalCheckIn>>
}
