package com.coffeepeek.feature.auth.impl.login

import com.coffeepeek.core.presentation.MviViewModel
import com.coffeepeek.feature.auth.api.AuthAnalytics
import com.coffeepeek.feature.auth.api.AuthEntry
import com.coffeepeek.feature.auth.api.SignInMethod
import com.coffeepeek.feature.auth.domain.AuthRepository
import com.coffeepeek.feature.auth.domain.MIN_LOGIN_PASSWORD_LENGTH
import com.coffeepeek.feature.auth.domain.classifyLoginFailure
import com.coffeepeek.feature.auth.domain.validateEmail
import com.coffeepeek.feature.auth.domain.validatePassword

/**
 * E-mail sign-in as the legacy screen did it: both fields are checked at once, the e-mail is trimmed and the password sent as typed,
 * a shown field error is validated live while it is shown, and the request error is cleared by any edit.
 */
internal class LoginViewModel(
    private val repository: AuthRepository,
    private val analytics: AuthAnalytics,
) : MviViewModel<LoginState, LoginAction, LoginEvent>(LoginState()) {

    override suspend fun handleActionInternal(action: LoginAction) {
        when (action) {
            is LoginAction.EmailChanged -> updateState {
                copy(email = action.value, failure = null, emailIssue = emailIssue?.let { validateEmail(action.value) })
            }
            is LoginAction.PasswordChanged -> updateState {
                copy(password = action.value, failure = null, passwordIssue = passwordIssue?.let { validatePassword(action.value, MIN_LOGIN_PASSWORD_LENGTH) })
            }
            LoginAction.Submit -> submit()
        }
    }

    private suspend fun submit() {
        if (currentState.signingIn) return
        val email = currentState.email.trim()
        val password = currentState.password
        val emailIssue = validateEmail(email)
        val passwordIssue = validatePassword(password, MIN_LOGIN_PASSWORD_LENGTH)
        updateState { copy(emailIssue = emailIssue, passwordIssue = passwordIssue, failure = null) }
        if (emailIssue != null || passwordIssue != null) return

        updateState { copy(signingIn = true) }
        try {
            repository.signIn(email, password).fold(
                onSuccess = { account ->
                    analytics.signedIn(account.userId, SignInMethod.Password, AuthEntry.Login, email)
                    sendEvent(LoginEvent.SignedIn)
                },
                onFailure = { error -> updateState { copy(failure = classifyLoginFailure(error.message)) } },
            )
        } finally {
            updateState { copy(signingIn = false) }
        }
    }
}
