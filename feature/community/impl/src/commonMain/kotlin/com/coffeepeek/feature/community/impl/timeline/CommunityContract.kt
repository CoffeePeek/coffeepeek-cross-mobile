package com.coffeepeek.feature.community.impl.timeline

import com.coffeepeek.feature.community.api.FeedItemAction
import com.coffeepeek.feature.community.api.FeedTrigger
import com.coffeepeek.feature.community.domain.model.CommunityPost
import com.coffeepeek.feature.community.domain.model.FeedFilters
import com.coffeepeek.feature.community.domain.rules.canBlockAuthor
import com.coffeepeek.feature.community.domain.rules.ownsPost

internal enum class CommunityTimeline { Public, Mine }

internal enum class TimelineError { LoadFailed, BlocksFailed }

internal enum class MessageKind { VoteFailed }

/** A short notice for the snackbar; [serverMessage] is shown instead of the default text when the server sent one. */
internal data class ActionMessage(val kind: MessageKind, val serverMessage: String? = null)

internal data class CommunityState(
    /** Null until the first session answer. */
    val isLoggedIn: Boolean? = null,
    /** Bumped when the account or the timeline scope changes; late answers for an older generation are dropped. */
    val sessionGeneration: Long = 0,
    val items: List<CommunityPost> = emptyList(),
    val firstPageCount: Int? = null,
    val timeline: CommunityTimeline = CommunityTimeline.Public,
    val filters: FeedFilters = FeedFilters(),
    val nextCursor: String? = null,
    val publishedAt: Map<String, String> = emptyMap(),
    val ownAuthorSlug: String? = null,
    val page: Int = 0,
    val hasMore: Boolean = false,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: TimelineError? = null,
    val failedPage: Int? = null,
    val helpfulId: String? = null,
    /** The cursor is no longer valid: the retry starts from the first page. */
    val restartPagination: Boolean = false,
) {
    val isMutating: Boolean get() = helpfulId != null
    fun owns(post: CommunityPost): Boolean = ownsPost(post, isLoggedIn, timeline == CommunityTimeline.Mine, ownAuthorSlug)
    fun canBlockAuthor(post: CommunityPost): Boolean = canBlockAuthor(isLoggedIn, timeline == CommunityTimeline.Mine, ownAuthorSlug, owns(post))
}

internal sealed interface CommunityAction {
    data class Refresh(val trigger: FeedTrigger? = null) : CommunityAction
    data class LoadMore(val track: Boolean = true) : CommunityAction
    data object Retry : CommunityAction
    data class SelectTimeline(val timeline: CommunityTimeline) : CommunityAction
    data class SetFilters(val filters: FeedFilters) : CommunityAction
    data class ToggleHelpful(val id: String) : CommunityAction
    data class ItemInteracted(val action: FeedItemAction, val id: String) : CommunityAction
}

internal sealed interface CommunityEvent {
    /** The screen must open the sign-in destination. */
    data object SignInRequested : CommunityEvent
    data class Message(val message: ActionMessage) : CommunityEvent
}
