package com.coffeepeek.admin.di.notifications

import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.screen.contributions.ContributionKind
import com.coffeepeek.feature.contributions.domain.model.*
import com.coffeepeek.feature.contributions.domain.repository.ContributionsRepository
import com.coffeepeek.feature.notifications.api.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationNavigationTest {
    @Test fun correctionOpensTheCurrentShopInsteadOfTheRequestAsChosenByTheUser() = runTest {
        val feature = Feature().apply { target = { NotificationDestination.ShopChange("change") } }
        val contributions = Contributions()
        val opened = mutableListOf<Navigator.Screen>()
        val navigation = NotificationNavigation(feature, contributions, { "alice" }, opened::add, { error("unexpected") })
        assertTrue(navigation.open("notice", "alice", 4))
        assertEquals(listOf<Navigator.Screen>(Navigator.Screen.ShopDetail("current-cafe")), opened)
        assertEquals(listOf("change"), contributions.changeIds)
        assertEquals(listOf("notice"), feature.reads)
    }

    @Test fun approvedSubmissionFindsItsPublishedSlugOnTheNextPage() = runTest {
        val feature = Feature().apply { target = { NotificationDestination.ShopSubmission("submission") } }
        val contributions = Contributions().apply { pages = { _, page ->
            Result.success(Paged(if (page == 1) emptyList() else listOf(submission("published-cafe")), 101, 2, page))
        } }
        val opened = mutableListOf<Navigator.Screen>()
        val navigation = NotificationNavigation(feature, contributions, { "alice" }, opened::add, { error("unexpected") })
        assertTrue(navigation.open("notice", "alice", 4))
        assertEquals(listOf<Navigator.Screen>(Navigator.Screen.ShopDetail("published-cafe")), opened)
        assertEquals(listOf(ModerationStatus.Approved to 1, ModerationStatus.Approved to 2), contributions.pageCalls)
    }

    @Test fun anUnpublishedSubmissionOpensTheOwnersSubmissionsInsteadOfInventingAPublicShop() = runTest {
        val feature = Feature().apply { target = { NotificationDestination.ShopSubmission("submission") } }
        val contributions = Contributions().apply { pages = { status, page ->
            Result.success(Paged(if (status == ModerationStatus.Rejected) listOf(submission(null)) else emptyList(), 1, 1, page))
        } }
        val opened = mutableListOf<Navigator.Screen>()
        val navigation = NotificationNavigation(feature, contributions, { "alice" }, opened::add, { error("unexpected") })
        assertTrue(navigation.open("notice", "alice", 4))
        assertEquals(listOf<Navigator.Screen>(Navigator.Screen.MyContributions(ContributionKind.Shops.name)), opened)
    }

    @Test fun lateDestinationIsDiscardedAfterAccountReplacement() = runTest {
        val release = CompletableDeferred<Unit>()
        val feature = Feature().apply { target = {
            release.await()
            NotificationDestination.Shop("alice-cafe")
        } }
        val opened = mutableListOf<Navigator.Screen>()
        var user = "alice"
        var errors = 0
        val navigation = NotificationNavigation(feature, Contributions(), { user }, opened::add, { errors++ })
        val result = async { navigation.open("notice", "alice", 4) }
        runCurrent()
        user = "bob"
        feature.mutable.value = NotificationInboxState(accountId = "bob", generation = 5)
        release.complete(Unit)
        assertFalse(result.await())
        assertTrue(opened.isEmpty())
        assertTrue(feature.reads.isEmpty())
        assertEquals(0, errors)
    }

    @Test fun aSessionChangeDuringPaginationStopsBeforeReadingTheNextAccountsPage() = runTest {
        val feature = Feature().apply { target = { NotificationDestination.ShopSubmission("submission") } }
        var user = "alice"
        val contributions = Contributions().apply { pages = { _, _ ->
            user = "bob"
            Result.success(Paged(emptyList(), 200, 2, 1))
        } }
        val navigation = NotificationNavigation(feature, contributions, { user }, { error("must not navigate") }, { error("stale error") })
        assertFalse(navigation.open("notice", "alice", 4))
        assertEquals(1, contributions.pageCalls.size)
        assertTrue(feature.reads.isEmpty())
    }

    @Test fun failureKeepsTheTargetClosedAndCancellationIsRethrown() = runTest {
        val feature = Feature().apply { target = { error("offline") } }
        var errors = 0
        val navigation = NotificationNavigation(feature, Contributions(), { "alice" }, { error("must not navigate") }, { errors++ })
        assertFalse(navigation.open("notice", "alice", 4))
        assertEquals(1, errors)
        feature.target = { throw CancellationException("cancelled") }
        assertFailsWith<CancellationException> { navigation.open("notice", "alice", 4) }
        assertEquals(1, errors)
        assertTrue(feature.reads.isEmpty())
    }

    private class Feature : NotificationFeature {
        val mutable = MutableStateFlow(NotificationInboxState(accountId = "alice", generation = 4))
        override val state = mutable
        var target: suspend (String) -> NotificationDestination? = { NotificationDestination.Shop("current-cafe") }
        val reads = mutableListOf<String>()
        override suspend fun destination(notificationId: String) = target(notificationId)
        override fun markRead(id: String) { reads += id }
        override fun refresh() = Unit
        override fun loadMore() = Unit
        override fun setPushEnabled(enabled: Boolean) = Unit
        override fun markAllRead() = Unit
        override fun foreground() = Unit
        override fun onCheckInCreated() = Unit
        override suspend fun beforeLogout() = Unit
    }

    private class Contributions : ContributionsRepository {
        val changeIds = mutableListOf<String>()
        val pageCalls = mutableListOf<Pair<ModerationStatus, Int>>()
        var pages: suspend (ModerationStatus, Int) -> Result<Paged<ShopSubmission>> = { _, page -> Result.success(Paged(emptyList(), 0, 1, page)) }
        override suspend fun shops(status: ModerationStatus, page: Int, pageSize: Int): Result<Paged<ShopSubmission>> {
            pageCalls += status to page
            return pages(status, page)
        }
        override suspend fun change(id: String): Result<ChangeRequest> {
            changeIds += id
            return Result.success(ChangeRequest(id, "current-cafe", ChangeSection.Description, ChangePayload(),
                ModerationStatus.Approved, null, "2026-10-25T10:00:00Z", null))
        }
        override suspend fun roasters(status: ModerationStatus, page: Int, pageSize: Int): Result<Paged<RoasterSubmission>> = error("unused")
        override suspend fun changes(status: ModerationStatus?, page: Int, pageSize: Int, shopSlug: String?, section: ChangeSection?): Result<Paged<ChangeRequest>> = error("unused")
        override suspend fun submitShop(shop: NewShop): Result<Unit> = error("unused")
        override suspend fun submitRoaster(roaster: NewRoaster): Result<RoasterSubmitted> = error("unused")
        override suspend fun submitChange(draft: ChangeDraft): Result<ChangeRequest> = error("unused")
        override suspend fun editChange(id: String, draft: ChangeDraft): Result<ChangeRequest> = error("unused")
    }

    companion object {
        private fun submission(slug: String?) = ShopSubmission("submission", "Cafe", null, ModerationStatus.Approved, null, slug)
    }
}
