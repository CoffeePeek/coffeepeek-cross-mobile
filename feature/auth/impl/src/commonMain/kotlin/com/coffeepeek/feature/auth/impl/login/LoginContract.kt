package com.coffeepeek.feature.auth.impl.login

import com.coffeepeek.feature.auth.domain.EmailIssue
import com.coffeepeek.feature.auth.domain.LoginFailure
import com.coffeepeek.feature.auth.domain.PasswordIssue

internal data class LoginState(
    val email: String = "",
    val password: String = "",
    val emailIssue: EmailIssue? = null,
    val passwordIssue: PasswordIssue? = null,
    /** Why the last attempt failed; cleared by any edit. */
    val failure: LoginFailure? = null,
    /** The primary button shows a loader and the form ignores a second submit. */
    val signingIn: Boolean = false,
)

internal sealed interface LoginAction {
    data class EmailChanged(val value: String) : LoginAction
    data class PasswordChanged(val value: String) : LoginAction
    data object Submit : LoginAction
}

internal sealed interface LoginEvent {
    data object SignedIn : LoginEvent
}
