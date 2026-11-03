package com.coffeepeek.admin.ui.screen.contributions

import com.coffeepeek.admin.base.BaseViewModel
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.utils.utcIsoToLocalDateTime
import com.coffeepeek.domain.model.ModerationStatus
import com.coffeepeek.domain.model.PagedResult
import com.coffeepeek.domain.model.ShopChangeSection
import com.coffeepeek.domain.model.ShopSubmission
import com.coffeepeek.domain.model.RoasterSubmission
import com.coffeepeek.domain.model.ShopChangeRequest
import com.coffeepeek.domain.repository.RoasterRepository
import com.coffeepeek.domain.repository.ShopChangeRequestRepository
import com.coffeepeek.domain.repository.ShopRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val PAGE_SIZE = 20

enum class ContributionKind { Shops, Roasters, Changes }

/** Tab order on screen. */
val CONTRIBUTION_TABS = listOf(ModerationStatus.Approved, ModerationStatus.Pending, ModerationStatus.Rejected)

data class ContributionItem(
    val id: String,
    val title: String = "",
    val subtitle: String? = null,
    val meta: String? = null,
    val changeSection: ShopChangeSection? = null,
    val changeSummary: ContributionSummary? = null,
    val awaitingCatalogPublication: Boolean = false,
    val rejectedReason: String? = null,
    val target: Navigator.Screen? = null,
)

data class ContributionTab(
    val items: List<ContributionItem> = emptyList(),
    val totalCount: Int = 0,
    val page: Int = 0,
    val hasMore: Boolean = false,
    val isLoadingMore: Boolean = false,
)

data class MyContributionsUiState(
    val isLoading: Boolean = true,
    val error: ContributionLoadError? = null,
    val tabs: Map<ModerationStatus, ContributionTab> = emptyMap(),
) {
    /** Only statuses that actually have items get a tab. */
    val visibleTabs: List<ModerationStatus>
        get() = CONTRIBUTION_TABS.filter { tabs[it]?.items?.isNotEmpty() == true }
}

class MyContributionsViewModel(
    private val kind: ContributionKind,
    private val shopRepository: ShopRepository,
    private val roasterRepository: RoasterRepository,
    private val changeRepository: ShopChangeRequestRepository,
) : BaseViewModel() {

    private val _state = MutableStateFlow(MyContributionsUiState())
    val state = _state.asStateFlow()

    init { refresh() }

    /** Loads the first page of every status at once: tab visibility depends on all three. */
    fun refresh() {
        workScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val results = coroutineScope {
                CONTRIBUTION_TABS.map { status -> async { status to fetch(status, page = 1) } }.awaitAll()
            }
            val failure = results.firstNotNullOfOrNull { it.second.exceptionOrNull() }
            if (failure != null) {
                _state.update { it.copy(isLoading = false, error = ContributionLoadError(failure.message)) }
                return@launch
            }
            _state.value = MyContributionsUiState(
                isLoading = false,
                tabs = results.associate { (status, result) -> status to result.getOrThrow().toTab() },
            )
        }
    }

    fun loadMore(status: ModerationStatus) {
        val tab = _state.value.tabs[status] ?: return
        if (!tab.hasMore || tab.isLoadingMore) return
        updateTab(status) { it.copy(isLoadingMore = true) }
        workScope.launch {
            fetch(status, tab.page + 1)
                .onSuccess { page ->
                    updateTab(status) { it.copy(items = it.items + page.items).withPage(page) }
                }
                .onFailure { updateTab(status) { it.copy(isLoadingMore = false) } }
        }
    }

    private fun updateTab(status: ModerationStatus, transform: (ContributionTab) -> ContributionTab) {
        _state.update { state ->
            val tab = state.tabs[status] ?: return@update state
            state.copy(tabs = state.tabs + (status to transform(tab)))
        }
    }

    private suspend fun fetch(status: ModerationStatus, page: Int): Result<PagedResult<ContributionItem>> = when (kind) {
        ContributionKind.Shops -> shopRepository.getMyShopSubmissions(status, page, PAGE_SIZE).mapItems { it.contributionItem() }
        ContributionKind.Roasters -> roasterRepository.getMyRoasterSubmissions(status, page, PAGE_SIZE).mapItems { it.contributionItem() }
        ContributionKind.Changes -> changeRepository.getMine(page, PAGE_SIZE, status = status).mapItems { it.contributionItem() }
    }

}

private fun ShopSubmission.contributionItem() = ContributionItem(
    id = id, title = name, subtitle = address,
    awaitingCatalogPublication = status == ModerationStatus.Approved && publishedShopId == null,
    rejectedReason = rejectedReason,
    target = publishedShopId?.let { Navigator.Screen.ShopDetail(it) },
)

private fun RoasterSubmission.contributionItem() = ContributionItem(
    id = id, title = name, subtitle = about, rejectedReason = rejectedReason,
)

private fun ShopChangeRequest.contributionItem() = ContributionItem(
    id = id, changeSummary = payload.contributionSummary(), meta = utcIsoToLocalDateTime(createdAtUtc),
    changeSection = section, rejectedReason = rejectionReason,
    target = Navigator.Screen.ShopChangeRequestDetail(requestId = id),
)

private fun <T> Result<PagedResult<T>>.mapItems(transform: (T) -> ContributionItem) =
    map { PagedResult(it.items.map(transform), it.totalCount, it.totalPages, it.currentPage) }

private fun PagedResult<ContributionItem>.toTab() = ContributionTab(items = items).withPage(this)

private fun ContributionTab.withPage(page: PagedResult<ContributionItem>) = copy(
    totalCount = page.totalCount,
    page = page.currentPage,
    hasMore = page.currentPage < page.totalPages,
    isLoadingMore = false,
)
