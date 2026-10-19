package com.coffeepeek.admin.feature.community.ui

import com.coffeepeek.admin.analytics.*

import com.coffeepeek.admin.base.BaseViewModel
import com.coffeepeek.domain.model.CheckIn
import com.coffeepeek.domain.model.CheckInVisibility
import com.coffeepeek.domain.model.ConsumedDrinkOption
import com.coffeepeek.domain.model.ReviewRating
import com.coffeepeek.domain.model.Session
import com.coffeepeek.domain.model.UpdateCheckInInput
import com.coffeepeek.domain.model.validateCheckInContent
import com.coffeepeek.domain.repository.CheckInRepository
import com.coffeepeek.domain.repository.SessionRepository
import com.coffeepeek.domain.repository.ShopRepository
import com.coffeepeek.domain.repository.UserRepository
import com.coffeepeek.domain.model.CheckInModerationState
import com.coffeepeek.domain.feature.feed.FeedFilters
import com.coffeepeek.domain.feature.feed.FeedLoadException
import com.coffeepeek.domain.feature.feed.FeedRepository
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import com.coffeepeek.feature.authorblocking.api.AuthorVisibility
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch

internal data class CheckInEditState(
    val source: CheckIn,
    val text: String = source.note,
    val rating: ReviewRating = source.rating ?: ReviewRating(0, 0, 0),
    val drinkSlug: String? = source.drinkSlug,
    val customDrinkName: String? = source.customDrinkName,
    val error: String? = null,
) {
    fun input() = UpdateCheckInInput(text.trim(), rating.takeUnless { it == ReviewRating(0, 0, 0) }, drinkSlug, customDrinkName?.trim())
    fun validationError() = validateCheckInContent(text, input().rating, drinkSlug, customDrinkName)
}

internal data class CommunityUiState(
    val isLoggedIn: Boolean? = null,
    val sessionGeneration: Long = 0,
    val items: List<CheckIn> = emptyList(),
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
    val error: String? = null,
    val failedPage: Int? = null,
    val editing: CheckInEditState? = null,
    val isSaving: Boolean = false,
    val changingVisibilityId: String? = null,
    val drinks: List<ConsumedDrinkOption> = emptyList(),
    val drinksError: String? = null,
    val actionMessage: String? = null,
    val needsLogin: Boolean = false,
    val helpfulId: String? = null,
    val reportingId: String? = null,
    val reportText: String = "",
    val reportError: String? = null,
    val isReporting: Boolean = false,
    val restartPagination: Boolean = false,
) {
    fun owns(checkIn: CheckIn): Boolean = isLoggedIn == true && (
        timeline == CommunityTimeline.Mine ||
            (ownAuthorSlug != null && checkIn.authorAddress?.slug == ownAuthorSlug)
        )
    // Public author IDs differ from authentication user IDs. Wait for the canonical
    // profile slug before offering a signed-in viewer an author-blocking action.
    fun canBlockAuthor(checkIn: CheckIn): Boolean = timeline == CommunityTimeline.Public && when (isLoggedIn) {
        false -> true // A guest may open the existing sign-in prompt.
        true -> ownAuthorSlug != null && !owns(checkIn)
        null -> false
    }
    val isMutating: Boolean get() = isSaving || changingVisibilityId != null || helpfulId != null || isReporting
}

internal enum class CommunityTimeline { Public, Mine }

