package com.coffeepeek.admin.di.settings

import com.coffeepeek.feature.profile.impl.NativeAccountDeletionSession
import com.coffeepeek.feature.profile.impl.NativeAccountDeletionState
import com.coffeepeek.feature.settings.api.LanguagePort
import kotlinx.coroutines.flow.map

class IosAccountDeletion internal constructor(
    private val session: NativeAccountDeletionSession,
    language: LanguagePort,
    onBack: () -> Unit,
    private val onSessionEnded: () -> Unit,
) : IosSettingsScreen(language, onBack, session::close) {
    fun currentState() = session.current().toIos()
    fun observeState(onState: (IosAccountDeletionState) -> Unit) = observe(session.state.map { it.toIos() }, onState)
    fun observeSessionEnd() = observe(session.ended) { onSessionEnded() }
    fun send() = session.send()
    fun dismissError() = session.dismissError()
}

data class IosAccountDeletionState(
    val email: String, val loading: Boolean, val sending: Boolean, val requested: Boolean,
    val canSend: Boolean, val resendAvailable: Boolean, val revoked: Boolean,
    val errorKey: String?, val serverError: String?,
)

private fun NativeAccountDeletionState.toIos() = IosAccountDeletionState(
    email, loading, sending, requested, canSend, resendAvailable, revoked, errorKey, serverError,
)
