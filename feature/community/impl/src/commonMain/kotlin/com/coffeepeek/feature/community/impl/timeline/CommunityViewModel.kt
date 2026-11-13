package com.coffeepeek.feature.community.impl.timeline

import androidx.lifecycle.viewModelScope
import com.coffeepeek.core.presentation.MviViewModel
import com.coffeepeek.feature.authorblocking.api.AuthorVisibility
import com.coffeepeek.feature.authorblocking.api.AuthorVisibilitySnapshot
import com.coffeepeek.feature.community.api.CommunityAccount
import com.coffeepeek.feature.community.api.CommunityAnalytics
import com.coffeepeek.feature.community.api.ConsumedDrinks
import com.coffeepeek.feature.community.api.EditField
import com.coffeepeek.feature.community.api.FeedAction
import com.coffeepeek.feature.community.api.FeedItemAction
import com.coffeepeek.feature.community.api.FeedTrigger
import com.coffeepeek.feature.community.domain.model.CommunityFailure
import com.coffeepeek.feature.community.domain.model.CommunityPost
import com.coffeepeek.feature.community.domain.model.HelpfulVote
import com.coffeepeek.feature.community.domain.model.FeedFilters
import com.coffeepeek.feature.community.domain.model.FeedLoadException
import com.coffeepeek.feature.community.domain.model.PostVisibility
import com.coffeepeek.feature.community.domain.repository.CommunityRepository
import com.coffeepeek.feature.community.domain.rules.appendTimeline
import com.coffeepeek.feature.community.domain.rules.isWithdrawnFromPublic
import com.coffeepeek.feature.community.domain.rules.isVisibleInTimeline
import com.coffeepeek.feature.community.domain.rules.MAX_REPORT_LENGTH
import com.coffeepeek.feature.community.domain.rules.validateReport
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.CancellationException
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
 * The timeline as the previous screen kept it: one account at a time (a session change resets everything and drops late answers), the public feed in
 * the server's order with cursor paging, the personal timeline by page, author blocking applied on the way in, one mutation at a time.
 */
