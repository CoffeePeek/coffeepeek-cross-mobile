package com.coffeepeek.feature.profile.impl.ui

import androidx.lifecycle.viewModelScope
import com.coffeepeek.core.presentation.MviViewModel
import com.coffeepeek.feature.profile.api.ProfileAccountPort
import com.coffeepeek.feature.profile.api.ProfileAuthorVisibility
import com.coffeepeek.feature.profile.api.ProfileAuthorVisibilityState
import com.coffeepeek.feature.profile.api.ProfileSnapshot
import com.coffeepeek.feature.profile.api.ProfileSnapshotStore
import com.coffeepeek.feature.profile.api.PublicCheckInsPort
import com.coffeepeek.feature.profile.domain.PublicProfile
import com.coffeepeek.feature.profile.domain.PublicProfileAuthenticationRequired
import com.coffeepeek.feature.profile.domain.PublicProfileCheckIn
import com.coffeepeek.feature.profile.domain.PublicProfileConnection
import com.coffeepeek.feature.profile.domain.PublicProfileConnectionKind
import com.coffeepeek.feature.profile.domain.PublicProfileData
import com.coffeepeek.feature.profile.domain.PublicProfileUnavailable
import com.coffeepeek.feature.profile.domain.isOwnPublicProfile
import com.coffeepeek.feature.profile.domain.isValidActivityMonth
import com.coffeepeek.feature.profile.impl.ui.model.PublicProfileError
import com.coffeepeek.feature.profile.impl.ui.model.PublicProfileAction
import com.coffeepeek.feature.profile.impl.ui.model.PublicProfileEvent
import com.coffeepeek.feature.profile.impl.ui.model.PublicProfileState
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/**
 * Public profile of one author. Every response is tied to a (generation, account) pair: a refresh, a visibility change or an
 * account switch makes older responses stale, so one viewer's data can never land in another viewer's screen.
 */
