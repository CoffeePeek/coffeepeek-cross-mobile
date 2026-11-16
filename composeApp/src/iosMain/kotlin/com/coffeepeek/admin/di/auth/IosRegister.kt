package com.coffeepeek.admin.di.auth

import com.coffeepeek.feature.auth.domain.EmailIssue
import com.coffeepeek.feature.auth.domain.MIN_REGISTRATION_NAME_LENGTH
import com.coffeepeek.feature.auth.domain.PasswordIssue
import com.coffeepeek.feature.auth.impl.NativeRegisterNameIssue
import com.coffeepeek.feature.auth.impl.NativeRegisterSession
import com.coffeepeek.feature.auth.impl.NativeRegisterState
import com.coffeepeek.feature.settings.api.LanguagePort
import kotlinx.coroutines.flow.map

class IosRegister internal constructor(
    private val presentation: NativeRegisterSession,
    language: LanguagePort,
    val googleAvailable: Boolean,
    back: () -> Unit,
    private val onLogin: () -> Unit,
    private val onSignedIn: () -> Unit,
    private val onTerms: () -> Unit,
    private val onPrivacy: () -> Unit,
) : IosAuthScreen(language, back, presentation::close) {
    private val google = IosGoogleRequest(scope)
    val appleAvailable get() = presentation.appleAvailable

    fun currentState() = presentation.current().toIos()
    fun observeState(onState: (IosRegisterState) -> Unit) = observe(presentation.state.map(NativeRegisterState::toIos), onState)
    fun observeSignedIn() = observe(presentation.signedIn) { onSignedIn() }
    fun nameChanged(value: String) = presentation.nameChanged(value)
    fun emailChanged(value: String) = presentation.emailChanged(value)
    fun passwordChanged(value: String) = presentation.passwordChanged(value)
    fun termsChanged(value: Boolean) = presentation.termsChanged(value)
    fun analyticsChanged(value: Boolean) = presentation.analyticsChanged(value)
    fun submit() = presentation.submit()
    fun signInWithApple() = presentation.signInWithApple()

    fun signInWithGoogle() {
        val state = presentation.current()
        if (googleAvailable && !state.busy && !state.success) google.launch(presentation::googleToken, presentation::googleFailure)
    }

    fun login() = onLogin()
    fun openTerms() = onTerms()
    fun openPrivacy() = onPrivacy()
}

data class IosRegisterState(
    val name: String,
    val email: String,
    val termsAccepted: Boolean,
    val analyticsAccepted: Boolean,
    val nameError: IosAuthMessage?,
    val emailError: IosAuthMessage?,
    val passwordError: IosAuthMessage?,
    val termsError: IosAuthMessage?,
    val failure: IosAuthMessage?,
    val failureText: String?,
    val busy: Boolean,
    val canSubmit: Boolean,
    val success: Boolean,
)

private fun NativeRegisterState.toIos() = IosRegisterState(
    name = name,
    email = email,
    termsAccepted = termsAccepted,
    analyticsAccepted = analyticsAccepted,
    nameError = nameIssue?.toIos(),
    emailError = emailError(),
    passwordError = passwordIssue?.registerMessage(),
    termsError = if (termsRequired) IosAuthMessage("auth_terms_required") else null,
    failure = socialFailure?.toIos() ?: requestFailure(),
    failureText = failureMessage,
    busy = busy,
    canSubmit = canSubmit,
    success = success,
)

private fun NativeRegisterNameIssue.toIos() = when (this) {
    NativeRegisterNameIssue.Required -> IosAuthMessage("auth_name_required")
    NativeRegisterNameIssue.TooShort -> IosAuthMessage("auth_name_too_short", minimumLength = MIN_REGISTRATION_NAME_LENGTH)
}

private fun NativeRegisterState.emailError() = when {
    emailTaken -> IosAuthMessage("auth_email_taken")
    emailIssue == EmailIssue.Empty -> IosAuthMessage("auth_email_required")
    emailIssue == EmailIssue.Invalid -> IosAuthMessage("auth_email_invalid")
    else -> null
}

private fun PasswordIssue.registerMessage() = when (this) {
    PasswordIssue.Empty -> IosAuthMessage("auth_password_required")
    is PasswordIssue.TooShort -> IosAuthMessage("auth_password_too_short", minimumLength = minLength)
}

private fun NativeRegisterState.requestFailure() = when {
    !requestFailed -> null
    checkingEmail -> IosAuthMessage("auth_check_failed")
    else -> IosAuthMessage("auth_register_failed")
}
