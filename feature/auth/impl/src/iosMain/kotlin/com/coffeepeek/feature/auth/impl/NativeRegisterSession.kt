package com.coffeepeek.feature.auth.impl

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.auth.api.AppleAuthorizer
import com.coffeepeek.feature.auth.api.AuthAnalytics
import com.coffeepeek.feature.auth.domain.AuthRepository
import com.coffeepeek.feature.auth.domain.EmailIssue
import com.coffeepeek.feature.auth.domain.MIN_REGISTRATION_NAME_LENGTH
import com.coffeepeek.feature.auth.domain.PasswordIssue
import com.coffeepeek.feature.auth.domain.RegistrationIssue
import com.coffeepeek.feature.auth.domain.SocialFailure
import com.coffeepeek.feature.auth.domain.canSubmitRegistration
import com.coffeepeek.feature.auth.impl.register.RegisterAction
import com.coffeepeek.feature.auth.impl.register.RegisterState
import com.coffeepeek.feature.auth.impl.register.RegisterStep
import com.coffeepeek.feature.auth.impl.register.RegisterViewModel
import kotlinx.coroutines.flow.map

/** A destination-owned registration session; the password stays in the ViewModel. */
class NativeRegisterSession(repository: AuthRepository, analytics: AuthAnalytics, apple: AppleAuthorizer?) {
    private val model = RegisterViewModel(repository, analytics, apple)
    private val store = ViewModelStore().apply { put("auth-register", model) }

    val appleAvailable = apple != null
    val state = model.state.map(RegisterState::snapshot)
    val signedIn = model.events.map { Unit }

    fun current() = model.state.value.snapshot()
    fun nameChanged(value: String) = model.onAction(RegisterAction.NameChanged(value))
    fun emailChanged(value: String) = model.onAction(RegisterAction.EmailChanged(value))
    fun passwordChanged(value: String) = model.onAction(RegisterAction.PasswordChanged(value))
    fun termsChanged(value: Boolean) = model.onAction(RegisterAction.TermsChanged(value))
    fun analyticsChanged(value: Boolean) = model.onAction(RegisterAction.AnalyticsChanged(value))
    fun submit() = model.onAction(RegisterAction.Submit)
    fun signInWithApple() = model.onAction(RegisterAction.AppleSignIn)
    fun googleToken(value: String) = model.onAction(RegisterAction.GoogleToken(value))
    fun googleFailure(message: String) = model.onAction(RegisterAction.GoogleFailed(message))
    fun close() = store.clear()
}

enum class NativeRegisterNameIssue { Required, TooShort }

data class NativeRegisterState(
    val name: String,
    val email: String,
    val termsAccepted: Boolean,
    val analyticsAccepted: Boolean,
    val nameIssue: NativeRegisterNameIssue?,
    val emailIssue: EmailIssue?,
    val emailTaken: Boolean,
    val passwordIssue: PasswordIssue?,
    val termsRequired: Boolean,
    val failureMessage: String?,
    val requestFailed: Boolean,
    val checkingEmail: Boolean,
    val socialFailure: SocialFailure?,
    val busy: Boolean,
    val success: Boolean,
    val canSubmit: Boolean,
)

private fun RegisterState.snapshot() = NativeRegisterState(
    name = name,
    email = email,
    termsAccepted = termsAccepted,
    analyticsAccepted = analyticsAccepted,
    nameIssue = nativeNameIssue(),
    emailIssue = emailIssue,
    emailTaken = emailTaken,
    passwordIssue = passwordIssue,
    termsRequired = !termsAccepted && canSubmitRegistration(name, password, true),
    failureMessage = failure?.message,
    requestFailed = failure != null,
    checkingEmail = failure?.checkingEmail == true,
    socialFailure = socialFailure,
    busy = busy,
    success = step == RegisterStep.Success,
    canSubmit = canSubmitRegistration(name, password, termsAccepted) && email.isNotBlank(),
)

private fun RegisterState.nativeNameIssue() = when {
    name.trim().length in 1 until MIN_REGISTRATION_NAME_LENGTH -> NativeRegisterNameIssue.TooShort
    reported == RegistrationIssue.NameRequired -> NativeRegisterNameIssue.Required
    else -> null
}
