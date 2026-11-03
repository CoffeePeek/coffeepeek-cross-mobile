package com.coffeepeek.feature.profile.impl.ui.model

internal sealed interface AccountDeletionError {
    data object Status : AccountDeletionError
    data object Send : AccountDeletionError
    data class Server(val message: String) : AccountDeletionError
}

internal data class AccountDeletionState(
    val email: String = "",
    val isLoading: Boolean = true,
    val isSending: Boolean = false,
    /** A deletion e-mail was issued (now or earlier). Before that the screen only asks for confirmation. */
    val requested: Boolean = false,
    val resendAvailable: Boolean = true,
    val resendAvailableAtUtc: String? = null,
    val error: AccountDeletionError? = null,
) {
    val canSend: Boolean get() = !isLoading && !isSending && (!requested || resendAvailable)
}

internal sealed interface AccountDeletionAction {
    /** The explicit confirmation, also used for a resend. */
    data object Send : AccountDeletionAction
    data object DismissError : AccountDeletionAction
}

internal sealed interface AccountDeletionEvent {
    data object SessionEnded : AccountDeletionEvent
}
