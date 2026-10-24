package com.coffeepeek.feature.contributions.impl.list

import androidx.lifecycle.viewModelScope
import com.coffeepeek.core.presentation.MviViewModel
import com.coffeepeek.feature.contributions.api.ContributionKind
import com.coffeepeek.feature.contributions.api.ContributionsAccount
import com.coffeepeek.feature.contributions.domain.model.ContributionFailure
import com.coffeepeek.feature.contributions.domain.model.FailureKind
import com.coffeepeek.feature.contributions.domain.model.ModerationStatus
import com.coffeepeek.feature.contributions.domain.model.Paged
import com.coffeepeek.feature.contributions.domain.repository.ContributionsRepository
import com.coffeepeek.feature.contributions.impl.summary
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

private const val PAGE_SIZE = 20

/**
 * The legacy list: the first page of all three statuses is loaded together (tab visibility depends on all of them), each tab pages on its own, one
 * page at a time. The new rule: one account at a time. A change of account clears the lists and drops late answers; a guest is asked to sign in.
 */
internal class ContributionsListViewModel(
    private val kind: ContributionKind,
    private val repository: ContributionsRepository,
    private val account: ContributionsAccount,
) : MviViewModel<ContributionsListState, ContributionsListAction, ContributionsListEvent>(ContributionsListState()) {
    private var key: String? = null
    private var generation = 0L
    private var loadJob: Job? = null
    private val moreJobs = mutableMapOf<ModerationStatus, Job>()

    init {
        viewModelScope.launch {
            account.accountKey.distinctUntilChanged().collect { accountKey ->
                key = accountKey
                if (accountKey == null) {
                    cancelWork()
                    updateState { ContributionsListState(isLoading = false, error = ListError.SignInRequired) }
                } else {
                    // Another account's lists must not stay visible while the new ones load.
                    cancelWork()
                    updateState { ContributionsListState() }
                    load()
                }
            }
        }
    }

    override suspend fun handleActionInternal(action: ContributionsListAction) {
        when (action) {
            ContributionsListAction.Refresh -> if (key != null) load()
            is ContributionsListAction.Select -> updateState { copy(selected = action.status) }
            is ContributionsListAction.LoadMore -> loadMore(action.status)
        }
    }

    private fun cancelWork() {
        generation++
        loadJob?.cancel()
        moreJobs.values.forEach(Job::cancel); moreJobs.clear()
    }

    private fun load() {
        cancelWork()
        val mine = generation
        val owner = key
        updateState { copy(isLoading = true, error = null, serverMessage = null) }
        loadJob = viewModelScope.launch {
            val results = coroutineScope { ContributionTabs.map { status -> async { status to fetch(status, 1) } }.awaitAll() }
            if (mine != generation || owner != account.currentAccountKey()) return@launch
            val failure = results.firstNotNullOfOrNull { it.second.exceptionOrNull() }
            if (failure != null) {
                updateState { copy(isLoading = false, error = failure.toListError(), serverMessage = (failure as? ContributionFailure)?.serverMessage) }
            } else {
                updateState {
                    ContributionsListState(
                        isLoading = false,
                        tabs = results.associate { (status, page) -> status to page.getOrThrow().toTab() },
                        selected = selected,
                    )
                }
            }
        }
    }

    private fun loadMore(status: ModerationStatus) {
        val tab = currentState.tabs[status] ?: return
        if (!tab.hasMore || tab.isLoadingMore) return
        updateTab(status) { copy(isLoadingMore = true) }
        val mine = generation
        moreJobs[status] = viewModelScope.launch {
            val result = fetch(status, tab.page + 1)
            if (mine != generation) return@launch
            result.onSuccess { page -> updateTab(status) { copy(items = items + page.items).withPage(page) } }
                .onFailure { updateTab(status) { copy(isLoadingMore = false) } }
        }
    }

    private fun updateTab(status: ModerationStatus, transform: TabState.() -> TabState) = updateState {
        val tab = tabs[status] ?: return@updateState this
        copy(tabs = tabs + (status to tab.transform()))
    }

    private suspend fun fetch(status: ModerationStatus, page: Int): Result<Paged<ContributionItem>> = when (kind) {
        ContributionKind.Shops -> repository.shops(status, page, PAGE_SIZE).map { paged ->
            paged.map { shop ->
                ContributionItem(
                    id = shop.id,
                    title = shop.name,
                    subtitle = shop.address,
                    // Approved but not in the catalog yet: nothing to open.
                    note = ContributionNote.WaitingForCatalog.takeIf { shop.status == ModerationStatus.Approved && shop.publishedShopSlug == null },
                    rejectedReason = shop.rejectedReason,
                    target = shop.publishedShopSlug?.let(ContributionTarget::Shop),
                )
            }
        }
        // The server gives no link to a published roaster.
        ContributionKind.Roasters -> repository.roasters(status, page, PAGE_SIZE).map { paged ->
            paged.map { ContributionItem(id = it.id, title = it.name, subtitle = it.about, rejectedReason = it.rejectedReason) }
        }
        ContributionKind.Changes -> repository.changes(status, page, PAGE_SIZE).map { paged ->
            paged.map { change ->
                ContributionItem(
                    id = change.id,
                    section = change.section,
                    summary = change.payload.summary(),
                    createdAtUtc = change.createdAtUtc,
                    rejectedReason = change.rejectionReason,
                    target = ContributionTarget.Request(change.id),
                )
            }
        }
    }
}

private fun <T, R> Paged<T>.map(transform: (T) -> R) = Paged(items.map(transform), totalCount, totalPages, currentPage)

private fun Paged<ContributionItem>.toTab() = TabState(items = items).withPage(this)

private fun TabState.withPage(page: Paged<ContributionItem>) = copy(totalCount = page.totalCount, page = page.currentPage, hasMore = page.hasMore, isLoadingMore = false)

private fun Throwable.toListError() = if ((this as? ContributionFailure)?.kind == FailureKind.SignInRequired) ListError.SignInRequired else ListError.LoadFailed
