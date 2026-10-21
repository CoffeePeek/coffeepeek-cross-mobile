package com.coffeepeek.feature.community.impl.timeline

import androidx.lifecycle.viewModelScope
import com.coffeepeek.core.presentation.MviViewModel
import com.coffeepeek.feature.authorblocking.api.AuthorVisibility
import com.coffeepeek.feature.authorblocking.api.AuthorVisibilitySnapshot
import com.coffeepeek.feature.community.api.CommunityAccount
import com.coffeepeek.feature.community.api.CommunityAnalytics
import com.coffeepeek.feature.community.api.FeedAction
import com.coffeepeek.feature.community.api.FeedItemAction
import com.coffeepeek.feature.community.api.FeedTrigger
import com.coffeepeek.feature.community.domain.model.CommunityFailure
import com.coffeepeek.feature.community.domain.model.CommunityPost
import com.coffeepeek.feature.community.domain.model.FeedFilters
import com.coffeepeek.feature.community.domain.model.FeedLoadException
import com.coffeepeek.feature.community.domain.repository.CommunityRepository
import com.coffeepeek.feature.community.domain.rules.appendTimeline
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch

private const val PAGE_SIZE = 20

/**
 * The timeline as the legacy screen kept it: one account at a time (a session change resets everything and drops late answers), the public feed in
 * the server's order with cursor paging, the personal timeline by page, author blocking applied on the way in, one mutation at a time.
 */
