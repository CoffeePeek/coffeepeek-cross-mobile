package com.coffeepeek.admin.di.discovery

import com.coffeepeek.feature.checkinhistory.domain.model.CheckInHistoryPage
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryAddress
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryModeration
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryPeriod
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryVisibility
import com.coffeepeek.feature.checkinhistory.domain.model.PersonalCheckIn
import com.coffeepeek.feature.checkinhistory.domain.repository.CheckInHistoryRepository
import com.coffeepeek.feature.shop.domain.model.ShopDetails
import com.coffeepeek.feature.shop.domain.repository.ShopDetailsRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class RecentCheckInShopsTest {
    @Test
    fun recentPlacesUseVisitTimeAndKeepOneEntryPerShopEvenWhenDetailsFail() = runTest {
        val visits = listOf(visit("first", "room", "Room", "2026-11-10T10:00:00Z"),
            visit("second", "other", "Other", "2026-11-12T10:00:00Z"),
            visit("third", "room", "Room", "2026-11-11T10:00:00Z"))
        val history = object : CheckInHistoryRepository {
            override suspend fun page(page: Int, pageSize: Int) =
                Result.success(CheckInHistoryPage(visits, visits.size, page, pageSize))
            override suspend fun period(period: HistoryPeriod) = Result.success(emptyList<PersonalCheckIn>())
        }
        val details = object : ShopDetailsRepository {
            override suspend fun getDetails(shopId: String): Result<ShopDetails> =
                Result.failure(IllegalStateException("Offline"))
        }

        val recent = RecentCheckInShops(history, details).load().getOrThrow()

        assertEquals(listOf("other", "room"), recent.map { it.id })
        assertEquals(listOf("Other", "Room"), recent.map { it.title })
    }

    private fun visit(id: String, slug: String, name: String, date: String) = PersonalCheckIn(
        id = id, shop = HistoryAddress(slug, "/shops/$slug"), shopName = name,
        author = null, username = "", note = "", createdAt = date, visitedAt = date,
        rating = null, photoUrls = emptyList(), drink = null,
        visibility = HistoryVisibility.Private, moderation = HistoryModeration.Approved,
        contentRevision = 0, rejectionReason = null, helpfulCount = 0, isHelpfulByCurrentUser = false,
    )
}
