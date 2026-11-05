package com.coffeepeek.feature.auth.impl.login

import com.coffeepeek.core.presentation.MviViewModel
import com.coffeepeek.feature.auth.api.AuthAnalytics
import com.coffeepeek.feature.auth.api.AppleAuthorizer
import com.coffeepeek.feature.auth.api.AuthEntry
import com.coffeepeek.feature.auth.api.SignInMethod
import com.coffeepeek.feature.auth.domain.AuthRepository
import com.coffeepeek.feature.auth.domain.SignedInAccount
import com.coffeepeek.feature.auth.impl.signInWithApple
import com.coffeepeek.feature.auth.domain.MIN_LOGIN_PASSWORD_LENGTH
import com.coffeepeek.feature.auth.domain.SocialProvider
import com.coffeepeek.feature.auth.domain.classifySocialFailure
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
    private val apple: AppleAuthorizer? = null,
) : MviViewModel<LoginState, LoginAction, LoginEvent>(LoginState()) {

    override suspend fun handleActionInternal(action: LoginAction) {
        if (currentState.signingIn) return
        when (action) {
            is LoginAction.EmailChanged -> updateState {
                copy(email = action.value, failure = null, socialFailure = null, emailIssue = emailIssue?.let { validateEmail(action.value) })
            }
            is LoginAction.PasswordChanged -> updateState {
                copy(password = action.value, failure = null, socialFailure = null, passwordIssue = passwordIssue?.let { validatePassword(action.value, MIN_LOGIN_PASSWORD_LENGTH) })
            }
            LoginAction.Submit -> submit()
            LoginAction.AppleSignIn -> appleSignIn()
            is LoginAction.GoogleToken -> socialSignIn(SignInMethod.Google, SocialProvider.Google) { repository.signInWithGoogle(action.idToken) }
            is LoginAction.GoogleFailed -> updateState {
                copy(socialFailure = classifySocialFailure(SocialProvider.Google, action.message), failure = null)
            }
        }
    }

    private suspend fun submit() {
        if (currentState.signingIn) return
        val email = currentState.email.trim()
        val password = currentState.password
        val emailIssue = validateEmail(email)
        val passwordIssue = validatePassword(password, MIN_LOGIN_PASSWORD_LENGTH)
        updateState { copy(emailIssue = emailIssue, passwordIssue = passwordIssue, failure = null, socialFailure = null) }
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

    private suspend fun appleSignIn() {
        val authorizer = apple ?: return
        socialSignIn(SignInMethod.Apple, SocialProvider.Apple) { signInWithApple(repository, authorizer) }
    }

    private suspend fun socialSignIn(method: SignInMethod, provider: SocialProvider, request: suspend () -> Result<SignedInAccount>) {
        if (currentState.signingIn) return
        updateState { copy(signingIn = true, failure = null, socialFailure = null) }
        try {
            request().fold(
                onSuccess = { account ->
                    analytics.signedIn(account.userId, method, AuthEntry.Login)
                    sendEvent(LoginEvent.SignedIn)
                },
                onFailure = { error -> updateState { copy(socialFailure = classifySocialFailure(provider, error.message)) } },
            )
        } finally {
            updateState { copy(signingIn = false) }
        }
    }
}