internal class CommunityViewModel(
    private val repository: CommunityRepository,
    private val account: CommunityAccount,
    private val analytics: CommunityAnalytics,
    private val authors: AuthorVisibility = AuthorVisibility.None,
) : MviViewModel<CommunityState, CommunityAction, CommunityEvent>(CommunityState()) {
    private var accountKey: String? = null
    private var loadJob: Job? = null
    private var mutationJob: Job? = null
    private var profileJob: Job? = null
    /** The snapshot this model's own refresh published; the load continues by itself, so that snapshot must not restart it a second time. */
    private var ownRefreshSnapshot: AuthorVisibilitySnapshot? = null

    init {
        viewModelScope.launch {
            authors.visibility.drop(1).collect { snapshot ->
                if (snapshot == ownRefreshSnapshot) { ownRefreshSnapshot = null; return@collect }
                if (currentState.timeline != CommunityTimeline.Public) return@collect
                loadJob?.cancel(); mutationJob?.cancel()
                updateState {
                    copy(items = emptyList(), publishedAt = emptyMap(), nextCursor = null, sessionGeneration = sessionGeneration + 1,
                        isLoading = false, isLoadingMore = false, helpfulId = null, hasMore = false, page = 0)
                }
                if (snapshot.ready) load(reset = true)
            }
        }
        viewModelScope.launch {
            account.accountKey.distinctUntilChanged().collect { key ->
                loadJob?.cancel(); mutationJob?.cancel(); profileJob?.cancel()
                accountKey = key
                val initial = updateAndGet {
                    CommunityState(
                        isLoggedIn = key != null,
                        isLoading = true,
                        timeline = timeline.takeIf { key != null } ?: CommunityTimeline.Public,
                        filters = filters,
                        sessionGeneration = sessionGeneration + 1,
                    )
                }
                load(reset = true, expectedGeneration = initial.sessionGeneration)
                if (key != null) profileJob = viewModelScope.launch {
                    val result = account.ownAuthorSlug()
                    currentCoroutineContext().ensureActive()
                    if (isCurrent(key)) result.onSuccess { slug -> updateState { copy(ownAuthorSlug = slug?.takeIf(String::isNotBlank)) } }
                }
            }
        }
    }

    private fun updateAndGet(transform: CommunityState.() -> CommunityState): CommunityState {
        updateState(transform)
        return currentState
    }

    private fun isCurrent(key: String?) =
        currentState.isLoggedIn != null && accountKey == key && account.currentAccountKey() == key

    override suspend fun handleActionInternal(action: CommunityAction) {
        when (action) {
            is CommunityAction.Refresh -> refresh(action.trigger)
            is CommunityAction.LoadMore -> loadMore(action.track)
            CommunityAction.Retry -> retry()
            is CommunityAction.SelectTimeline -> selectTimeline(action.timeline)
            is CommunityAction.SetFilters -> setFilters(action.filters)
            is CommunityAction.ToggleHelpful -> toggleHelpful(action.id)
            is CommunityAction.ItemInteracted -> itemInteracted(action.action, currentState.items.firstOrNull { it.id == action.id })
        }
    }

    private fun itemInteracted(action: FeedItemAction, post: CommunityPost?) {
        if (post != null && currentState.timeline == CommunityTimeline.Public) analytics.itemInteracted(action, post.isHome)
    }

    private suspend fun requestSignIn() = sendEvent(CommunityEvent.SignInRequested)

    private suspend fun selectTimeline(timeline: CommunityTimeline) {
        val current = currentState
        if (current.isMutating || current.timeline == timeline) return
        if (timeline == CommunityTimeline.Mine && current.isLoggedIn != true) { requestSignIn(); return }
        loadJob?.cancel()
        updateState { reset(timeline = timeline) }
        load(reset = true)
    }

    private fun setFilters(filters: FeedFilters) {
        val current = currentState
        if (current.isMutating || current.filters == filters) return
        loadJob?.cancel()
        updateState { reset(filters = filters) }
        load(reset = true)
    }

    private fun CommunityState.reset(timeline: CommunityTimeline = this.timeline, filters: FeedFilters = this.filters) = copy(
        timeline = timeline, filters = filters, items = emptyList(), publishedAt = emptyMap(), page = 0, nextCursor = null,
        hasMore = false, error = null, sessionGeneration = sessionGeneration + 1,
    )

    private fun refresh(trigger: FeedTrigger?) {
        if (trigger != null && currentState.timeline == CommunityTimeline.Public) analytics.feedAction(FeedAction.Refresh, trigger)
        val current = currentState
        if (!current.isLoading && !current.isMutating) load(reset = true)
    }

    private fun loadMore(track: Boolean) {
        val current = currentState
        if (current.hasMore && !current.isLoading && !current.isLoadingMore && !current.isMutating) {
            if (track && current.timeline == CommunityTimeline.Public) analytics.feedAction(FeedAction.LoadMore, FeedTrigger.Scroll)
            load(reset = false)
        }
    }

    private fun retry() {
        if (currentState.timeline == CommunityTimeline.Public) analytics.feedAction(FeedAction.Retry, FeedTrigger.Button)
        if (!currentState.restartPagination && (currentState.failedPage ?: 1) > 1) loadMore(track = false) else refresh(null)
    }

    private fun load(reset: Boolean, expectedGeneration: Long? = null) {
        val key = accountKey
        val current = currentState
        val generation = current.sessionGeneration
        if (expectedGeneration != null && generation != expectedGeneration) return
        if (!isCurrent(key)) return
        val timeline = current.timeline
        if (timeline == CommunityTimeline.Mine && key == null) return
        val page = if (reset) 1 else current.page + 1
        val cursor = if (reset) null else current.nextCursor
        val filters = current.filters
        updateState {
            if (sessionGeneration != generation) this else copy(
                isLoading = reset, isLoadingMore = !reset, error = null, failedPage = null, restartPagination = false,
                nextCursor = if (reset) null else nextCursor, page = if (reset) 0 else this.page, hasMore = if (reset) false else hasMore,
            )
        }
        val request = viewModelScope.launch(start = CoroutineStart.LAZY) {
            if (!isCurrent(key) || currentState.sessionGeneration != generation) return@launch
            if (timeline == CommunityTimeline.Public && !authors.visibility.value.ready) {
                val refreshed = authors.refresh()
                if (refreshed.isSuccess) ownRefreshSnapshot = authors.visibility.value
                if (refreshed.isFailure) {
                    if (currentState.sessionGeneration == generation) updateState { copy(isLoading = false, isLoadingMore = false, error = TimelineError.BlocksFailed) }
                    return@launch
                }
            }
            val result = if (timeline == CommunityTimeline.Public) {
                repository.feed(PAGE_SIZE, cursor, filters).map { feed ->
                    TimelinePage(feed.entries.map { it.post }, feed.nextCursor != null, feed.nextCursor, feed.entries.associate { it.post.id to it.publishedAtUtc })
                }
            } else repository.myPosts(page, PAGE_SIZE).map { TimelinePage(it.posts, it.hasMore) }
            currentCoroutineContext().ensureActive()
            if (!isCurrent(key)) return@launch
            result.onSuccess { loaded ->
                val visible = if (timeline == CommunityTimeline.Public) loaded.copy(posts = loaded.posts.filterNot {
                    authors.visibility.value.hides(it.author.id, it.author.slug)
                }) else loaded
                updateState {
                    if (sessionGeneration != generation || !isCurrent(key)) this else copy(
                        items = if (reset) visible.posts.distinctBy(CommunityPost::id) else appendTimeline(items, visible.posts),
                        firstPageCount = if (reset) visible.posts.size else firstPageCount,
                        page = page, hasMore = visible.hasMore, nextCursor = visible.cursor,
                        publishedAt = (if (reset) emptyMap() else publishedAt) + visible.publishedAt,
                        isLoading = false, isLoadingMore = false, error = null,
                    )
                }
            }.onFailure { error ->
                updateState {
                    if (sessionGeneration != generation || !isCurrent(key)) this else copy(
                        isLoading = false, isLoadingMore = false, failedPage = page,
                        restartPagination = (error as? FeedLoadException)?.restartPagination == true,
                        error = TimelineError.LoadFailed,
                    )
                }
            }
        }
        // Stored before it can answer, so a filter or timeline change triggered by its response cancels the right job.
        loadJob = request
        request.start()
    }

    private suspend fun toggleHelpful(id: String) {
        val current = currentState
        val item = current.items.firstOrNull { it.id == id } ?: return
        if (current.timeline != CommunityTimeline.Public || current.isMutating || current.owns(item)) return
        analytics.itemInteracted(if (item.isHelpfulByCurrentUser) FeedItemAction.Unlike else FeedItemAction.Like, item.isHome)
        val key = accountKey ?: run { analytics.likeGateShown(); requestSignIn(); return }
        if (!isCurrent(key)) return
        loadJob?.cancel()
        updateState { copy(helpfulId = id, isLoading = false, isLoadingMore = false) }
        mutationJob = viewModelScope.launch {
            val result = repository.setHelpful(id, !item.isHelpfulByCurrentUser)
            currentCoroutineContext().ensureActive()
            if (!isCurrent(key)) return@launch
            result.onSuccess { vote ->
                updateState {
                    copy(helpfulId = null, items = items.map { post ->
                        if (post.id == id) post.copy(helpfulCount = vote.helpfulCount, isHelpfulByCurrentUser = vote.isHelpful) else post
                    })
                }
            }.onFailure { error ->
                updateState { copy(helpfulId = null) }
                sendEvent(CommunityEvent.Message(ActionMessage(MessageKind.VoteFailed, (error as? CommunityFailure)?.serverMessage)))
            }
        }
    }
}

private data class TimelinePage(
    val posts: List<CommunityPost>,
    val hasMore: Boolean,
    val cursor: String? = null,
    val publishedAt: Map<String, String> = emptyMap(),
)
