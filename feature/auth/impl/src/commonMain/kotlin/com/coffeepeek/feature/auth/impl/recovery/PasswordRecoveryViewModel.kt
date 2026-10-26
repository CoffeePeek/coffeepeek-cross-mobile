package com.coffeepeek.feature.auth.impl.recovery

import com.coffeepeek.core.presentation.MviViewModel
import com.coffeepeek.feature.auth.domain.AuthRepository
import com.coffeepeek.feature.auth.domain.EmailIssue
import com.coffeepeek.feature.auth.domain.validateEmail

/** Asks the server for a reset e-mail. Edits are ignored while a request runs; a failure can be retried. */
internal class PasswordRecoveryViewModel(
    initialEmail: String,
    private val repository: AuthRepository,
) : MviViewModel<PasswordRecoveryState, PasswordRecoveryAction, PasswordRecoveryEvent>(PasswordRecoveryState(email = initialEmail)) {

    override suspend fun handleActionInternal(action: PasswordRecoveryAction) {
        when (action) {
            is PasswordRecoveryAction.EmailChanged -> edit { copy(email = action.value) }
            PasswordRecoveryAction.RequestAnotherEmail -> edit { copy(step = RecoveryStep.Email) }
            PasswordRecoveryAction.Submit -> submit()
        }
    }

    private fun edit(change: PasswordRecoveryState.() -> PasswordRecoveryState) {
        if (!currentState.busy) updateState { change().copy(error = null) }
    }

    private suspend fun submit() {
        val current = currentState
        if (current.busy || current.step != RecoveryStep.Email) return
        val issue = validateEmail(current.email)
        if (issue != null) {
            updateState { copy(error = if (issue == EmailIssue.Empty) RecoveryError.EmailEmpty else RecoveryError.EmailInvalid) }
            return
        }
        updateState { copy(busy = true, error = null) }
        repository.requestPasswordReset(current.email).fold(
            onSuccess = { updateState { copy(busy = false, step = RecoveryStep.EmailSent) } },
            onFailure = { updateState { copy(busy = false, error = RecoveryError.SendFailed) } },
        )
    }
}
