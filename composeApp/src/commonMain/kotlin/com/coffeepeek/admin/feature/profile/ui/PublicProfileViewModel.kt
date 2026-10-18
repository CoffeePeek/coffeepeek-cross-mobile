package com.coffeepeek.admin.feature.profile.ui

import com.coffeepeek.admin.feature.authorblocking.api.AuthorVisibility
import kotlinx.coroutines.flow.drop
import androidx.lifecycle.ViewModel
import com.coffeepeek.admin.feature.profile.domain.*
import com.coffeepeek.domain.model.Session
import com.coffeepeek.domain.repository.SessionRepository
import com.coffeepeek.domain.repository.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.ExperimentalTime

internal data class PublicProfileUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val profile: PublicProfile? = null,
    val checkIns: List<PublicProfileCheckIn> = emptyList(),
    val isLoadingCheckIns: Boolean = false,
    val isLoadingMoreCheckIns: Boolean = false,
    val checkInsError: String? = null,
    val hasMoreCheckIns: Boolean = false,
    val isOwnProfile: Boolean = false,
    val isFollowing: Boolean = false,
    val isFollowLoading: Boolean = false,
    val followError: String? = null,
    val needsLogin: Boolean = false,
    val isLoadingFavorites: Boolean = false,
    val favoritesError: String? = null,
    val isLoadingActivity: Boolean = false,
    val activityError: String? = null,
    val activityMonth: String = currentActivityMonth(),
    val connectionKind: PublicProfileConnectionKind? = null,
    val connections: List<PublicProfileConnection> = emptyList(),
    val isLoadingConnections: Boolean = false,
    val isLoadingMoreConnections: Boolean = false,
    val connectionsError: String? = null,
    val hasMoreConnections: Boolean = false,
)

