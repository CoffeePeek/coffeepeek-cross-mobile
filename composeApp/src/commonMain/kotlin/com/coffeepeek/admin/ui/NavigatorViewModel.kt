package com.coffeepeek.admin.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.auth.api.UsernameSelectionGate
import kotlinx.coroutines.launch

class NavigatorViewModel(
    private val sessionRepository: SessionReader,
    private val usernameSelection: UsernameSelectionGate,
) : ViewModel() {
    private val lifecycle = RootNavigationLifecycle(Navigator::prepareRootTransition, Navigator::acceptRootTransition)
    internal val navigationState = lifecycle.state
    internal fun dismissOnboarding() = lifecycle.dismissOnboarding()
    init {
        viewModelScope.launch { sessionRepository.observeReadyRootNavigationSession(usernameSelection).collect(lifecycle::accept) }
    }
}
