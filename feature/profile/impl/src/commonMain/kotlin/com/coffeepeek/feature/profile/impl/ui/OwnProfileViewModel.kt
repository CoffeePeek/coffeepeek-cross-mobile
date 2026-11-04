package com.coffeepeek.feature.profile.impl.ui

import androidx.lifecycle.viewModelScope
import com.coffeepeek.core.presentation.MviViewModel
import com.coffeepeek.feature.profile.api.OwnProfile
import com.coffeepeek.feature.profile.api.ProfileAccountPort
import com.coffeepeek.feature.profile.api.ProfileSignOutPort
import com.coffeepeek.feature.profile.impl.ui.model.OwnProfileError
import com.coffeepeek.feature.profile.impl.ui.model.OwnProfileAction
import com.coffeepeek.feature.profile.impl.ui.model.OwnProfileEvent
import com.coffeepeek.feature.profile.impl.ui.model.OwnProfileState
import kotlinx.coroutines.launch

/**
 * The Profile tab's account state. It follows the active account: a switch or a sign-out clears the previous user's data first, a late
 * answer for an old account is ignored, and a refresh failure keeps visible content (only [OwnProfileState.refreshError] is set).
 */
internal class OwnProfileViewModel(
    private val accounts: ProfileAccountPort,
    private val signOut: ProfileSignOutPort,
) : MviViewModel<OwnProfileState, OwnProfileAction, OwnProfileEvent>(cachedOwnProfileState(accounts)) {
    private var initialized = false
    private var loadedForAccount: String? = accounts.activeAccount()
    private var loadedForScope: String? = accounts.cacheScope()

    init { onAction(OwnProfileAction.Initialize) }

    override suspend fun handleActionInternal(action: OwnProfileAction) {
        when (action) {
            OwnProfileAction.Initialize -> if (!initialized) { initialized = true; start() }
            OwnProfileAction.Refresh -> refresh()
            OwnProfileAction.Logout -> logout()
        }
    }

    private fun start() {
        viewModelScope.launch { observeProfile() }
        viewModelScope.launch { observeAccount() }
        viewModelScope.launch { observeScope() }
        refresh()
    }

    private suspend fun observeProfile() {
        accounts.observeOwnProfile().collect { profile ->
            if (profile != null) applyProfile(profile)
            else {
                val signedIn = accounts.activeAccount() != null && loadedForAccount != null
                updateState { OwnProfileState(isLoggedIn = signedIn, isLoading = signedIn && error == null, error = error) }
            }
        }
    }

    private suspend fun observeAccount() {
        accounts.observeActiveAccount().collect { account ->
            if (account == null) resetState()
            else if (account != loadedForAccount) {
                if (loadedForAccount != null) resetState()
                updateState { copy(isLoggedIn = true, isLoading = true) }
                loadedForAccount = account
                loadedForScope = accounts.cacheScope()
                accounts.cachedOwnProfile()?.let(::applyProfile)
                refresh()
            }
        }
    }

    private suspend fun observeScope() {
        accounts.observeCacheScope().collect { scope ->
            if (scope != loadedForScope) {
                resetState()
                loadedForScope = scope
                val account = accounts.activeAccount()
                if (account != null) {
                    loadedForAccount = account
                    accounts.cachedOwnProfile()?.let(::applyProfile)
                    refresh()
                }
            }
        }
    }

    private fun refresh() {
        val account = accounts.activeAccount()
        if (account == null) { resetState(); return }
        val scope = accounts.cacheScope()
        viewModelScope.launch {
            if (loadedForAccount != account || accounts.activeAccount() != account ||
                loadedForScope != scope || accounts.cacheScope() != scope) return@launch
            val showFullScreenLoader = !currentState.hasContent && currentState.error == null
            updateState {
                copy(isLoading = showFullScreenLoader, isRefreshing = !showFullScreenLoader,
                    error = if (showFullScreenLoader) null else error, refreshError = null)
            }
            accounts.loadOwnProfile(forceRefresh = true).onFailure { handleRefreshFailure(account, scope) }
        }
    }

    private fun handleRefreshFailure(account: String, scope: String?) {
        if (accounts.activeAccount() != account || loadedForAccount != account ||
            loadedForScope != scope || accounts.cacheScope() != scope) return
        updateState {
            if (hasContent) copy(isLoading = false, isRefreshing = false, refreshError = OwnProfileError.LoadFailed)
            else copy(isLoading = false, isRefreshing = false, error = OwnProfileError.LoadFailed)
        }
    }

    private fun logout() {
        resetState()
        viewModelScope.launch { signOut.signOut() }
    }

    private fun applyProfile(profile: OwnProfile) {
        val account = accounts.activeAccount()
        updateState {
            if (account == null || loadedForAccount == null || loadedForAccount != account || loadedForScope != accounts.cacheScope()) OwnProfileState(isLoading = false)
            else copy(isLoggedIn = true, profile = profile, isLoading = false, isRefreshing = false, error = null, refreshError = null)
        }
    }

    private fun resetState() {
        loadedForAccount = null
        loadedForScope = null
        updateState { OwnProfileState(isLoading = false) }
    }

}

private fun cachedOwnProfileState(accounts: ProfileAccountPort): OwnProfileState {
    val account = accounts.activeAccount() ?: return OwnProfileState(isLoading = false)
    val cached = accounts.cachedOwnProfile().takeIf { accounts.activeAccount() == account }
    return OwnProfileState(isLoggedIn = true, profile = cached, isLoading = cached == null)
}
