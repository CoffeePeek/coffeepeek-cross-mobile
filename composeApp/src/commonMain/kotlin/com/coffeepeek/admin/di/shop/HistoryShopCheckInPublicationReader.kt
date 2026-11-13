package com.coffeepeek.admin.di.shop

import com.coffeepeek.feature.checkinhistory.domain.model.HistoryModeration
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryVisibility
import com.coffeepeek.feature.checkinhistory.domain.repository.CheckInHistoryRepository
import com.coffeepeek.feature.shop.domain.model.ShopCheckInPublication
import com.coffeepeek.feature.shop.domain.repository.ShopCheckInPublicationReader
import kotlinx.coroutines.CancellationException

/** Reads the exact new record from owner history; an absent or stale page stays unknown. */
internal class HistoryShopCheckInPublicationReader(
    private val history: CheckInHistoryRepository,
) : ShopCheckInPublicationReader {
    override suspend fun status(checkInId: String): Result<ShopCheckInPublication> {
        val result = history.page(1, 30)
        result.exceptionOrNull()?.let { if (it is CancellationException) throw it }
        return result.map { page ->
            val record = page.items.firstOrNull { it.id == checkInId }
            when {
                record?.visibility != HistoryVisibility.Public -> ShopCheckInPublication.Unknown
                record.moderation == HistoryModeration.Pending -> ShopCheckInPublication.Pending
                record.moderation == HistoryModeration.Approved -> ShopCheckInPublication.Published
                else -> ShopCheckInPublication.Unknown
            }
        }
    }
}
