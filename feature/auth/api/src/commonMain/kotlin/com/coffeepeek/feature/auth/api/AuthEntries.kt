package com.coffeepeek.feature.auth.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** «Вход в аккаунт». The caller owns navigation and the destination's ViewModel store. */
interface LoginEntry {
    @Composable
    fun Content(
        onClose: () -> Unit,
        onSignedIn: () -> Unit,
        onForgotPassword: (email: String) -> Unit,
        onCreateAccount: () -> Unit,
        modifier: Modifier,
    )
}

/** «Восстановление пароля»: asks the server to e-mail a reset link; the reset itself happens on the web. */
interface PasswordRecoveryEntry {
    @Composable
    fun Content(initialEmail: String, onClose: () -> Unit, onBackToLogin: () -> Unit, modifier: Modifier)
}

/** «Регистрация»: three steps in one destination. [onGoToLogin] serves both the footer link and the final button; [onSignedIn] is a Google sign-up. */
interface RegisterEntry {
    @Composable
    fun Content(onClose: () -> Unit, onGoToLogin: () -> Unit, onSignedIn: () -> Unit, modifier: Modifier)
}
