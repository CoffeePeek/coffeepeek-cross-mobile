package com.coffeepeek.admin.di.shop

import com.coffeepeek.feature.checkinhistory.domain.model.CheckInHistoryPage
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryModeration
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryPeriod
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryVisibility
import com.coffeepeek.feature.checkinhistory.domain.model.PersonalCheckIn
import com.coffeepeek.feature.checkinhistory.domain.repository.CheckInHistoryRepository
import com.coffeepeek.feature.shop.domain.model.ShopCheckInPublication
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class HistoryShopCheckInPublicationReaderTest {
    @Test fun exactIdControlsStatusEvenWhenOtherRowsArePublished() = runTest {
        val rows = listOf(record("other", HistoryModeration.Approved), record("created", HistoryModeration.Pending))
        assertEquals(ShopCheckInPublication.Pending, reader(Result.success(rows)).status("created").getOrThrow())
        assertEquals(ShopCheckInPublication.Unknown, reader(Result.success(rows)).status("absent").getOrThrow())
        assertEquals(ShopCheckInPublication.Published,
            reader(Result.success(listOf(record("created", HistoryModeration.Approved)))).status("created").getOrThrow())
    }

    @Test fun privateAndUnknownModerationNeverClaimPublication() = runTest {
        val private = record("created", HistoryModeration.Approved, HistoryVisibility.Private)
        assertEquals(ShopCheckInPublication.Unknown, reader(Result.success(listOf(private))).status("created").getOrThrow())
        val unknown = record("created", HistoryModeration.Unknown)
        assertEquals(ShopCheckInPublication.Unknown, reader(Result.success(listOf(unknown))).status("created").getOrThrow())
    }

    @Test fun readFailureIsAvailableForUnknownFallbackAndCancellationPropagates() = runTest {
        val failure = IllegalStateException("offline")
        assertEquals(failure, reader(Result.failure(failure)).status("created").exceptionOrNull())
        assertFailsWith<CancellationException> {
            reader(Result.failure(CancellationException("cancelled"))).status("created")
        }
    }

    private fun reader(result: Result<List<PersonalCheckIn>>) = HistoryShopCheckInPublicationReader(
        object : CheckInHistoryRepository {
            override suspend fun page(page: Int, pageSize: Int): Result<CheckInHistoryPage> =
                result.map { CheckInHistoryPage(it, it.size, page, pageSize) }
            override suspend fun period(period: HistoryPeriod) = Result.success(emptyList<PersonalCheckIn>())
        },
    )

    private fun record(id: String, moderation: HistoryModeration,
        visibility: HistoryVisibility = HistoryVisibility.Public) = PersonalCheckIn(
        id = id, shop = null, shopName = "Coffee", author = null, username = "", note = "",
        createdAt = "2026-11-13T10:00:00Z", visitedAt = "2026-11-13T10:00:00Z", rating = null,
        photoUrls = emptyList(), drink = null, visibility = visibility, moderation = moderation,
        contentRevision = 0, rejectionReason = null, helpfulCount = 0, isHelpfulByCurrentUser = false,
    )
}
