package com.coffeepeek.admin.di.auth

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.screen.auth.AuthScreenRenderer
import com.coffeepeek.feature.auth.api.LoginEntry
import com.coffeepeek.feature.auth.api.PasswordRecoveryEntry
import com.coffeepeek.feature.auth.api.RegisterEntry

/** Routes sign-in, registration and recovery to the migrated entries; where each button leads stays with the navigator. */
internal class FeatureAuthScreenRenderer(
    private val login: LoginEntry,
    private val register: RegisterEntry,
    private val recovery: PasswordRecoveryEntry,
) : AuthScreenRenderer {
    @Composable
    override fun Login() = login.Content(
        onClose = Navigator::closeAuth,
        onSignedIn = { Navigator.navigate(Navigator.Screen.Main) },
        onForgotPassword = { email -> Navigator.navigate(Navigator.Screen.ForgotPassword(email)) },
        onCreateAccount = { Navigator.popThenNavigate(Navigator.Screen.Register) },
        modifier = Modifier,
    )

    @Composable
    override fun Register() = register.Content(
        onClose = Navigator::closeAuth,
        onGoToLogin = { Navigator.popThenNavigate(Navigator.Screen.Auth) },
        onSignedIn = { Navigator.navigate(Navigator.Screen.Main) },
        modifier = Modifier,
    )

    @Composable
    override fun Recovery(initialEmail: String, onClose: () -> Unit, onBackToLogin: () -> Unit) =
        recovery.Content(initialEmail, onClose, onBackToLogin, Modifier)
}
