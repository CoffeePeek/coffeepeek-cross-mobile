package com.coffeepeek.admin.feature.community.ui

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
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class CheckInEditState(
    val source: CheckIn,
    val text: String = source.note,
    val rating: ReviewRating = source.rating ?: ReviewRating(0, 0, 0),
    val drinkSlug: String? = source.drinkSlug,
    val customDrinkName: String? = source.customDrinkName,
    val error: String? = null,
) {
    fun input() = UpdateCheckInInput(text.trim(), rating, drinkSlug, customDrinkName?.trim())
    fun validationError() = validateCheckInContent(text, rating, drinkSlug, customDrinkName)
}

internal data class CommunityUiState(
    val isLoggedIn: Boolean? = null,
    val sessionGeneration: Long = 0,
    val items: List<CheckIn> = emptyList(),
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
)

internal class CommunityViewModel(
    private val checkIns: CheckInRepository,
    private val sessions: SessionRepository,
    private val shops: ShopRepository,
) : BaseViewModel() {
    private val _state = MutableStateFlow(CommunityUiState())
    val state = _state.asStateFlow()
    private var accountKey: String? = null
    private var loadJob: Job? = null
    private var mutationJob: Job? = null
    private var drinksJob: Job? = null

    init {
        workScope.launch {
            sessions.observeSession().map(::sessionKey).distinctUntilChanged().collect { key ->
                loadJob?.cancel()
                mutationJob?.cancel()
                drinksJob?.cancel()
                accountKey = key
                _state.value = CommunityUiState(
                    isLoggedIn = key != null,
                    isLoading = key != null,
                    sessionGeneration = _state.value.sessionGeneration + 1,
                )
                if (key != null) load(reset = true)
            }
        }
    }

    private fun sessionKey(session: Session?): String? =
        if (session != null && sessions.isActiveSession(session)) session.userId ?: session.accessToken else null

    private fun isCurrent(key: String) = accountKey == key && sessionKey(sessions.peekSession()) == key

    fun refresh() {
        val current = state.value
        if (!current.isLoading && !current.isSaving && current.changingVisibilityId == null) load(reset = true)
    }

    fun loadMore() {
        val current = state.value
        if (current.hasMore && !current.isLoading && !current.isLoadingMore && !current.isSaving && current.changingVisibilityId == null) {
            load(reset = false)
        }
    }

    fun retry() {
        if ((state.value.failedPage ?: 1) > 1) loadMore() else refresh()
    }

    private fun load(reset: Boolean) {
        val key = accountKey ?: return
        if (!isCurrent(key)) return
        val page = if (reset) 1 else state.value.page + 1
        loadJob?.cancel()
        _state.update { it.copy(isLoading = reset, isLoadingMore = !reset, error = null, failedPage = null) }
        loadJob = workScope.launch {
            val result = checkIns.getMyCheckIns(page, 20)
            currentCoroutineContext().ensureActive()
            if (!isCurrent(key)) return@launch
            result.onSuccess { resultPage ->
                _state.update {
                    it.copy(
                        items = (if (reset) resultPage.items else it.items + resultPage.items).distinctBy(CheckIn::id),
                        page = resultPage.currentPage,
                        hasMore = resultPage.currentPage < resultPage.totalPages,
                        isLoading = false, isLoadingMore = false, error = null,
                    )
                }
            }.onFailure { error ->
                _state.update { it.copy(isLoading = false, isLoadingMore = false, failedPage = page, error = error.message ?: "Не удалось загрузить чек-ины") }
            }
        }
    }

    fun edit(id: String) {
        if (state.value.isSaving || state.value.changingVisibilityId != null) return
        val key = accountKey ?: return
        if (!isCurrent(key)) return
        // /mine establishes ownership even when the author's public address is null.
        val own = state.value.items.firstOrNull { it.id == id } ?: return
        _state.update { it.copy(editing = CheckInEditState(own)) }
        loadDrinks()
    }

    fun updateEdit(value: CheckInEditState) {
        if (!state.value.isSaving && state.value.editing?.source?.id == value.source.id) {
            _state.update { it.copy(editing = value.copy(error = null)) }
        }
    }

    fun dismissEdit() {
        if (!state.value.isSaving) _state.update { it.copy(editing = null) }
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
        if (!isCurrent(key) || state.value.isSaving || state.value.changingVisibilityId != null) return
        edit.validationError()?.let { error ->
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
                _state.update {
                    it.copy(items = it.items.map { item -> if (item.id == updated.id) updated else item },
                        editing = null, isSaving = false, actionMessage = "Чек-ин сохранён")
                }
            }.onFailure { error ->
                _state.update { it.copy(isSaving = false, editing = edit.copy(error = error.message ?: "Не удалось сохранить чек-ин")) }
            }
        }
    }

    fun toggleVisibility(id: String) {
        val key = accountKey ?: return
        val current = state.value
        if (!isCurrent(key) || current.isSaving || current.changingVisibilityId != null || current.editing != null) return
        val own = current.items.firstOrNull { it.id == id } ?: return
        val next = if (own.visibility == CheckInVisibility.Private) CheckInVisibility.Public else CheckInVisibility.Private
        loadJob?.cancel()
        _state.update { it.copy(changingVisibilityId = id, isLoading = false, isLoadingMore = false) }
        mutationJob = workScope.launch {
            val result = checkIns.setVisibility(id, next)
            currentCoroutineContext().ensureActive()
            if (!isCurrent(key)) return@launch
            result.onSuccess { updated ->
                _state.update { it.copy(items = it.items.map { item -> if (item.id == updated.id) updated else item }, changingVisibilityId = null) }
            }.onFailure { error ->
                _state.update { it.copy(changingVisibilityId = null, actionMessage = error.message ?: "Не удалось изменить видимость") }
            }
        }
    }

    fun clearActionMessage() { _state.update { it.copy(actionMessage = null) } }
}
