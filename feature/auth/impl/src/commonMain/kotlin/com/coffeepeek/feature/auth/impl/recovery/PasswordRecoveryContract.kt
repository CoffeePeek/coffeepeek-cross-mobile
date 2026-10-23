package com.coffeepeek.feature.auth.impl.recovery

internal enum class RecoveryStep { Email, EmailSent }

internal enum class RecoveryError { EmailEmpty, EmailInvalid, SendFailed }

internal data class PasswordRecoveryState(
    val step: RecoveryStep = RecoveryStep.Email,
    val email: String = "",
    val busy: Boolean = false,
    val error: RecoveryError? = null,
)

internal sealed interface PasswordRecoveryAction {
    data class EmailChanged(val value: String) : PasswordRecoveryAction
    data object RequestAnotherEmail : PasswordRecoveryAction
    data object Submit : PasswordRecoveryAction
}

internal sealed interface PasswordRecoveryEvent