internal class PublicProfileViewModel(
    private val slug: String,
    private val repository: PublicProfileRepository,
    private val sessions: SessionRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
    private val users: UserRepository? = null,
    private val visibility: AuthorVisibility = AuthorVisibility.None,
) : ViewModel(), AutoCloseable {
    private val _state = MutableStateFlow(PublicProfileUiState())
    val state = _state.asStateFlow()
    val authorVisibility = visibility.visibility
    private var profileJob: Job? = null
    private var checkInsJob: Job? = null
    private var followJob: Job? = null
    private var favoritesJob: Job? = null
    private var activityJob: Job? = null
    private var connectionsJob: Job? = null
    private var ownProfileJob: Job? = null
    private var nextCursor: String? = null
    private var connectionsPage = 0
    private var generation = 0
    private var activityGeneration = 0
    private var connectionsGeneration = 0
    private var account = sessionKey(sessions.peekSession())
    private var ownProfileAccount = account
    private var ownIdentityLoaded = users == null || users.observeProfile().value != null
    private var ownAddress = users?.observeProfile()?.value?.address.takeIf { account != null }

    init {
        addCloseable(this)
        scope.launch {
            visibility.visibility.drop(1).collect {
                _state.value = PublicProfileUiState(activityMonth = state.value.activityMonth)
                refresh()
            }
        }
        refresh()
        observeOwnProfile()
        loadOwnProfile(forceRefresh = false)
        scope.launch {
            sessions.observeSession().map(::sessionKey).distinctUntilChanged().collect { current ->
                if (current != account) {
                    account = current
                    ownProfileAccount = null
                    ownIdentityLoaded = users == null
                    ownAddress = null
                    _state.value = PublicProfileUiState(activityMonth = state.value.activityMonth)
                    refresh()
                    loadOwnProfile(forceRefresh = true)
                }
            }
        }
    }

    private fun sessionKey(session: Session?): String? = session?.takeIf(sessions::isActiveSession)?.let { it.userId ?: it.accessToken }
    private fun isCurrent(requestGeneration: Int, requestAccount: String?): Boolean =
        generation == requestGeneration && account == requestAccount && sessionKey(sessions.peekSession()) == requestAccount

    private fun observeOwnProfile() {
        val users = users ?: return
        scope.launch {
            users.observeProfile().collect { profile ->
                val currentAccount = sessionKey(sessions.peekSession())
                if (currentAccount != null && currentAccount == account && ownProfileAccount == currentAccount) {
                    ownAddress = profile?.address
                    ownIdentityLoaded = profile != null
                    updateOwnProfile()
                }
            }
        }
    }

    private fun loadOwnProfile(forceRefresh: Boolean) {
        ownProfileJob?.cancel()
        val users = users ?: return
        val requestAccount = account ?: return
        if (!forceRefresh && users.observeProfile().value != null) return
        ownProfileJob = scope.launch {
            val result = if (forceRefresh) users.refreshProfile() else users.getMe()
            currentCoroutineContext().ensureActive()
            if (account != requestAccount || sessionKey(sessions.peekSession()) != requestAccount) return@launch
            result.onSuccess { profile ->
                ownProfileAccount = requestAccount
                ownIdentityLoaded = true
                ownAddress = profile.address
                updateOwnProfile()
            }
        }
    }

    private fun isOwnProfile(profile: PublicProfile): Boolean {
        val session = sessions.peekSession()
        val currentAccount = sessionKey(session) ?: return false
        if (currentAccount != account) return false
        if (ownProfileAccount != currentAccount) return false
        val address = users?.observeProfile()?.value?.address ?: ownAddress ?: return false
        return address.slug.isNotBlank() && address.slug == profile.slug ||
            address.canonicalPath.isNotBlank() && profile.canonicalPath?.trimEnd('/') == address.canonicalPath.trimEnd('/')
    }

    private fun updateOwnProfile() {
        val isOwn = state.value.profile?.let(::isOwnProfile) == true
        if (isOwn) followJob?.cancel()
        _state.update { it.copy(isOwnProfile = isOwn,
            isFollowing = if (isOwn) false else it.isFollowing,
            isFollowLoading = if (isOwn) false else it.isFollowLoading,
            followError = if (isOwn) null else it.followError,
            needsLogin = if (isOwn) false else it.needsLogin) }
    }

    fun refresh() {
        profileJob?.cancel(); checkInsJob?.cancel(); followJob?.cancel()
        favoritesJob?.cancel(); activityJob?.cancel(); connectionsJob?.cancel()
        generation++
        val requestGeneration = generation
        val requestAccount = account
        nextCursor = null
        _state.update { it.copy(isLoading = true, error = null, checkInsError = null,
            isLoadingCheckIns = false, isLoadingMoreCheckIns = false, hasMoreCheckIns = false,
            isFollowLoading = false, followError = null, isLoadingFavorites = false,
            isLoadingActivity = false, isLoadingConnections = false, isLoadingMoreConnections = false) }
        profileJob = scope.launch {
            if (!visibility.visibility.value.ready && visibility.refresh().isFailure) {
                if (isCurrent(requestGeneration, requestAccount)) _state.update { it.copy(isLoading = false, error = "Не удалось загрузить блокировки. Повторите попытку.") }
                return@launch
            }
            val result = repository.getProfile(slug)
            currentCoroutineContext().ensureActive()
            if (!isCurrent(requestGeneration, requestAccount)) return@launch
            result.onSuccess { profile ->
                val isOwn = isOwnProfile(profile)
                _state.update { it.copy(isLoading = false, profile = profile, error = null,
                    isOwnProfile = isOwn, isFollowing = !isOwn && profile.isFollowing) }
                if (profile.isBlockedByViewer || visibility.visibility.value.hides(profile.authorId, profile.slug)) {
                    _state.update { it.copy(profile = profile.copy(isBlockedByViewer = true, favorites = emptyList(), activity = null),
                        checkIns = emptyList(), connections = emptyList(), isFollowing = false, hasMoreCheckIns = false) }
                    return@onSuccess
                }
                loadCheckIns(reset = true)
                retryFavorites()
                retryActivity()
                state.value.connectionKind?.let(::openConnections)
            }.onFailure { error ->
                _state.update {
                    if (error is PublicProfileUnavailable) PublicProfileUiState(isLoading = false, error = "Профиль недоступен", activityMonth = it.activityMonth)
                    else it.copy(isLoading = false, error = "Не удалось загрузить профиль")
                }
            }
        }
    }

    fun toggleFollow() {
        val current = state.value
        val profile = current.profile?.takeUnless { it.isBlockedByViewer } ?: return
        if (profile.isBlockedByViewer || current.isFollowLoading || current.isLoading || current.isOwnProfile || isOwnProfile(profile)) return
        val requestAccount = sessionKey(sessions.peekSession())
        if (requestAccount == null) { _state.update { it.copy(needsLogin = true) }; return }
        if (requestAccount != account) return
        val requestGeneration = generation
        _state.update { it.copy(isFollowLoading = true, followError = null) }
        followJob = scope.launch {
            if (users != null && (!ownIdentityLoaded || ownProfileAccount != requestAccount)) {
                ownProfileJob?.join()
                if (!isCurrent(requestGeneration, requestAccount)) return@launch
                if (!ownIdentityLoaded || ownProfileAccount != requestAccount) {
                    val ownProfile = users.getMe()
                    currentCoroutineContext().ensureActive()
                    if (!isCurrent(requestGeneration, requestAccount)) return@launch
                    ownProfile.onSuccess {
                        ownProfileAccount = requestAccount
                        ownIdentityLoaded = true
                        ownAddress = it.address
                        updateOwnProfile()
                    }.onFailure {
                        _state.update { it.copy(isFollowLoading = false, followError = "Не удалось проверить профиль. Попробуйте ещё раз") }
                        return@launch
                    }
                }
            }
            if (isOwnProfile(profile)) { updateOwnProfile(); return@launch }
            val result = repository.setFollowing(profile.slug, !current.isFollowing)
            currentCoroutineContext().ensureActive()
            if (!isCurrent(requestGeneration, requestAccount) || isOwnProfile(profile)) return@launch
            result.onSuccess { follow ->
                _state.update { it.copy(isFollowing = follow.isFollowing, isFollowLoading = false,
                    profile = it.profile?.let { loaded -> loaded.copy(isFollowing = follow.isFollowing,
                        stats = loaded.stats.copy(followers = follow.followers)) }) }
                state.value.connectionKind?.let(::openConnections)
            }.onFailure { error ->
                _state.update { it.copy(isFollowLoading = false, followError = "Не удалось изменить подписку",
                    needsLogin = error is PublicProfileAuthenticationRequired) }
            }
        }
    }

    fun loginHandled() { _state.update { it.copy(needsLogin = false) } }

    fun retryFavorites() {
        val profile = state.value.profile?.takeUnless { it.isBlockedByViewer } ?: return
        if (state.value.isLoading) return
        favoritesJob?.cancel()
        val requestGeneration = generation
        val requestAccount = account
        _state.update { it.copy(isLoadingFavorites = true, favoritesError = null) }
        favoritesJob = scope.launch {
            val result = repository.getFavorites(profile.slug)
            currentCoroutineContext().ensureActive()
            if (!isCurrent(requestGeneration, requestAccount)) return@launch
            result.onSuccess { favorites -> _state.update { it.copy(isLoadingFavorites = false,
                profile = it.profile?.copy(favorites = favorites)) } }
                .onFailure { _state.update { it.copy(isLoadingFavorites = false, favoritesError = "Не удалось загрузить избранное") } }
        }
    }

    fun changeActivityMonth(month: String) {
        if (!month.matches(Regex("\\d{4}-(0[1-9]|1[0-2])")) || month.take(4).toInt() !in 2000..2100 || month == state.value.activityMonth) return
        _state.update { it.copy(activityMonth = month, profile = it.profile?.copy(activity = null)) }
        retryActivity()
    }

    fun retryActivity() {
        val current = state.value
        val profile = current.profile?.takeUnless { it.isBlockedByViewer } ?: return
        if (current.isLoading) return
        activityJob?.cancel()
        val period = current.activityMonth
        val requestGeneration = generation
        val requestAccount = account
        val periodGeneration = ++activityGeneration
        _state.update { it.copy(isLoadingActivity = true, activityError = null) }
        activityJob = scope.launch {
            val result = repository.getActivity(profile.slug, period.take(4).toInt(), period.takeLast(2).toInt(), "Europe/Minsk")
            currentCoroutineContext().ensureActive()
            if (!isCurrent(requestGeneration, requestAccount) || activityGeneration != periodGeneration) return@launch
            result.onSuccess { activity -> _state.update { it.copy(isLoadingActivity = false, profile = it.profile?.copy(activity = activity)) } }
                .onFailure { _state.update { it.copy(isLoadingActivity = false, activityError = "Не удалось загрузить активность") } }
        }
    }

    fun openConnections(kind: PublicProfileConnectionKind) {
        connectionsJob?.cancel()
        connectionsGeneration++
        connectionsPage = 0
        _state.update { it.copy(connectionKind = kind, connections = emptyList(), hasMoreConnections = false, connectionsError = null,
            isLoadingConnections = false, isLoadingMoreConnections = false) }
        loadConnections(reset = true)
    }

    fun closeConnections() {
        connectionsJob?.cancel()
        connectionsGeneration++
        _state.update { it.copy(connectionKind = null, connections = emptyList(), connectionsError = null,
            isLoadingConnections = false, isLoadingMoreConnections = false, hasMoreConnections = false) }
    }

    fun retryConnections() { loadConnections(reset = connectionsPage == 0) }
    fun loadMoreConnections() {
        val current = state.value
        if (current.hasMoreConnections && current.connectionsError == null) loadConnections(reset = false)
    }

    private fun loadConnections(reset: Boolean) {
        val current = state.value
        val profile = current.profile?.takeUnless { it.isBlockedByViewer } ?: return
        val kind = current.connectionKind ?: return
        if (current.isLoading || current.isLoadingConnections || current.isLoadingMoreConnections) return
        val page = if (reset) 1 else connectionsPage + 1
        if (page > 10_000) return
        val requestGeneration = generation
        val requestAccount = account
        val listGeneration = connectionsGeneration
        _state.update { it.copy(isLoadingConnections = reset, isLoadingMoreConnections = !reset, connectionsError = null) }
        connectionsJob = scope.launch {
            val result = repository.getConnections(profile.slug, kind, page, 20)
            currentCoroutineContext().ensureActive()
            if (!isCurrent(requestGeneration, requestAccount) || listGeneration != connectionsGeneration) return@launch
            result.onSuccess { loaded ->
                connectionsPage = loaded.page
                _state.update { it.copy(connections = (if (reset) loaded.items else it.connections + loaded.items).distinctBy(PublicProfileConnection::slug),
                    isLoadingConnections = false, isLoadingMoreConnections = false,
                    hasMoreConnections = loaded.items.isNotEmpty() && loaded.page < 10_000 && loaded.page.toLong() * loaded.pageSize < loaded.totalCount) }
            }.onFailure { _state.update { it.copy(isLoadingConnections = false, isLoadingMoreConnections = false, connectionsError = "Не удалось загрузить пользователей") } }
        }
    }

    fun loadMoreCheckIns() {
        val current = state.value
        if (current.hasMoreCheckIns && current.checkInsError == null && !current.isLoadingCheckIns && !current.isLoadingMoreCheckIns && !current.isLoading) loadCheckIns(reset = false)
    }

    fun retryCheckIns() {
        val current = state.value
        if (current.profile != null && !current.isLoading && !current.isLoadingCheckIns && !current.isLoadingMoreCheckIns) loadCheckIns(reset = true)
    }

    private fun loadCheckIns(reset: Boolean) {
        val profile = state.value.profile?.takeUnless { it.isBlockedByViewer } ?: return
        val cursor = if (reset) null else nextCursor
        val requestGeneration = generation
        val requestAccount = account
        _state.update { it.copy(isLoadingCheckIns = reset, isLoadingMoreCheckIns = !reset, checkInsError = null) }
        checkInsJob = scope.launch {
            val result = repository.getCheckIns(profile.slug, cursor)
            currentCoroutineContext().ensureActive()
            if (!isCurrent(requestGeneration, requestAccount)) return@launch
            result.onSuccess { page ->
                nextCursor = page.nextCursor
                _state.update { it.copy(checkIns = (if (reset) page.items else it.checkIns + page.items).distinctBy(PublicProfileCheckIn::id),
                    hasMoreCheckIns = page.nextCursor != null, isLoadingCheckIns = false, isLoadingMoreCheckIns = false, checkInsError = null) }
            }.onFailure { _state.update { it.copy(isLoadingCheckIns = false, isLoadingMoreCheckIns = false, checkInsError = "Не удалось загрузить чекины") } }
        }
    }

    override fun close() { scope.cancel() }
}

@OptIn(ExperimentalTime::class)
private fun currentActivityMonth(): String = (Clock.System.now() + 3.hours).toString().take(7)