internal class CommunityViewModel(
    private val checkIns: CheckInRepository,
    private val sessions: SessionRepository,
    private val shops: ShopRepository,
    private val feed: FeedRepository,
    private val users: UserRepository,
    private val visibility: AuthorVisibility = AuthorVisibility.None,
) : BaseViewModel() {
    private val _state = MutableStateFlow(CommunityUiState())
    val state = _state.asStateFlow()
    private var accountKey: String? = null
    private var editAnalytics: CheckInTracker? = null

    fun itemAction(action: String, item: CheckIn) {
        if (state.value.timeline == CommunityTimeline.Public) Analytics.track(AnalyticsEvent.FEED_ITEM_INTERACTED, "action" to action, "check_in_kind" to if (item.isHome) "home" else "shop")
    }
    private var loadJob: Job? = null
    private var mutationJob: Job? = null
    private var drinksJob: Job? = null
    private var profileJob: Job? = null
    private var hasResumed: Boolean = false

    init {
        workScope.launch {
            visibility.visibility.drop(1).collect { snapshot ->
                if (state.value.timeline == CommunityTimeline.Public) {
                    loadJob?.cancel(); mutationJob?.cancel()
                    _state.update { it.copy(items = emptyList(), publishedAt = emptyMap(), nextCursor = null,
                        sessionGeneration = it.sessionGeneration + 1, isLoading = false, isLoadingMore = false,
                        helpfulId = null, reportingId = null, isReporting = false, changingVisibilityId = null,
                        hasMore = false, page = 0) }
                    if (snapshot.ready) load(reset = true)
                }
            }
        }
        workScope.launch {
            sessions.observeSession().map(::sessionKey).distinctUntilChanged().collect { key ->
                loadJob?.cancel()
                mutationJob?.cancel()
                drinksJob?.cancel()
                profileJob?.cancel()
                accountKey = key
                val initial = _state.updateAndGet { current ->
                    CommunityUiState(
                        isLoggedIn = key != null,
                        isLoading = true,
                        timeline = current.timeline.takeIf { key != null } ?: CommunityTimeline.Public,
                        filters = current.filters,
                        sessionGeneration = current.sessionGeneration + 1,
                    )
                }
                load(reset = true, expectedGeneration = initial.sessionGeneration)
                if (key != null) profileJob = workScope.launch {
                    val result = users.refreshProfile()
                    currentCoroutineContext().ensureActive()
                    if (isCurrent(key)) result.onSuccess { profile ->
                        _state.update { it.copy(ownAuthorSlug = profile.address?.slug?.takeIf(String::isNotBlank)) }
                    }
                }
            }
        }
    }

    private fun sessionKey(session: Session?): String? =
        if (session != null && sessions.isActiveSession(session)) session.userId ?: session.accessToken else null

    private fun isCurrent(key: String?) = state.value.isLoggedIn != null && accountKey == key && sessionKey(sessions.peekSession()) == key

    fun selectTimeline(timeline: CommunityTimeline) {
        val current = state.value
        if (current.isMutating || current.editing != null || current.reportingId != null || current.timeline == timeline) return
        if (timeline == CommunityTimeline.Mine && current.isLoggedIn != true) {
            requestLogin(); return
        }
        loadJob?.cancel()
        _state.update { it.copy(timeline = timeline, items = emptyList(), publishedAt = emptyMap(), page = 0,
            nextCursor = null, hasMore = false, error = null, sessionGeneration = it.sessionGeneration + 1) }
        load(reset = true)
    }

    fun setFilters(filters: FeedFilters) {
        val current = state.value
        if (current.isMutating || current.editing != null || current.reportingId != null || current.filters == filters) return
        loadJob?.cancel()
        _state.update { it.copy(filters = filters, items = emptyList(), publishedAt = emptyMap(), page = 0,
            nextCursor = null, hasMore = false, error = null, sessionGeneration = it.sessionGeneration + 1) }
        load(reset = true)
    }

    fun onScreenResumed() {
        // Session observation owns the initial load, even if it finishes before the first resume.
        if (hasResumed) refresh() else hasResumed = true
    }

    fun refresh(trigger: String? = null) {
        if (trigger != null && state.value.timeline == CommunityTimeline.Public) Analytics.track(AnalyticsEvent.FEED_ACTION_CLICKED, "action" to "refresh", "trigger" to trigger)
        val current = state.value
        if (!current.isLoading && !current.isMutating) load(reset = true)
    }

    fun loadMore(trackAction: Boolean = true) {
        val current = state.value
        if (current.hasMore && !current.isLoading && !current.isLoadingMore && !current.isMutating) {
            if (trackAction && current.timeline == CommunityTimeline.Public) Analytics.track(AnalyticsEvent.FEED_ACTION_CLICKED, "action" to "load_more", "trigger" to "scroll")
            load(reset = false)
        }
    }

    fun retry() {
        if (state.value.timeline == CommunityTimeline.Public) Analytics.track(AnalyticsEvent.FEED_ACTION_CLICKED, "action" to "retry", "trigger" to "button")
        if (!state.value.restartPagination && (state.value.failedPage ?: 1) > 1) loadMore(trackAction = false) else refresh()
    }

    private fun load(reset: Boolean, expectedGeneration: Long? = null) {
        val key = accountKey
        val current = state.value
        val generation = current.sessionGeneration
        if (expectedGeneration != null && generation != expectedGeneration) return
        if (!isCurrent(key)) return
        val timeline = current.timeline
        if (timeline == CommunityTimeline.Mine && key == null) return
        val page = if (reset) 1 else current.page + 1
        val cursor = if (reset) null else current.nextCursor
        val filters = current.filters
        if (state.value.sessionGeneration != generation) return
        _state.update {
            if (it.sessionGeneration != generation) it else it.copy(
                isLoading = reset, isLoadingMore = !reset, error = null, failedPage = null, restartPagination = false,
                nextCursor = if (reset) null else it.nextCursor, page = if (reset) 0 else it.page,
                hasMore = if (reset) false else it.hasMore,
            )
        }
        val request = workScope.launch(start = CoroutineStart.LAZY) {
            if (!isCurrent(key) || state.value.sessionGeneration != generation) return@launch
            if (timeline == CommunityTimeline.Public && !visibility.visibility.value.ready) {
                if (visibility.refresh().isFailure) {
                    if (state.value.sessionGeneration == generation) _state.update { it.copy(isLoading = false, isLoadingMore = false, error = "Не удалось загрузить блокировки. Повторите попытку.") }
                    return@launch
                }
            }
            val result = if (timeline == CommunityTimeline.Public) {
                feed.getFeed(20, cursor, filters).map { feedPage ->
                    TimelinePage(feedPage.items.map { it.checkIn }, feedPage.nextCursor != null, feedPage.nextCursor,
                        feedPage.items.associate { it.checkIn.id to it.publishedAtUtc })
                }
            } else checkIns.getMyCheckIns(page, 20).map { TimelinePage(it.items, it.currentPage < it.totalPages) }
            currentCoroutineContext().ensureActive()
            if (!isCurrent(key)) return@launch
            result.onSuccess { loadedPage ->
                val resultPage = if (timeline == CommunityTimeline.Public) loadedPage.copy(items = loadedPage.items.filterNot {
                    visibility.visibility.value.hides(it.authorId, it.authorAddress?.slug)
                }) else loadedPage
                _state.update {
                    if (it.sessionGeneration != generation || !isCurrent(key)) it else it.copy(
                        items = if (reset) resultPage.items.distinctBy(CheckIn::id) else appendTimeline(it.items, resultPage.items),
                        firstPageCount = if (reset) resultPage.items.size else it.firstPageCount,
                        page = page,
                        hasMore = resultPage.hasMore,
                        nextCursor = resultPage.cursor,
                        publishedAt = (if (reset) emptyMap() else it.publishedAt) + resultPage.publishedAt,
                        isLoading = false, isLoadingMore = false, error = null,
                    )
                }
            }.onFailure { error ->
                _state.update {
                    if (it.sessionGeneration != generation || !isCurrent(key)) it else it.copy(
                        isLoading = false, isLoadingMore = false, failedPage = page,
                        restartPagination = (error as? FeedLoadException)?.restartPagination == true,
                        error = "Не удалось загрузить ленту",
                    )
                }
            }
        }
        // Store the job before its response can trigger a filter/timeline change.
        loadJob = request
        request.start()
    }

    fun edit(id: String) {
        if (state.value.isMutating || state.value.reportingId != null) return
        val key = accountKey ?: return
        if (!isCurrent(key)) return
        val own = state.value.items.firstOrNull { it.id == id && state.value.owns(it) } ?: return
        editAnalytics = CheckInTracker(CheckInKind.Visit, CheckInSource.Diary, edit = true).also { it.step(CheckInStep.Editor) }
        _state.update { it.copy(editing = CheckInEditState(own)) }
        loadDrinks()
    }

    fun updateEdit(value: CheckInEditState) {
        if (!state.value.isSaving && state.value.editing?.source?.id == value.source.id) {
            val before = state.value.editing
            if (before != null) listOf("note" to (before.text != value.text), "rating" to (before.rating != value.rating),
                "drink" to (before.drinkSlug != value.drinkSlug || before.customDrinkName != value.customDrinkName))
                .filter { it.second }.forEach { editAnalytics?.field(it.first) }
            _state.update { it.copy(editing = value.copy(error = null)) }
        }
    }

    fun dismissEdit() {
        if (!state.value.isSaving) { editAnalytics?.cancel("close"); _state.update { it.copy(editing = null) } }
    }

    fun loadDrinks() {
        val key = accountKey ?: return
        drinksJob?.cancel()
        drinksJob = workScope.launch {
            val result = shops.getConsumedDrinks()
            currentCoroutineContext().ensureActive()
            if (!isCurrent(key)) return@launch
            result.onSuccess { drinks -> _state.update { it.copy(drinks = drinks, drinksError = null) } }
                .onFailure { _state.update { it.copy(drinksError = "Не удалось загрузить напитки") } }
        }
    }

    fun saveEdit() {
        val edit = state.value.editing ?: return
        val key = accountKey ?: return
        if (!isCurrent(key) || state.value.isMutating) return
        val tracker = editAnalytics
        tracker?.submit()
        edit.validationError()?.let { error ->
            tracker?.validation(listOf("form"))
            _state.update { it.copy(editing = edit.copy(error = error)) }
            return
        }
        loadJob?.cancel()
        _state.update { it.copy(isSaving = true, isLoading = false, isLoadingMore = false) }
        mutationJob = workScope.launch {
            val result = checkIns.updateCheckIn(edit.source.id, edit.input())
            currentCoroutineContext().ensureActive()
            if (!isCurrent(key)) return@launch
            result.onSuccess { updated ->
                tracker?.saved(updated.visibility.name, updated.photoUrls.isNotEmpty())
                _state.update {
                    it.withUpdated(updated).copy(editing = null, isSaving = false,
                        actionMessage = if (it.timeline == CommunityTimeline.Public) "Чек-ин сохранён и отправлен на проверку. Он доступен в профиле, в разделе «Чекины»" else "Чек-ин сохранён")
                }
            }.onFailure { error ->
                tracker?.failed(CheckInStage.Save, safeAnalyticsError(error))
                _state.update { it.copy(isSaving = false, editing = edit.copy(error = error.message ?: "Не удалось сохранить чек-ин")) }
            }
        }
    }

    fun toggleVisibility(id: String) {
        val key = accountKey ?: return
        val current = state.value
        if (!isCurrent(key) || current.isMutating || current.editing != null || current.reportingId != null) return
        val own = current.items.firstOrNull { it.id == id && current.owns(it) } ?: return
        val next = if (own.visibility == CheckInVisibility.Private) CheckInVisibility.Public else CheckInVisibility.Private
        loadJob?.cancel()
        _state.update { it.copy(changingVisibilityId = id, isLoading = false, isLoadingMore = false) }
        mutationJob = workScope.launch {
            val result = checkIns.setVisibility(id, next)
            currentCoroutineContext().ensureActive()
            if (!isCurrent(key)) return@launch
            result.onSuccess { updated ->
                _state.update { it.withUpdated(updated).copy(changingVisibilityId = null) }
            }.onFailure { error ->
                _state.update { it.copy(changingVisibilityId = null, actionMessage = error.message ?: "Не удалось изменить видимость") }
            }
        }
    }

    fun clearActionMessage() { _state.update { it.copy(actionMessage = null) } }

    fun requestLogin() { _state.update { it.copy(needsLogin = true) } }
    fun loginHandled() { _state.update { it.copy(needsLogin = false) } }

    fun toggleHelpful(id: String) {
        val current = state.value
        val item = current.items.firstOrNull { it.id == id } ?: return
        if (current.timeline != CommunityTimeline.Public || current.isMutating || current.editing != null || current.reportingId != null || current.owns(item)) return
        itemAction(if (item.isHelpfulByCurrentUser) "unlike" else "like", item)
        val key = accountKey ?: run { Analytics.gate("feed_like"); requestLogin(); return }
        if (!isCurrent(key)) return
        loadJob?.cancel()
        _state.update { it.copy(helpfulId = id, isLoading = false, isLoadingMore = false) }
        mutationJob = workScope.launch {
            val result = checkIns.setHelpful(id, !item.isHelpfulByCurrentUser)
            currentCoroutineContext().ensureActive()
            if (!isCurrent(key)) return@launch
            result.onSuccess { vote ->
                _state.update { it.copy(helpfulId = null, items = it.items.map { post ->
                    if (post.id == id) post.copy(helpfulCount = vote.helpfulCount, isHelpfulByCurrentUser = vote.isHelpful) else post
                }) }
            }.onFailure { error -> _state.update { it.copy(helpfulId = null, actionMessage = error.message ?: "Не удалось изменить отметку") } }
        }
    }

    fun openReport(id: String) {
        val current = state.value
        if (current.timeline != CommunityTimeline.Public || current.isMutating || current.editing != null) return
        if (current.items.none { it.id == id && !current.owns(it) }) return
        if (accountKey == null) { requestLogin(); return }
        _state.update { it.copy(reportingId = id, reportText = "", reportError = null) }
    }

    fun updateReport(text: String) {
        if (!state.value.isReporting) _state.update { it.copy(reportText = text.take(2000), reportError = null) }
    }

    fun dismissReport() {
        if (!state.value.isReporting) _state.update { it.copy(reportingId = null, reportText = "", reportError = null) }
    }

    fun submitReport() {
        val current = state.value
        val id = current.reportingId ?: return
        val key = accountKey ?: return
        if (!isCurrent(key) || current.isMutating) return
        val text = current.reportText.trim()
        if (text.length !in 1..2000) {
            _state.update { it.copy(reportError = "Опишите проблему: от 1 до 2000 символов") }; return
        }
        _state.update { it.copy(isReporting = true) }
        mutationJob = workScope.launch {
            val result = checkIns.report(id, text)
            currentCoroutineContext().ensureActive()
            if (!isCurrent(key)) return@launch
            result.onSuccess { _state.update { it.copy(isReporting = false, reportingId = null, reportText = "", actionMessage = "Жалоба отправлена") } }
                .onFailure { error -> _state.update { it.copy(isReporting = false, reportError = error.message ?: "Не удалось отправить жалобу") } }
        }
    }
}

private data class TimelinePage(val items: List<CheckIn>, val hasMore: Boolean, val cursor: String? = null, val publishedAt: Map<String, String> = emptyMap())

// Keep the server's publication order; update overlapping live items without moving them.
private fun appendTimeline(current: List<CheckIn>, incoming: List<CheckIn>): List<CheckIn> {
    val updates = incoming.associateBy(CheckIn::id)
    val known = current.mapTo(mutableSetOf(), CheckIn::id)
    return current.map { updates[it.id] ?: it } + incoming.filter { known.add(it.id) }
}

private fun CommunityUiState.withUpdated(updated: CheckIn): CommunityUiState {
    val withdrawn = timeline == CommunityTimeline.Public && (
        updated.visibility != CheckInVisibility.Public || updated.moderationState != CheckInModerationState.Approved
        )
    return copy(items = if (withdrawn) items.filterNot { it.id == updated.id } else items.map { if (it.id == updated.id) updated else it },
        publishedAt = if (withdrawn) publishedAt - updated.id else publishedAt)
}
