package com.coffeepeek.feature.contributions.impl

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.contributions.api.ContributionKind
import com.coffeepeek.feature.contributions.domain.model.*
import com.coffeepeek.feature.contributions.impl.list.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class ContributionsListViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { store.clear(); Dispatchers.resetMain() }

    private fun TestScope.model(kind: ContributionKind, repository: FakeRepository, account: FakeAccount = FakeAccount()) =
        ContributionsListViewModel(kind, repository, account).also { store.put("list", it); runCurrent() }

    private fun ContributionsListViewModel.send(action: ContributionsListAction, scope: TestScope) { onAction(action); scope.runCurrent() }

    @Test fun aGuestIsAskedToSignInWithoutAnyRequest() = runTest(dispatcher) {
        val repository = FakeRepository()
        val vm = model(ContributionKind.Shops, repository, FakeAccount(null))
        assertEquals(ListError.SignInRequired, vm.state.value.error); assertFalse(vm.state.value.isLoading)
        vm.send(ContributionsListAction.Refresh, this)
        assertTrue(repository.requests.isEmpty())
    }

    @Test fun shopsAreMappedWithTheirTargetNoteAndReasonAndOnlyStatusesWithItemsGetATab() = runTest(dispatcher) {
        val repository = FakeRepository().apply {
            shops = { status, _ ->
                when (status) {
                    ModerationStatus.Approved -> pageOf(shop("a", published = "cafe"), shop("b"))
                    ModerationStatus.Pending -> pageOf()
                    ModerationStatus.Rejected -> pageOf(shop("c", ModerationStatus.Rejected, reason = "Нет адреса"))
                }
            }
        }
        val state = model(ContributionKind.Shops, repository).state.value
        assertEquals(listOf(ModerationStatus.Approved, ModerationStatus.Rejected), state.visibleTabs)
        val approved = state.tabs.getValue(ModerationStatus.Approved).items
        assertEquals(ContributionTarget.Shop("cafe"), approved[0].target); assertNull(approved[0].note)
        assertNull(approved[1].target); assertEquals(ContributionNote.WaitingForCatalog, approved[1].note)
        assertEquals("Street a", approved[0].subtitle); assertEquals("Shop a", approved[0].title)
        assertEquals("Нет адреса", state.tabs.getValue(ModerationStatus.Rejected).items.single().rejectedReason)
        assertNull(state.tabs.getValue(ModerationStatus.Rejected).items.single().note)
        assertEquals(setOf("shops:Approved:1", "shops:Pending:1", "shops:Rejected:1"), repository.requests.toSet())
        assertFalse(state.isLoading); assertNull(state.error)
    }

    @Test fun roastersHaveNoTargetAndChangesOpenTheirRequest() = runTest(dispatcher) {
        val repository = FakeRepository().apply {
            roasters = { status, _ -> if (status == ModerationStatus.Approved) pageOf(RoasterSubmission("r", "Roast", "О нас", ModerationStatus.Approved, null)) else pageOf() }
            changes = { status, _ ->
                if (status == ModerationStatus.Pending) pageOf(request("c1", ChangeSection.Tags, ChangePayload(tagIds = listOf("a", "b")))) else pageOf()
            }
        }
        val roaster = model(ContributionKind.Roasters, repository).state.value.tabs.getValue(ModerationStatus.Approved).items.single()
        assertEquals(ContributionItem(id = "r", title = "Roast", subtitle = "О нас"), roaster)
        val change = model(ContributionKind.Changes, repository).state.value.tabs.getValue(ModerationStatus.Pending).items.single()
        assertEquals(ContributionTarget.Request("c1"), change.target)
        assertEquals(ChangeSection.Tags, change.section); assertEquals(ChangeSummary.Selected(ChangeSection.Tags, 2), change.summary)
        assertEquals("2026-10-21T07:00:00Z", change.createdAtUtc); assertNull(change.title)
    }

    @Test fun oneFailingStatusFailsTheWholeListWithTheServerMessage() = runTest(dispatcher) {
        val repository = FakeRepository().apply {
            shops = { status, _ -> if (status == ModerationStatus.Pending) Result.failure(ContributionFailure(FailureKind.Other, "Сервер недоступен")) else pageOf(shop("a")) }
        }
        val state = model(ContributionKind.Shops, repository).state.value
        assertEquals(ListError.LoadFailed, state.error); assertEquals("Сервер недоступен", state.serverMessage)
        assertTrue(state.tabs.isEmpty()); assertFalse(state.isLoading)

        val unauthorised = FakeRepository().apply { shops = { _, _ -> Result.failure(ContributionFailure(FailureKind.SignInRequired)) } }
        assertEquals(ListError.SignInRequired, model(ContributionKind.Shops, unauthorised).state.value.error)
        val transport = FakeRepository().apply { shops = { _, _ -> Result.failure(IllegalStateException("socket closed")) } }
        val state2 = model(ContributionKind.Shops, transport).state.value
        assertEquals(ListError.LoadFailed, state2.error); assertNull(state2.serverMessage)
    }

    @Test fun refreshReloadsAfterAFailureAndClearsTheError() = runTest(dispatcher) {
        var fail = true
        val repository = FakeRepository().apply { shops = { _, _ -> if (fail) Result.failure(IllegalStateException()) else pageOf(shop("a")) } }
        val vm = model(ContributionKind.Shops, repository)
        assertEquals(ListError.LoadFailed, vm.state.value.error)
        fail = false
        vm.send(ContributionsListAction.Refresh, this)
        assertNull(vm.state.value.error); assertEquals(listOf(ModerationStatus.Approved, ModerationStatus.Pending, ModerationStatus.Rejected), vm.state.value.visibleTabs)
    }

    @Test fun loadMoreAppendsOnePageAtATimeAndAFailureKeepsTheLoadedItems() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        var secondPageRequests = 0
        val repository = FakeRepository().apply {
            shops = { status, page ->
                when {
                    status != ModerationStatus.Approved -> pageOf()
                    page == 1 -> pageOf(shop("1"), total = 3, pages = 3, current = 1)
                    page == 2 -> { secondPageRequests++; gate.await(); pageOf(shop("2"), total = 3, pages = 3, current = 2) }
                    else -> Result.failure(IllegalStateException("fail"))
                }
            }
        }
        val vm = model(ContributionKind.Shops, repository)
        vm.send(ContributionsListAction.LoadMore(ModerationStatus.Pending), this)
        assertNull(vm.state.value.tabs[ModerationStatus.Pending]?.items?.firstOrNull())
        vm.send(ContributionsListAction.LoadMore(ModerationStatus.Approved), this)
        assertTrue(vm.state.value.tabs.getValue(ModerationStatus.Approved).isLoadingMore)
        repeat(5) { vm.send(ContributionsListAction.LoadMore(ModerationStatus.Approved), this) }
        gate.complete(Unit); runCurrent()
        assertEquals(1, secondPageRequests)
        val approved = vm.state.value.tabs.getValue(ModerationStatus.Approved)
        assertEquals(listOf("1", "2"), approved.items.map { it.id }); assertTrue(approved.hasMore); assertEquals(3, approved.totalCount)
        vm.send(ContributionsListAction.LoadMore(ModerationStatus.Approved), this)
        val failed = vm.state.value.tabs.getValue(ModerationStatus.Approved)
        assertFalse(failed.isLoadingMore); assertEquals(listOf("1", "2"), failed.items.map { it.id }); assertNull(vm.state.value.error)
    }

    @Test fun theLastPageDisablesLoadMore() = runTest(dispatcher) {
        val repository = FakeRepository().apply { shops = { status, _ -> if (status == ModerationStatus.Approved) pageOf(shop("1")) else pageOf() } }
        val vm = model(ContributionKind.Shops, repository)
        assertFalse(vm.state.value.tabs.getValue(ModerationStatus.Approved).hasMore)
        vm.send(ContributionsListAction.LoadMore(ModerationStatus.Approved), this)
        assertEquals(3, repository.requests.size)
    }

    @Test fun theSelectedTabFallsBackToTheFirstOneWithItems() = runTest(dispatcher) {
        val repository = FakeRepository().apply {
            shops = { status, _ -> if (status == ModerationStatus.Pending || status == ModerationStatus.Rejected) pageOf(shop("a", status)) else pageOf() }
        }
        val vm = model(ContributionKind.Shops, repository)
        assertEquals(ModerationStatus.Pending, vm.state.value.selectedTab)
        vm.send(ContributionsListAction.Select(ModerationStatus.Rejected), this)
        assertEquals(ModerationStatus.Rejected, vm.state.value.selectedTab)
        vm.send(ContributionsListAction.Select(ModerationStatus.Approved), this)
        assertEquals(ModerationStatus.Pending, vm.state.value.selectedTab)
    }

    @Test fun aNewAccountClearsTheListsAndAnOldAnswerIsDropped() = runTest(dispatcher) {
        val slow = CompletableDeferred<Unit>()
        val repository = FakeRepository().apply { shops = { _, _ -> pageOf(shop("mine")) } }
        val account = FakeAccount("one")
        val vm = model(ContributionKind.Shops, repository, account)
        assertEquals("mine", vm.state.value.tabs.getValue(ModerationStatus.Approved).items.single().id)

        repository.shops = { status, _ -> if (status == ModerationStatus.Approved) { slow.await(); pageOf(shop("late")) } else pageOf() }
        account.key.value = "two"; runCurrent()
        assertTrue(vm.state.value.isLoading); assertTrue(vm.state.value.tabs.isEmpty())
        repository.shops = { _, _ -> pageOf(shop("other")) }
        account.key.value = "three"; runCurrent()
        slow.complete(Unit); runCurrent()
        assertEquals("other", vm.state.value.tabs.getValue(ModerationStatus.Approved).items.single().id)

        account.key.value = null; runCurrent()
        assertEquals(ListError.SignInRequired, vm.state.value.error); assertTrue(vm.state.value.tabs.isEmpty())
    }
}
