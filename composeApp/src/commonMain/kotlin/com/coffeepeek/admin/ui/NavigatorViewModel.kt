package com.coffeepeek.admin.ui

import com.coffeepeek.admin.base.BaseViewModel
import com.coffeepeek.core.session.SessionReader
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class NavigatorViewModel(
    private val sessionRepository: SessionReader,
) : BaseViewModel() {

    val isLoggedIn: StateFlow<Boolean?> = sessionRepository.observeSession()
        .map { session -> sessionRepository.isActiveSession(session) }
        .stateIn(
            scope = workScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = null,
        )
}
