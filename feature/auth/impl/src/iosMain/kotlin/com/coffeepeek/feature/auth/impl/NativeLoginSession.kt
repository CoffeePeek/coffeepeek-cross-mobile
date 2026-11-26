package com.coffeepeek.feature.auth.impl

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.auth.api.AppleAuthorizer
import com.coffeepeek.feature.auth.api.AuthAnalytics
import com.coffeepeek.feature.auth.domain.AuthRepository
import com.coffeepeek.feature.auth.domain.EmailIssue
import com.coffeepeek.feature.auth.domain.LoginFailure
import com.coffeepeek.feature.auth.domain.PasswordIssue
import com.coffeepeek.feature.auth.domain.SocialFailure
import com.coffeepeek.feature.auth.impl.login.LoginAction
import com.coffeepeek.feature.auth.impl.login.LoginState
import com.coffeepeek.feature.auth.impl.login.LoginViewModel
import kotlinx.coroutines.flow.map

/** One destination's login actions and state. The password stays in the ViewModel. */
class NativeLoginSession(
    repository: AuthRepository,
    analytics: AuthAnalytics,
    apple: AppleAuthorizer?,
) {
    private val model = LoginViewModel(repository, analytics, apple)
    private val store = ViewModelStore().apply { put("auth-login", model) }

    val appleAvailable = apple != null
    val state = model.state.map(LoginState::snapshot)
    val signedIn = model.events.map { Unit }

    fun current() = model.state.value.snapshot()
    fun emailChanged(value: String) = model.onAction(LoginAction.EmailChanged(value))
    fun passwordChanged(value: String) = model.onAction(LoginAction.PasswordChanged(value))
    fun submit() = model.onAction(LoginAction.Submit)
    fun signInWithApple() = model.onAction(LoginAction.AppleSignIn)
    fun googleToken(value: String) = model.onAction(LoginAction.GoogleToken(value))
    fun googleFailure(message: String) = model.onAction(LoginAction.GoogleFailed(message))
    fun close() = store.clear()
}

data class NativeLoginState(
    val email: String,
    val emailIssue: EmailIssue?,
    val passwordIssue: PasswordIssue?,
    val failure: LoginFailure?,
    val socialFailure: SocialFailure?,
    val busy: Boolean,
)

private fun LoginState.snapshot() = NativeLoginState(email, emailIssue, passwordIssue, failure, socialFailure, signingIn)
