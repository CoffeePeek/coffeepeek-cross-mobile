package com.coffeepeek.admin.di.auth

import com.coffeepeek.feature.auth.domain.EmailIssue
import com.coffeepeek.feature.auth.domain.LoginFailure
import com.coffeepeek.feature.auth.domain.PasswordIssue
import com.coffeepeek.feature.auth.domain.SocialFailure
import com.coffeepeek.feature.auth.domain.SocialFailureKind
import com.coffeepeek.feature.auth.domain.SocialProvider
import com.coffeepeek.feature.auth.impl.NativeLoginSession
import com.coffeepeek.feature.auth.impl.NativeLoginState
import com.coffeepeek.feature.settings.api.LanguagePort
import kotlinx.coroutines.flow.map

class IosLogin internal constructor(
    private val presentation: NativeLoginSession,
    language: LanguagePort,
    val googleAvailable: Boolean,
    back: () -> Unit,
    private val onForgotPassword: (String) -> Unit,
    private val onRegister: () -> Unit,
    private val onSignedIn: () -> Unit,
) : IosAuthScreen(language, back, presentation::close) {
    private val google = IosGoogleRequest(scope)
    val appleAvailable get() = presentation.appleAvailable

    fun currentState() = presentation.current().toIos()
    fun observeState(onState: (IosLoginState) -> Unit) = observe(presentation.state.map(NativeLoginState::toIos), onState)
    fun observeSignedIn() = observe(presentation.signedIn) { onSignedIn() }
    fun emailChanged(value: String) = presentation.emailChanged(value)
    fun passwordChanged(value: String) = presentation.passwordChanged(value)
    fun submit() = presentation.submit()
    fun signInWithApple() = presentation.signInWithApple()

    fun signInWithGoogle() {
        if (googleAvailable && !presentation.current().busy) google.launch(presentation::googleToken, presentation::googleFailure)
    }

    fun forgotPassword(email: String) = onForgotPassword(email)
    fun register() = onRegister()
}

data class IosLoginState(
    val email: String,
    val emailError: IosAuthMessage?,
    val passwordError: IosAuthMessage?,
    val failure: IosAuthMessage?,
    val busy: Boolean,
)

data class IosAuthMessage(
    val key: String,
    val minimumLength: Int = 0,
    val diagnosticCode: String? = null,
    val diagnosticDomain: String? = null,
    val diagnosticOAuth: String? = null,
)

private fun NativeLoginState.toIos() = IosLoginState(
    email = email,
    emailError = emailIssue?.let { IosAuthMessage(if (it == EmailIssue.Empty) "auth_email_required" else "auth_email_invalid") },
    passwordError = when (val issue = passwordIssue) {
        PasswordIssue.Empty -> IosAuthMessage("auth_password_required")
        is PasswordIssue.TooShort -> IosAuthMessage("auth_password_too_short", minimumLength = issue.minLength)
        null -> null
    },
    failure = socialFailure?.toIos() ?: failure?.let { IosAuthMessage(it.textKey()) },
    busy = busy,
)

private fun LoginFailure.textKey() = when (this) {
    LoginFailure.WrongCredentials -> "auth_login_wrong_credentials"
    LoginFailure.EmailNotConfirmed -> "auth_login_email_not_confirmed"
    LoginFailure.Network -> "auth_login_network"
    LoginFailure.Unknown -> "auth_login_unknown"
}

private fun SocialFailure.toIos() = IosAuthMessage(
    key = when (kind) {
        SocialFailureKind.AccountExists -> "auth_social_account_exists"
        SocialFailureKind.EmailNotVerified -> if (provider == SocialProvider.Apple) "auth_social_apple_email_not_verified" else "auth_social_email_not_verified"
        SocialFailureKind.Network -> "auth_login_network"
        SocialFailureKind.Generic -> if (provider == SocialProvider.Apple) "auth_social_apple_generic" else "auth_social_generic"
    },
    diagnosticCode = diagnostic?.code,
    diagnosticDomain = diagnostic?.domain,
    diagnosticOAuth = diagnostic?.oauthError,
)
