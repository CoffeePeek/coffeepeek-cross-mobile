package com.coffeepeek.admin.di.auth

import com.coffeepeek.feature.auth.impl.NativePasswordRecoverySession
import com.coffeepeek.feature.auth.impl.NativePasswordRecoveryState
import com.coffeepeek.feature.auth.impl.NativeRecoveryFailure
import com.coffeepeek.feature.settings.api.LanguagePort
import kotlinx.coroutines.flow.map

class IosPasswordRecovery internal constructor(
    private val presentation: NativePasswordRecoverySession,
    language: LanguagePort,
    back: () -> Unit,
    private val onBackToLogin: () -> Unit,
) : IosAuthScreen(language, back, presentation::close) {
    fun currentState() = presentation.current().toIos()
    fun observeState(onState: (IosPasswordRecoveryState) -> Unit) = observe(presentation.state.map(NativePasswordRecoveryState::toIos), onState)
    fun emailChanged(value: String) = presentation.emailChanged(value)
    fun submit() = presentation.submit()
    fun requestAnotherEmail() = presentation.requestAnotherEmail()
    fun login() = onBackToLogin()
}

data class IosPasswordRecoveryState(
    val email: String,
    val busy: Boolean,
    val emailSent: Boolean,
    val emailError: IosAuthMessage?,
    val failed: Boolean,
)

private fun NativePasswordRecoveryState.toIos() = IosPasswordRecoveryState(
    email, busy, emailSent,
    when (failure) {
        NativeRecoveryFailure.EmailRequired -> IosAuthMessage("auth_email_required")
        NativeRecoveryFailure.EmailInvalid -> IosAuthMessage("auth_email_invalid")
        else -> null
    },
    failed = failure == NativeRecoveryFailure.SendingFailed,
)
