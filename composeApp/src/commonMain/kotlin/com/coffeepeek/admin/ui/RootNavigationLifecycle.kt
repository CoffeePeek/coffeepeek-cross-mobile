package com.coffeepeek.admin.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal data class RootNavigationState(val session: RootNavigationSession? = null,
    val generation: Int = 0, val onboardingDismissed: Boolean = false)

internal data class RootSessionTransition(val previous: RootNavigationSession?, val current: RootNavigationSession) {
    val resetsNavigation get() = previous != null && previous != current
    val clearsPrivateRequests get() = resetsNavigation && (!current.isLoggedIn || previous?.isLoggedIn == true)
}

/** Root routing rules shared by native and Compose hosts; the caller owns its serial lifecycle scope. */
internal class RootNavigationLifecycle(private val prepare: (RootSessionTransition) -> Unit = {},
    private val accepted: suspend (RootSessionTransition) -> Unit = {}) {
    private val mutable = MutableStateFlow(RootNavigationState())
    val state = mutable.asStateFlow()

    suspend fun accept(session: RootNavigationSession) {
        val previous = mutable.value
        if (previous.session == session) return
        val transition = RootSessionTransition(previous.session, session)
        prepare(transition)
        mutable.value = RootNavigationState(session, previous.generation + if (transition.resetsNavigation) 1 else 0,
            previous.onboardingDismissed && !session.isLoggedIn)
        accepted(transition)
    }

    fun dismissOnboarding(): Boolean {
        val state = mutable.value
        if (state.session?.isLoggedIn != false || state.onboardingDismissed) return false
        return mutable.compareAndSet(state, state.copy(onboardingDismissed = true))
    }
}
