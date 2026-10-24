package com.coffeepeek.feature.contributions.impl.list

import com.coffeepeek.feature.contributions.domain.model.ChangeSection
import com.coffeepeek.feature.contributions.domain.model.ModerationStatus
import com.coffeepeek.feature.contributions.impl.ChangeSummary

/** Tab order on screen. */
internal val ContributionTabs = listOf(ModerationStatus.Approved, ModerationStatus.Pending, ModerationStatus.Rejected)

/** Where a card leads. */
internal sealed interface ContributionTarget {
    data class Shop(val slug: String) : ContributionTarget
    data class Request(val id: String) : ContributionTarget
}

internal enum class ContributionNote { WaitingForCatalog }

/** One card. Shops and roasters bring their own [title]; a change request brings its [section], [summary] and [createdAtUtc]. */
internal data class ContributionItem(
    val id: String,
    val title: String? = null,
    val subtitle: String? = null,
    val note: ContributionNote? = null,
    val section: ChangeSection? = null,
    val summary: ChangeSummary? = null,
    val createdAtUtc: String? = null,
    val rejectedReason: String? = null,
    val target: ContributionTarget? = null,
)

internal data class TabState(
    val items: List<ContributionItem> = emptyList(),
    val totalCount: Int = 0,
    val page: Int = 0,
    val hasMore: Boolean = false,
    val isLoadingMore: Boolean = false,
)

internal enum class ListError { SignInRequired, LoadFailed }

internal data class ContributionsListState(
    val isLoading: Boolean = true,
    val error: ListError? = null,
    /** The server's own wording of a failed load, shown instead of the default text. */
    val serverMessage: String? = null,
    val tabs: Map<ModerationStatus, TabState> = emptyMap(),
    val selected: ModerationStatus? = null,
) {
    /** Only statuses that have items get a tab. */
    val visibleTabs: List<ModerationStatus> get() = ContributionTabs.filter { tabs[it]?.items?.isNotEmpty() == true }

    /** The chosen tab if it still has items, else the first one. */
    val selectedTab: ModerationStatus? get() = visibleTabs.firstOrNull { it == selected } ?: visibleTabs.firstOrNull()
}

internal sealed interface ContributionsListAction {
    data object Refresh : ContributionsListAction
    data class Select(val status: ModerationStatus) : ContributionsListAction
    data class LoadMore(val status: ModerationStatus) : ContributionsListAction
}

internal sealed interface ContributionsListEvent
