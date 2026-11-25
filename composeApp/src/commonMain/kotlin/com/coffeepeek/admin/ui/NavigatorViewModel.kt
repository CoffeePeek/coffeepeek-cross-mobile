package com.coffeepeek.admin.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coffeepeek.core.session.SessionReader
import kotlinx.coroutines.launch

class NavigatorViewModel(
    private val sessionRepository: SessionReader,
) : ViewModel() {
    private val lifecycle = RootNavigationLifecycle(Navigator::prepareRootTransition, Navigator::acceptRootTransition)
    internal val navigationState = lifecycle.state
    internal fun dismissOnboarding() = lifecycle.dismissOnboarding()
    init {
        viewModelScope.launch { sessionRepository.observeRootNavigationSession().collect(lifecycle::accept) }
    }
}
