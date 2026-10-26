package com.coffeepeek.admin.ui.screen.auth

import androidx.compose.runtime.Composable
import com.coffeepeek.admin.feature.passwordrecovery.ui.PasswordRecoveryScreen
import com.coffeepeek.admin.ui.screen.auth.registr.RegisterScreen

/** Platform selection only. Shared/iOS keep the legacy sign-in, registration and recovery screens; Android composes the migrated entries. */
interface AuthScreenRenderer {
    @Composable
    fun Login()

    @Composable
    fun Register()

    @Composable
    fun Recovery(initialEmail: String, onClose: () -> Unit, onBackToLogin: () -> Unit)
}

internal object LegacyAuthScreenRenderer : AuthScreenRenderer {
    @Composable
    override fun Login() = AuthScreen()

    @Composable
    override fun Register() = RegisterScreen()

    @Composable
    override fun Recovery(initialEmail: String, onClose: () -> Unit, onBackToLogin: () -> Unit) =
        PasswordRecoveryScreen(initialEmail = initialEmail, onClose = onClose, onLogin = onBackToLogin)
}
