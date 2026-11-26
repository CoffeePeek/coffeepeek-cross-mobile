package com.coffeepeek.feature.auth.impl

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.auth.domain.AuthRepository
import com.coffeepeek.feature.auth.impl.recovery.PasswordRecoveryAction
import com.coffeepeek.feature.auth.impl.recovery.PasswordRecoveryState
import com.coffeepeek.feature.auth.impl.recovery.PasswordRecoveryViewModel
import com.coffeepeek.feature.auth.impl.recovery.RecoveryError
import com.coffeepeek.feature.auth.impl.recovery.RecoveryStep
import kotlinx.coroutines.flow.map

class NativePasswordRecoverySession(initialEmail: String, repository: AuthRepository) {
    private val model = PasswordRecoveryViewModel(initialEmail, repository)
    private val store = ViewModelStore().apply { put("auth-recovery", model) }

    val state = model.state.map(PasswordRecoveryState::snapshot)
    fun current() = model.state.value.snapshot()
    fun emailChanged(value: String) = model.onAction(PasswordRecoveryAction.EmailChanged(value))
    fun submit() = model.onAction(PasswordRecoveryAction.Submit)
    fun requestAnotherEmail() = model.onAction(PasswordRecoveryAction.RequestAnotherEmail)
    fun close() = store.clear()
}

enum class NativeRecoveryFailure { EmailRequired, EmailInvalid, SendingFailed }

data class NativePasswordRecoveryState(
    val email: String,
    val busy: Boolean,
    val emailSent: Boolean,
    val failure: NativeRecoveryFailure?,
)

private fun PasswordRecoveryState.snapshot() = NativePasswordRecoveryState(email, busy, step == RecoveryStep.EmailSent, when (error) {
    RecoveryError.EmailEmpty -> NativeRecoveryFailure.EmailRequired
    RecoveryError.EmailInvalid -> NativeRecoveryFailure.EmailInvalid
    RecoveryError.SendFailed -> NativeRecoveryFailure.SendingFailed
    null -> null
})
