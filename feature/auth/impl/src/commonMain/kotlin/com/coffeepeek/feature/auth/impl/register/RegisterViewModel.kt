package com.coffeepeek.feature.auth.impl.register

import com.coffeepeek.core.presentation.MviViewModel
import com.coffeepeek.feature.auth.api.AuthAnalytics
import com.coffeepeek.feature.auth.api.AuthEntry
import com.coffeepeek.feature.auth.api.SignInMethod
import com.coffeepeek.feature.auth.domain.AuthRepository
import com.coffeepeek.feature.auth.domain.MIN_REGISTRATION_PASSWORD_LENGTH
import com.coffeepeek.feature.auth.domain.SocialProvider
import com.coffeepeek.feature.auth.domain.checkRegistration
import com.coffeepeek.feature.auth.domain.classifySocialFailure
import com.coffeepeek.feature.auth.domain.validateEmail
import com.coffeepeek.feature.auth.domain.validatePassword
import kotlinx.coroutines.CancellationException

/**
 * Registration as the legacy screens did it: e-mail check, then name/password/terms, then the "check your mail" screen.
 * The analytics choice is stored before the account is created and the password sign-up is remembered after it.
 */
internal class RegisterViewModel(
    private val repository: AuthRepository,
    private val analytics: AuthAnalytics,
) : MviViewModel<RegisterState, RegisterAction, RegisterEvent>(RegisterState()) {

    override suspend fun handleActionInternal(action: RegisterAction) {
        when (action) {
            is RegisterAction.NameChanged -> updateState { copy(name = action.value, reported = null, failure = null, socialFailure = null) }
            is RegisterAction.EmailChanged -> updateState {
                copy(email = action.value, emailIssue = null, emailTaken = false, failure = null, socialFailure = null)
            }
            is RegisterAction.PasswordChanged -> updateState {
                copy(
                    password = action.value,
                    reported = null,
                    failure = null,
                    passwordIssue = passwordIssue?.let { validatePassword(action.value, MIN_REGISTRATION_PASSWORD_LENGTH) },
                )
            }
            is RegisterAction.TermsChanged -> updateState { copy(termsAccepted = action.checked, reported = null) }
            is RegisterAction.AnalyticsChanged -> updateState { copy(analyticsAccepted = action.checked) }
            RegisterAction.ChangeEmail -> updateState { copy(step = RegisterStep.Email, passwordIssue = null, reported = null, failure = null) }
            RegisterAction.ContinueWithEmail -> continueWithEmail()
            RegisterAction.Submit -> submit()
            is RegisterAction.GoogleToken -> googleSignUp(action.idToken)
            is RegisterAction.GoogleFailed -> updateState {
                copy(socialFailure = classifySocialFailure(SocialProvider.Google, action.message), failure = null)
            }
        }
    }

    private suspend fun continueWithEmail() {
        if (currentState.busy) return
        val email = currentState.email.trim()
        val issue = validateEmail(email)
        if (issue != null) {
            updateState { copy(emailIssue = issue, failure = null) }
            return
        }
        updateState { copy(busy = true, failure = null) }
        try {
            repository.isEmailTaken(email).fold(
                onSuccess = { taken ->
                    updateState {
                        if (taken) copy(emailTaken = true, emailIssue = null) else copy(emailTaken = false, emailIssue = null, step = RegisterStep.Details)
                    }
                },
                onFailure = { error -> updateState { copy(failure = failureOf(error)) } },
            )
        } finally {
            updateState { copy(busy = false) }
        }
    }

    private suspend fun submit() {
        if (currentState.busy) return
        val name = currentState.name.trim()
        val email = currentState.email.trim()
        val password = currentState.password
        val check = checkRegistration(name, email, password, currentState.termsAccepted)
        if (!check.valid) {
            updateState { copy(passwordIssue = check.password, emailIssue = check.email, reported = check.reported, failure = null) }
            return
        }
        updateState { copy(busy = true, reported = null, failure = null) }
        try {
            analytics.saveAnalyticsConsent(currentState.analyticsAccepted)
            repository.register(name, email, password).fold(
                onSuccess = {
                    analytics.passwordSignUpCompleted(email)
                    updateState { copy(step = RegisterStep.Success) }
                },
                onFailure = { error -> updateState { copy(failure = failureOf(error)) } },
            )
        } finally {
            updateState { copy(busy = false) }
        }
    }

    private suspend fun googleSignUp(idToken: String) {
        if (currentState.busy) return
        updateState { copy(busy = true, failure = null, socialFailure = null) }
        try {
            repository.signInWithGoogle(idToken).fold(
                onSuccess = { account ->
                    analytics.signedIn(account.userId, SignInMethod.Google, AuthEntry.Register)
                    sendEvent(RegisterEvent.SignedIn)
                },
                onFailure = { error -> updateState { copy(socialFailure = classifySocialFailure(SocialProvider.Google, error.message)) } },
            )
        } finally {
            updateState { copy(busy = false) }
        }
    }

    private fun failureOf(error: Throwable): RequestFailure {
        if (error is CancellationException) throw error
        return RequestFailure(error.message?.takeIf(String::isNotBlank))
    }
}