internal class CommunityViewModel(
    private val repository: CommunityRepository,
    private val account: CommunityAccount,
    private val analytics: CommunityAnalytics,
    private val drinks: ConsumedDrinks,
    private val authors: AuthorVisibility = AuthorVisibility.None,
    private val saved: com.coffeepeek.feature.community.api.CommunitySavedPosts = com.coffeepeek.feature.community.api.CommunitySavedPosts.None,
) : MviViewModel<CommunityState, CommunityAction, CommunityEvent>(CommunityState()) {
    private var firstResume = true
    private var accountKey: String? = null
    private var loadJob: Job? = null
    private var mutationJob: Job? = null
    private val voteJobs = mutableMapOf<String, Job>()
    private var profileJob: Job? = null
    private var drinksJob: Job? = null
    /** The snapshot this model's own refresh published; the load continues by itself, so that snapshot must not restart it a second time. */
    private var ownRefreshSnapshot: AuthorVisibilitySnapshot? = null

    init {
        viewModelScope.launch {
            saved.posts.collect { posts ->
                val own = posts.filter { it.accountKey == accountKey }.map { it.id }
                updateState { copy(confirmedSaves = own) }
                if (own.isNotEmpty()) refresh(null)
            }
        }
        viewModelScope.launch {
            authors.visibility.drop(1).collect { snapshot ->
                if (snapshot == ownRefreshSnapshot) { ownRefreshSnapshot = null; return@collect }
                if (currentState.timeline != CommunityTimeline.Public) return@collect
                loadJob?.cancel(); mutationJob?.cancel(); cancelVotes()
                updateState {
                    copy(items = emptyList(), publishedAt = emptyMap(), nextCursor = null, sessionGeneration = sessionGeneration + 1,
                        isLoading = false, isLoadingMore = false, helpfulIds = emptySet(), reportingId = null, isReporting = false, changingVisibilityId = null,
                        hasMore = false, page = 0)
                }
                if (snapshot.ready) load(reset = true)
            }
        }
        viewModelScope.launch {
            account.accountKey.distinctUntilChanged().collect { key ->
                loadJob?.cancel(); mutationJob?.cancel(); profileJob?.cancel(); drinksJob?.cancel(); cancelVotes()
                accountKey = key
                saved.retainAccount(key)
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
            CommunityAction.Resume -> if (firstResume) firstResume = false else refresh(null)
            CommunityAction.Retry -> retry()
            is CommunityAction.SelectTimeline -> selectTimeline(action.timeline)
            is CommunityAction.SetFilters -> setFilters(action.filters)
            is CommunityAction.ToggleHelpful -> toggleHelpful(action.id)
            is CommunityAction.ItemInteracted -> itemInteracted(action.action, currentState.items.firstOrNull { it.id == action.id })
            is CommunityAction.ToggleVisibility -> toggleVisibility(action.id)
            else -> handleEditorAction(action)
        }
    }

    private suspend fun handleEditorAction(action: CommunityAction) {
        when (action) {
            is CommunityAction.Edit -> edit(action.id)
            is CommunityAction.EditText -> updateEdit { copy(text = action.text) }
            is CommunityAction.EditRating -> updateEdit { copy(rating = action.rating) }
            is CommunityAction.EditDrink -> updateEdit { copy(drinkSlug = action.slug, customDrinkName = action.customName) }
            CommunityAction.LoadDrinks -> loadDrinks()
            CommunityAction.SaveEdit -> saveEdit()
            CommunityAction.DismissEdit -> dismissEdit()
            else -> handleReportAction(action)
        }
    }

    private suspend fun handleReportAction(action: CommunityAction) {
        when (action) {
            is CommunityAction.OpenReport -> openReport(action.id)
            is CommunityAction.ReportText -> if (!currentState.isReporting) {
                updateState { copy(reportText = action.text.take(MAX_REPORT_LENGTH), reportIssue = null, reportFailed = false, reportFailure = null) }
            }
            CommunityAction.SubmitReport -> submitReport()
            CommunityAction.DismissReport -> if (!currentState.isReporting) {
                updateState { copy(reportingId = null, reportText = "", reportIssue = null, reportFailed = false, reportFailure = null) }
            }
            else -> Unit
        }
    }

    private fun itemInteracted(action: FeedItemAction, post: CommunityPost?) {
        if (post != null && currentState.timeline == CommunityTimeline.Public) analytics.itemInteracted(action, post.isHome)
    }

    private suspend fun requestSignIn() = sendEvent(CommunityEvent.SignInRequested)

    private suspend fun selectTimeline(timeline: CommunityTimeline) {
        val current = currentState
        if (current.isMutating || current.editing != null || current.reportingId != null || current.timeline == timeline) return
        if (timeline == CommunityTimeline.Mine && current.isLoggedIn != true) { requestSignIn(); return }
        loadJob?.cancel()
        updateState { reset(timeline = timeline) }
        load(reset = true)
    }

    private fun setFilters(filters: FeedFilters) {
        val current = currentState
        if (current.isMutating || current.editing != null || current.reportingId != null || current.filters == filters) return
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
        if (current.isMutating || current.editing != null || current.reportingId != null) return
        loadJob?.cancel()
        updateState { copy(sessionGeneration = sessionGeneration + 1) }
        load(reset = true)
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

    private fun beginLoad(key: String?, generation: Long, reset: Boolean) {
        updateState {
            if (sessionGeneration != generation) this else copy(
                confirmedSaves = saved.posts.value.filter { it.accountKey == key }.map { it.id },
                isLoading = reset, isLoadingMore = !reset, error = null, failedPage = null, restartPagination = false,
                nextCursor = if (reset) null else nextCursor, page = if (reset) 0 else this.page, hasMore = if (reset) false else hasMore,
            )
        }
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
        val confirmed = saved.posts.value.filter { it.accountKey == key }
        beginLoad(key, generation, reset)
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
                repository.feed(PAGE_SIZE, cursor, filters.copy(includeOwnPending = true)).map { feed ->
                    TimelinePage(feed.entries.map { it.post }, feed.nextCursor != null, feed.nextCursor, feed.entries.associate { it.post.id to it.feedPositionAtUtc })
                }
            } else repository.myPosts(page, PAGE_SIZE).map { TimelinePage(it.posts, it.hasMore) }
            currentCoroutineContext().ensureActive()
            if (!isCurrent(key)) return@launch
            result.onSuccess { loaded ->
                if (currentState.sessionGeneration != generation) return@onSuccess
                if (reset && currentState.isUnfilteredFeed) saved.acknowledge(confirmed)
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
        if (!current.canLike || isWithdrawnFromPublic(item) || id in current.helpfulIds) return
        analytics.itemInteracted(if (item.isHelpfulByCurrentUser) FeedItemAction.Unlike else FeedItemAction.Like, item.isHome)
        val key = accountKey ?: run { analytics.likeGateShown(); requestSignIn(); return }
        if (!isCurrent(key)) return
        loadJob?.cancel()
        val generation = current.sessionGeneration
        updateState {
            copy(helpfulIds = helpfulIds + id, isLoading = false, isLoadingMore = false, items = items.map { post ->
                if (post.id == id) post.copy(isHelpfulByCurrentUser = !post.isHelpfulByCurrentUser,
                    helpfulCount = (post.helpfulCount + if (post.isHelpfulByCurrentUser) -1 else 1).coerceAtLeast(0)) else post
            })
        }
        val request = viewModelScope.launch(start = CoroutineStart.LAZY) { submitHelpful(item, key, generation) }
        voteJobs[id] = request
        request.start()
    }

    private suspend fun submitHelpful(original: CommunityPost, key: String, generation: Long) {
        try {
            val result = repository.setHelpful(original.id, !original.isHelpfulByCurrentUser)
            result.exceptionOrNull()?.let { if (it is CancellationException) throw it }
            currentCoroutineContext().ensureActive()
            if (!isCurrent(key) || generation != currentState.sessionGeneration) return
            finishHelpful(original, result.getOrNull())
            result.exceptionOrNull()?.let { reportVoteFailure(it) }
        } catch (cancelled: CancellationException) {
            if (isCurrent(key) && generation == currentState.sessionGeneration) finishHelpful(original, null)
            throw cancelled
        } catch (error: Exception) {
            if (isCurrent(key) && generation == currentState.sessionGeneration) {
                finishHelpful(original, null)
                reportVoteFailure(error)
            }
        } finally {
            if (voteJobs[original.id] === currentCoroutineContext()[Job]) voteJobs.remove(original.id)
        }
    }

    private fun finishHelpful(original: CommunityPost, vote: HelpfulVote?) = updateState {
        copy(helpfulIds = helpfulIds - original.id, items = items.map { post ->
            if (post.id == original.id) post.copy(helpfulCount = vote?.helpfulCount ?: original.helpfulCount,
                isHelpfulByCurrentUser = vote?.isHelpful ?: original.isHelpfulByCurrentUser) else post
        })
    }

    private suspend fun reportVoteFailure(error: Throwable) = sendEvent(
        CommunityEvent.Message(ActionMessage(MessageKind.VoteFailed, (error as? CommunityFailure)?.serverMessage)),
    )

    private fun cancelVotes() {
        voteJobs.values.toList().forEach { it.cancel() }
        voteJobs.clear()
    }

    private fun edit(id: String) {
        if (currentState.isMutating || currentState.reportingId != null) return
        val key = accountKey ?: return
        if (!isCurrent(key)) return
        val own = currentState.items.firstOrNull { it.id == id && currentState.owns(it) } ?: return
        analytics.editOpened()
        updateState { copy(editing = PostEditState(own)) }
        loadDrinks()
    }

    private fun updateEdit(change: PostEditState.() -> PostEditState) {
        val before = currentState.editing ?: return
        if (currentState.isSaving) return
        val after = before.change()
        if (before.text != after.text) analytics.editFieldChanged(EditField.Note)
        if (before.rating != after.rating) analytics.editFieldChanged(EditField.Rating)
        if (before.drinkSlug != after.drinkSlug || before.customDrinkName != after.customDrinkName) analytics.editFieldChanged(EditField.Drink)
        updateState { copy(editing = after.copy(error = null)) }
    }

    private fun dismissEdit() {
        if (currentState.isSaving) return
        analytics.editCancelled()
        updateState { copy(editing = null) }
    }

    private fun loadDrinks() {
        val key = accountKey ?: return
        drinksJob?.cancel()
        drinksJob = viewModelScope.launch {
            val result = drinks.load()
            currentCoroutineContext().ensureActive()
            if (!isCurrent(key)) return@launch
            result.onSuccess { options -> updateState { copy(drinks = options, drinksFailed = false) } }
                .onFailure { updateState { copy(drinksFailed = true) } }
        }
    }

    private fun saveEdit() {
        val edit = currentState.editing ?: return
        val key = accountKey ?: return
        if (!isCurrent(key) || currentState.isMutating) return
        analytics.editSubmitted()
        edit.issue()?.let { issue ->
            analytics.editRejected()
            updateState { copy(editing = edit.copy(error = EditError.Invalid(issue))) }
            return
        }
        submitEdit(edit, key)
    }

    private fun submitEdit(edit: PostEditState, key: String) {
        loadJob?.cancel()
        updateState { copy(isSaving = true, isLoading = false, isLoadingMore = false) }
        mutationJob = viewModelScope.launch {
            val result = repository.update(edit.source.id, edit.input())
            currentCoroutineContext().ensureActive()
            if (!isCurrent(key)) return@launch
            result.onSuccess { updated ->
                discardConfirmation(updated.id)
                analytics.editSaved(updated.visibility, updated.photos.originals.isNotEmpty())
                updateState { withUpdated(updated).copy(editing = null, isSaving = false) }
                sendEvent(CommunityEvent.Message(ActionMessage(MessageKind.EditSaved)))
            }.onFailure { error ->
                analytics.editFailed(error)
                updateState { copy(isSaving = false, editing = edit.copy(error = EditError.Server((error as? CommunityFailure)?.serverMessage))) }
            }
        }
    }

    private fun discardConfirmation(id: String) =
        saved.acknowledge(saved.posts.value.filter { it.accountKey == accountKey && it.id == id })

    private fun finishVisibility(updated: CommunityPost) {
        discardConfirmation(updated.id)
        updateState { withUpdated(updated).copy(changingVisibilityId = null) }
    }

    private fun toggleVisibility(id: String) {
        val key = accountKey ?: return
        val current = currentState
        if (!isCurrent(key) || current.isMutating || current.editing != null || current.reportingId != null) return
        val own = current.items.firstOrNull { it.id == id && current.owns(it) } ?: return
        val next = if (own.visibility == PostVisibility.Private) PostVisibility.Public else PostVisibility.Private
        loadJob?.cancel()
        updateState { copy(changingVisibilityId = id, isLoading = false, isLoadingMore = false) }
        mutationJob = viewModelScope.launch {
            val result = repository.setVisibility(id, next)
            currentCoroutineContext().ensureActive()
            if (!isCurrent(key)) return@launch
            result.onSuccess(::finishVisibility)
                .onFailure { error ->
                    updateState { copy(changingVisibilityId = null) }
                    sendEvent(CommunityEvent.Message(ActionMessage(MessageKind.VisibilityFailed, (error as? CommunityFailure)?.serverMessage)))
                }
        }
    }

    private suspend fun openReport(id: String) {
        val current = currentState
        if (current.timeline != CommunityTimeline.Public || current.isMutating || current.editing != null) return
        if (current.items.none { it.id == id && !current.owns(it) }) return
        if (accountKey == null) { requestSignIn(); return }
        updateState { copy(reportingId = id, reportText = "", reportIssue = null, reportFailed = false, reportFailure = null) }
    }

    private fun submitReport() {
        val current = currentState
        val id = current.reportingId ?: return
        val key = accountKey ?: return
        if (!isCurrent(key) || current.isMutating) return
        val text = current.reportText.trim()
        validateReport(text)?.let { issue -> updateState { copy(reportIssue = issue) }; return }
        updateState { copy(isReporting = true) }
        mutationJob = viewModelScope.launch {
            val result = repository.report(id, text)
            currentCoroutineContext().ensureActive()
            if (!isCurrent(key)) return@launch
            result.onSuccess {
                updateState { copy(isReporting = false, reportingId = null, reportText = "") }
                sendEvent(CommunityEvent.Message(ActionMessage(MessageKind.ReportSent)))
            }.onFailure { error ->
                updateState { copy(isReporting = false, reportFailed = true, reportFailure = (error as? CommunityFailure)?.serverMessage) }
            }
        }
    }

    /** An owner keeps their pending public post in place; rejected and private posts leave the feed. */
    private fun CommunityState.withUpdated(updated: CommunityPost): CommunityState {
        val withdrawn = timeline == CommunityTimeline.Public && !updated.isVisibleInTimeline(owns(updated))
        return copy(
            items = if (withdrawn) items.filterNot { it.id == updated.id } else items.map { if (it.id == updated.id) updated else it },
            publishedAt = if (withdrawn) publishedAt - updated.id else publishedAt,
        )
    }
}

private data class TimelinePage(
    val posts: List<CommunityPost>,
    val hasMore: Boolean,
    val cursor: String? = null,
    val publishedAt: Map<String, String> = emptyMap(),
)
