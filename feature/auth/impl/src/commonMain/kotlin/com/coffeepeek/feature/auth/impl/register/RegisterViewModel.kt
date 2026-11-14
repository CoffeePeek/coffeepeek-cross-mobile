package com.coffeepeek.feature.auth.impl.register

import com.coffeepeek.core.presentation.MviViewModel
import com.coffeepeek.feature.auth.api.AuthAnalytics
import com.coffeepeek.feature.auth.api.AppleAuthorizer
import com.coffeepeek.feature.auth.api.AuthEntry
import com.coffeepeek.feature.auth.api.SignInMethod
import com.coffeepeek.feature.auth.domain.AuthRepository
import com.coffeepeek.feature.auth.domain.SignedInAccount
import com.coffeepeek.feature.auth.impl.signInWithApple
import com.coffeepeek.feature.auth.domain.MIN_REGISTRATION_PASSWORD_LENGTH
import com.coffeepeek.feature.auth.domain.SocialProvider
import com.coffeepeek.feature.auth.domain.checkRegistration
import com.coffeepeek.feature.auth.domain.classifySocialFailure
import com.coffeepeek.feature.auth.domain.validateEmail
import com.coffeepeek.feature.auth.domain.validatePassword
import kotlinx.coroutines.CancellationException

/**
 * Validates the form, checks e-mail availability and creates the account before showing email confirmation.
 * The analytics choice is stored before account creation and the password sign-up is remembered after it.
 */
internal class RegisterViewModel(
    private val repository: AuthRepository,
    private val analytics: AuthAnalytics,
    private val apple: AppleAuthorizer? = null,
) : MviViewModel<RegisterState, RegisterAction, RegisterEvent>(RegisterState()) {

    override suspend fun handleActionInternal(action: RegisterAction) {
        // While a request is running the form is frozen: edits, consent and "change e-mail" must not change what is being sent or move the step.
        if (currentState.busy || currentState.step == RegisterStep.Success) return
        when (action) {
            is RegisterAction.NameChanged -> updateState { copy(name = action.value, reported = null, failure = null, socialFailure = null) }
            is RegisterAction.EmailChanged -> changeEmail(action.value)
            is RegisterAction.PasswordChanged -> changePassword(action.value)
            is RegisterAction.TermsChanged -> updateState { copy(termsAccepted = action.checked, reported = null) }
            is RegisterAction.AnalyticsChanged -> updateState { copy(analyticsAccepted = action.checked) }
            RegisterAction.Submit -> submit()
            RegisterAction.AppleSignIn -> appleSignIn()
            is RegisterAction.GoogleToken -> socialSignIn(SignInMethod.Google, SocialProvider.Google) { repository.signInWithGoogle(action.idToken) }
            is RegisterAction.GoogleFailed -> googleFailed(action.message)
        }
    }

    private fun changeEmail(value: String) = updateState {
        copy(
            email = value,
            emailIssue = emailIssue?.let { validateEmail(value) },
            emailTaken = emailTaken && value.trim().equals(email.trim(), ignoreCase = true),
            reported = null,
            failure = null,
            socialFailure = null,
        )
    }

    private fun changePassword(value: String) = updateState {
        copy(
            password = value,
            reported = null,
            failure = null,
            passwordIssue = passwordIssue?.let { validatePassword(value, MIN_REGISTRATION_PASSWORD_LENGTH) },
        )
    }

    private fun googleFailed(message: String) = updateState {
        copy(socialFailure = classifySocialFailure(SocialProvider.Google, message), failure = null)
    }

    private suspend fun emailIsAvailable(email: String): Boolean {
        val taken = repository.isEmailTaken(email).getOrElse { error ->
            val failure = failureOf(error).copy(checkingEmail = true)
            updateState { copy(failure = failure) }
            return false
        }
        if (taken) updateState { copy(emailTaken = true) }
        return !taken
    }

    private suspend fun submit() {
        val form = currentState
        val name = form.name.trim()
        val email = form.email.trim()
        val check = checkRegistration(name, email, form.password, form.termsAccepted)
        if (!check.valid) {
            updateState { copy(passwordIssue = check.password, emailIssue = check.email, reported = check.reported, failure = null) }
            return
        }
        updateState { copy(busy = true, emailTaken = false, emailIssue = null, passwordIssue = null,
            reported = null, failure = null, socialFailure = null) }
        try {
            if (emailIsAvailable(email)) register(name, email, form)
        } finally {
            updateState { copy(busy = false) }
        }
    }

    private suspend fun register(name: String, email: String, form: RegisterState) {
        analytics.saveAnalyticsConsent(form.analyticsAccepted)
        repository.register(name, email, form.password).fold(
            onSuccess = {
                analytics.passwordSignUpCompleted(email)
                updateState { copy(step = RegisterStep.Success, email = email, password = "") }
            },
            onFailure = { error -> updateState { copy(failure = failureOf(error)) } },
        )
    }

    private suspend fun appleSignIn() {
        val authorizer = apple ?: return
        socialSignIn(SignInMethod.Apple, SocialProvider.Apple) { signInWithApple(repository, authorizer) }
    }

    private suspend fun socialSignIn(method: SignInMethod, provider: SocialProvider, request: suspend () -> Result<SignedInAccount>) {
        if (currentState.busy) return
        updateState { copy(busy = true, failure = null, socialFailure = null) }
        try {
            request().fold(
                onSuccess = { account ->
                    analytics.signedIn(account.userId, method, AuthEntry.Register)
                    sendEvent(RegisterEvent.SignedIn)
                },
                onFailure = { error -> updateState { copy(socialFailure = classifySocialFailure(provider, error.message)) } },
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