internal class PublicProfileViewModel(
    private val slug: String,
    private val data: PublicProfileData,
    private val checkIns: PublicCheckInsPort,
    private val accounts: ProfileAccountPort,
    private val visibility: ProfileAuthorVisibility = ProfileAuthorVisibility.None,
    private val snapshots: ProfileSnapshotStore = ProfileSnapshotStore.None,
) : MviViewModel<PublicProfileState, PublicProfileAction, PublicProfileEvent>(cachedState(slug, accounts, visibility, snapshots)) {
    private var initialized = false
    private var profileJob: Job? = null
    private var checkInsJob: Job? = null
    private var followJob: Job? = null
    private var favoritesJob: Job? = null
    private var activityJob: Job? = null
    private var connectionsJob: Job? = null
    private var ownProfileJob: Job? = null
    private var nextCursor: String? = cachedSnapshot(slug, accounts, visibility, snapshots)?.nextCheckInCursor
    private var connectionsPage = 0
    private var generation = 0
    private var generationScope = accounts.cacheScope()
    private var generationRevision = visibility.current().revision
    private var activityGeneration = 0
    private var connectionsGeneration = 0
    private var account = accounts.activeAccount()
    private var ownProfileAccount = account
    private var ownIdentityLoaded = accounts.cachedOwnProfile() != null
    private var ownAddress = accounts.cachedOwnProfile()?.address.takeIf { account != null }

    private fun rememberSnapshot() {
        val scope = accounts.cacheScope() ?: return
        val current = currentState
        val profile = current.profile?.takeUnless { it.isBlockedByViewer } ?: return
        val access = visibility.current()
        if (!access.ready || access.hides(profile.authorId, profile.slug)) return
        if (scope != generationScope || access.revision != generationRevision || accounts.activeAccount() != account) return
        snapshots.write(scope, slug, access.revision, ProfileSnapshot(profile, current.checkIns, nextCursor, current.activityMonth))
    }

    private fun PublicProfileState.refreshedProfileState(received: PublicProfile, isOwn: Boolean): PublicProfileState {
        val previous = profile
        val sameAuthor = previous != null && sameAuthor(previous, received)
        val visible = if (isOwn) received else received.copy(favorites = emptyList())
        val retained = visible.copy(
            favorites = if (isOwn && sameAuthor && visible.favorites.isEmpty()) previous!!.favorites else visible.favorites,
            activity = visible.activity ?: if (sameAuthor) previous?.activity?.takeIf { it.month == activityMonth } else null,
        )
        if (!sameAuthor) nextCursor = null
        return copy(isLoading = false, profile = retained, error = null, isOwnProfile = isOwn,
            isFollowing = !isOwn && received.isFollowing, checkIns = if (sameAuthor) checkIns else emptyList(),
            hasMoreCheckIns = sameAuthor && hasMoreCheckIns)
    }

    private fun sameAuthor(first: PublicProfile, second: PublicProfile): Boolean =
        if (first.authorId != null || second.authorId != null) first.authorId != null && first.authorId == second.authorId
        else first.slug == second.slug && first.canonicalPath != null && first.canonicalPath == second.canonicalPath

    /** Author visibility for the screen: open photos and sheets are closed when a new revision arrives. */
    val authorVisibility: Flow<ProfileAuthorVisibilityState> get() = visibility.observe()
    fun currentVisibility(): ProfileAuthorVisibilityState = visibility.current()

    init { onAction(PublicProfileAction.Initialize) }

    override suspend fun handleActionInternal(action: PublicProfileAction) {
        when (action) {
            PublicProfileAction.Initialize -> if (!initialized) { initialized = true; start() }
            PublicProfileAction.Refresh -> refresh()
            PublicProfileAction.ToggleFollow -> toggleFollow()
            PublicProfileAction.RetryFavorites -> retryFavorites()
            is PublicProfileAction.ChangeActivityMonth -> changeActivityMonth(action.month)
            PublicProfileAction.RetryActivity -> retryActivity()
            is PublicProfileAction.OpenConnections -> openConnections(action.kind)
            PublicProfileAction.CloseConnections -> closeConnections()
            PublicProfileAction.RetryConnections -> loadConnections(reset = connectionsPage == 0)
            PublicProfileAction.LoadMoreConnections -> {
                val current = currentState
                if (current.hasMoreConnections && current.connectionsError == null) loadConnections(reset = false)
            }
            PublicProfileAction.LoadMoreCheckIns -> {
                val current = currentState
                if (current.hasMoreCheckIns && current.checkInsError == null && !current.isLoadingCheckIns &&
                    !current.isLoadingMoreCheckIns && !current.isLoading) loadCheckIns(reset = false)
            }
            PublicProfileAction.RetryCheckIns -> {
                val current = currentState
                if (current.profile != null && !current.isLoading && !current.isLoadingCheckIns && !current.isLoadingMoreCheckIns) loadCheckIns(reset = true)
            }
        }
    }

    private fun start() {
        viewModelScope.launch {
            visibility.observe().drop(1).collect {
                updateState { PublicProfileState(activityMonth = activityMonth) }
                refresh()
            }
        }
        refresh()
        observeOwnProfile()
        loadOwnProfile(forceRefresh = false)
        viewModelScope.launch {
            accounts.observeActiveAccount().collect { current ->
                if (current != account) resetViewer(current)
            }
        }
        viewModelScope.launch {
            accounts.observeCacheScope().collect { current ->
                if (current != generationScope) resetViewer(accounts.activeAccount())
            }
        }
    }

    private fun resetViewer(current: String?) {
        account = current
        ownProfileAccount = null
        ownIdentityLoaded = false
        ownAddress = null
        updateState { PublicProfileState(activityMonth = activityMonth) }
        refresh()
        loadOwnProfile(forceRefresh = true)
    }

    private fun isCurrent(requestGeneration: Int, requestAccount: String?): Boolean =
        generation == requestGeneration && account == requestAccount && accounts.activeAccount() == requestAccount &&
            generationScope == accounts.cacheScope() && generationRevision == visibility.current().revision

    private fun observeOwnProfile() {
        viewModelScope.launch {
            accounts.observeOwnProfile().collect { profile ->
                val currentAccount = accounts.activeAccount()
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
        val requestAccount = account ?: return
        if (!forceRefresh && accounts.cachedOwnProfile() != null) return
        ownProfileJob = viewModelScope.launch {
            val result = accounts.loadOwnProfile(forceRefresh)
            currentCoroutineContext().ensureActive()
            if (account != requestAccount || accounts.activeAccount() != requestAccount) return@launch
            result.onSuccess { profile ->
                ownProfileAccount = requestAccount
                ownIdentityLoaded = true
                ownAddress = profile.address
                updateOwnProfile()
            }
        }
    }

    private fun isOwnProfile(profile: PublicProfile): Boolean {
        val currentAccount = accounts.activeAccount() ?: return false
        if (currentAccount != account) return false
        if (ownProfileAccount != currentAccount) return false
        val address = accounts.cachedOwnProfile()?.address ?: ownAddress ?: return false
        return isOwnPublicProfile(address, profile)
    }

    private fun updateOwnProfile() {
        val wasOwn = currentState.isOwnProfile
        val isOwn = currentState.profile?.let(::isOwnProfile) == true
        if (isOwn) followJob?.cancel()
        else favoritesJob?.cancel()
        updateState { copy(isOwnProfile = isOwn,
            profile = profile?.let { if (isOwn) it else it.copy(favorites = emptyList()) },
            isLoadingFavorites = if (isOwn) isLoadingFavorites else false,
            favoritesError = if (isOwn) favoritesError else null,
            isFollowing = if (isOwn) false else isFollowing,
            isFollowLoading = if (isOwn) false else isFollowLoading,
            followError = if (isOwn) null else followError) }
        rememberSnapshot()
        if (isOwn && !wasOwn) retryFavorites()
    }

    private fun refresh() {
        cancelRefreshJobs()
        generation++
        generationScope = accounts.cacheScope()
        generationRevision = visibility.current().revision
        val requestGeneration = generation
        val requestAccount = account
        nextCursor = nextCursor.takeIf { currentState.profile != null }
        updateState { copy(isLoading = true, error = null, checkInsError = null,
            isLoadingCheckIns = false, isLoadingMoreCheckIns = false,
            isFollowLoading = false, followError = null, isLoadingFavorites = false,
            isLoadingActivity = false, activityError = null, isLoadingConnections = false, isLoadingMoreConnections = false) }
        profileJob = viewModelScope.launch { loadProfile(requestGeneration, requestAccount) }
    }

    private fun cancelRefreshJobs() {
        profileJob?.cancel(); checkInsJob?.cancel(); followJob?.cancel()
        favoritesJob?.cancel(); activityJob?.cancel(); connectionsJob?.cancel()
    }

    private suspend fun loadProfile(requestGeneration: Int, requestAccount: String?) {
        val accessFailure = if (!visibility.current().ready) visibility.refresh().isFailure else false
        if (accessFailure || !visibility.current().ready) {
            if (isCurrent(requestGeneration, requestAccount)) updateState {
                PublicProfileState(isLoading = false, error = PublicProfileError.BlocksLoad, activityMonth = activityMonth)
            }
            return
        }
        val result = data.getProfile(slug)
        currentCoroutineContext().ensureActive()
        if (!isCurrent(requestGeneration, requestAccount)) return
        result.onSuccess(::applyLoadedProfile).onFailure(::applyProfileFailure)
    }

    private fun applyLoadedProfile(profile: PublicProfile) {
        updateState { refreshedProfileState(profile, isOwnProfile(profile)) }
        if (profile.isBlockedByViewer || visibility.current().hides(profile.authorId, profile.slug)) {
            updateState { copy(profile = profile.copy(isBlockedByViewer = true, favorites = emptyList(), activity = null),
                checkIns = emptyList(), connections = emptyList(), isFollowing = false, hasMoreCheckIns = false) }
            snapshots.clear()
            return
        }
        rememberSnapshot()
        loadCheckIns(reset = true)
        retryFavorites()
        retryActivity()
        currentState.connectionKind?.let(::openConnections)
    }

    private fun applyProfileFailure(error: Throwable) {
        val message = if (error is PublicProfileUnavailable) PublicProfileError.Unavailable else PublicProfileError.Load
        updateState {
            if (error is PublicProfileUnavailable) PublicProfileState(isLoading = false, error = message, activityMonth = activityMonth)
            else copy(isLoading = false, error = message)
        }
        if (error is PublicProfileUnavailable) snapshots.clear()
    }

    private suspend fun toggleFollow() {
        val current = currentState
        val profile = current.profile?.takeUnless { it.isBlockedByViewer } ?: return
        if (profile.isBlockedByViewer || current.isFollowLoading || current.isLoading || current.isOwnProfile || isOwnProfile(profile)) return
        val requestAccount = accounts.activeAccount()
        if (requestAccount == null) { sendEvent(PublicProfileEvent.RequireLogin); return }
        if (requestAccount != account) return
        val requestGeneration = generation
        updateState { copy(isFollowLoading = true, followError = null) }
        followJob = viewModelScope.launch {
            if (!ownIdentityLoaded || ownProfileAccount != requestAccount) {
                ownProfileJob?.join()
                if (!isCurrent(requestGeneration, requestAccount)) return@launch
                if (!ownIdentityLoaded || ownProfileAccount != requestAccount) {
                    val ownProfile = accounts.loadOwnProfile(forceRefresh = false)
                    val failureMessage = if (ownProfile.isFailure) PublicProfileError.Verification else null
                    currentCoroutineContext().ensureActive()
                    if (!isCurrent(requestGeneration, requestAccount)) return@launch
                    ownProfile.onSuccess {
                        ownProfileAccount = requestAccount
                        ownIdentityLoaded = true
                        ownAddress = it.address
                        updateOwnProfile()
                    }.onFailure {
                        updateState { copy(isFollowLoading = false, followError = failureMessage) }
                        return@launch
                    }
                }
            }
            if (isOwnProfile(profile)) { updateOwnProfile(); return@launch }
            val result = data.setFollowing(profile.slug, !current.isFollowing)
            val failureMessage = if (result.isFailure) PublicProfileError.Follow else null
            currentCoroutineContext().ensureActive()
            if (!isCurrent(requestGeneration, requestAccount) || isOwnProfile(profile)) return@launch
            result.onSuccess { follow ->
                updateState { copy(isFollowing = follow.isFollowing, isFollowLoading = false,
                    profile = this.profile?.let { loaded -> loaded.copy(isFollowing = follow.isFollowing,
                        stats = loaded.stats.copy(followers = follow.followers)) }) }
                rememberSnapshot()
                currentState.connectionKind?.let(::openConnections)
            }.onFailure { error ->
                updateState { copy(isFollowLoading = false, followError = failureMessage) }
                if (error is PublicProfileAuthenticationRequired) sendEvent(PublicProfileEvent.RequireLogin)
            }
        }
    }

    private fun retryFavorites() {
        val profile = currentState.profile?.takeUnless { it.isBlockedByViewer } ?: return
        if (currentState.isLoading || !isOwnProfile(profile)) return
        favoritesJob?.cancel()
        val requestGeneration = generation
        val requestAccount = account
        updateState { copy(isLoadingFavorites = true, favoritesError = null) }
        favoritesJob = viewModelScope.launch {
            val result = data.getFavorites(profile.slug)
            val failureMessage = if (result.isFailure) PublicProfileError.Favorites else null
            currentCoroutineContext().ensureActive()
            if (!isCurrent(requestGeneration, requestAccount) || !isOwnProfile(profile)) return@launch
            result.onSuccess { favorites ->
                updateState { copy(isLoadingFavorites = false, profile = this.profile?.copy(favorites = favorites)) }
                rememberSnapshot()
            }
                .onFailure { updateState { copy(isLoadingFavorites = false, favoritesError = failureMessage) } }
        }
    }

    private fun changeActivityMonth(month: String) {
        if (!isValidActivityMonth(month) || month == currentState.activityMonth) return
        updateState { copy(activityMonth = month, profile = profile?.copy(activity = null)) }
        rememberSnapshot()
        retryActivity()
    }

    private fun retryActivity() {
        val current = currentState
        val profile = current.profile?.takeUnless { it.isBlockedByViewer } ?: return
        if (current.isLoading) return
        activityJob?.cancel()
        val period = current.activityMonth
        val requestGeneration = generation
        val requestAccount = account
        val periodGeneration = ++activityGeneration
        updateState { copy(isLoadingActivity = true, activityError = null) }
        activityJob = viewModelScope.launch {
            val result = data.getActivity(profile.slug, period.take(4).toInt(), period.takeLast(2).toInt(), "Europe/Minsk")
            val failureMessage = if (result.isFailure) PublicProfileError.Activity else null
            currentCoroutineContext().ensureActive()
            if (!isCurrent(requestGeneration, requestAccount) || activityGeneration != periodGeneration) return@launch
            result.onSuccess { activity ->
                updateState { copy(isLoadingActivity = false, profile = this.profile?.copy(activity = activity)) }
                rememberSnapshot()
            }
                .onFailure { updateState { copy(isLoadingActivity = false, activityError = failureMessage) } }
        }
    }

    private fun openConnections(kind: PublicProfileConnectionKind) {
        connectionsJob?.cancel()
        connectionsGeneration++
        connectionsPage = 0
        updateState { copy(connectionKind = kind, connections = emptyList(), hasMoreConnections = false, connectionsError = null,
            isLoadingConnections = false, isLoadingMoreConnections = false) }
        loadConnections(reset = true)
    }

    private fun closeConnections() {
        connectionsJob?.cancel()
        connectionsGeneration++
        updateState { copy(connectionKind = null, connections = emptyList(), connectionsError = null,
            isLoadingConnections = false, isLoadingMoreConnections = false, hasMoreConnections = false) }
    }

    private fun loadConnections(reset: Boolean) {
        val current = currentState
        val profile = current.profile?.takeUnless { it.isBlockedByViewer } ?: return
        val kind = current.connectionKind ?: return
        if (current.isLoading || current.isLoadingConnections || current.isLoadingMoreConnections) return
        val page = if (reset) 1 else connectionsPage + 1
        if (page > 10_000) return
        val requestGeneration = generation
        val requestAccount = account
        val listGeneration = connectionsGeneration
        updateState { copy(isLoadingConnections = reset, isLoadingMoreConnections = !reset, connectionsError = null) }
        connectionsJob = viewModelScope.launch {
            val result = data.getConnections(profile.slug, kind, page, 20)
            val failureMessage = if (result.isFailure) PublicProfileError.Connections else null
            currentCoroutineContext().ensureActive()
            if (!isCurrent(requestGeneration, requestAccount) || listGeneration != connectionsGeneration) return@launch
            result.onSuccess { loaded ->
                connectionsPage = loaded.page
                updateState { copy(connections = (if (reset) loaded.items else connections + loaded.items).distinctBy(PublicProfileConnection::slug),
                    isLoadingConnections = false, isLoadingMoreConnections = false,
                    hasMoreConnections = loaded.items.isNotEmpty() && loaded.page < 10_000 && loaded.page.toLong() * loaded.pageSize < loaded.totalCount) }
            }.onFailure { updateState { copy(isLoadingConnections = false, isLoadingMoreConnections = false, connectionsError = failureMessage) } }
        }
    }

    private fun loadCheckIns(reset: Boolean) {
        val profile = currentState.profile?.takeUnless { it.isBlockedByViewer } ?: return
        val cursor = if (reset) null else nextCursor
        val requestGeneration = generation
        val requestAccount = account
        updateState { copy(isLoadingCheckIns = reset, isLoadingMoreCheckIns = !reset, checkInsError = null) }
        checkInsJob = viewModelScope.launch {
            val result = checkIns.page(profile.slug, cursor)
            val failureMessage = if (result.isFailure) PublicProfileError.CheckIns else null
            currentCoroutineContext().ensureActive()
            if (!isCurrent(requestGeneration, requestAccount)) return@launch
            result.onSuccess { page ->
                nextCursor = page.nextCursor
                updateState { copy(checkIns = (if (reset) page.items else this.checkIns + page.items).distinctBy(PublicProfileCheckIn::id),
                    hasMoreCheckIns = page.nextCursor != null, isLoadingCheckIns = false, isLoadingMoreCheckIns = false, checkInsError = null) }
                rememberSnapshot()
            }.onFailure { updateState { copy(isLoadingCheckIns = false, isLoadingMoreCheckIns = false, checkInsError = failureMessage) } }
        }
    }
}

private fun cachedState(
    slug: String, accounts: ProfileAccountPort, visibility: ProfileAuthorVisibility, snapshots: ProfileSnapshotStore,
): PublicProfileState {
    val account = accounts.activeAccount()
    val saved = cachedSnapshot(slug, accounts, visibility, snapshots) ?: return PublicProfileState()
    val profile = saved.profile
    val own = account != null && accounts.cachedOwnProfile()?.address?.let { isOwnPublicProfile(it, profile) } == true
    val visible = if (own) profile else profile.copy(favorites = emptyList())
    return PublicProfileState(isLoading = false, profile = visible, checkIns = saved.checkIns,
        hasMoreCheckIns = saved.nextCheckInCursor != null, activityMonth = saved.activityMonth,
        isOwnProfile = own, isFollowing = !own && visible.isFollowing)
}

private fun cachedSnapshot(
    slug: String, accounts: ProfileAccountPort, visibility: ProfileAuthorVisibility, snapshots: ProfileSnapshotStore,
): ProfileSnapshot? {
    val account = accounts.activeAccount()
    val scope = accounts.cacheScope() ?: return null
    val access = visibility.current()
    if (!access.ready) return null
    val saved = snapshots.read(scope, slug, access.revision) ?: return null
    if (access.hides(saved.profile.authorId, saved.profile.slug) || saved.profile.isBlockedByViewer) return null
    if (account != accounts.activeAccount() || scope != accounts.cacheScope() || access != visibility.current()) return null
    return saved
}
