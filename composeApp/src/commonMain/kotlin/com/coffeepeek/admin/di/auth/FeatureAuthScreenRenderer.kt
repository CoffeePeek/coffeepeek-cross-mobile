package com.coffeepeek.admin.di.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.screen.auth.AuthScreenRenderer
import com.coffeepeek.core.designsystem.component.CoffeePeekBackHandler
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

    // Session navigation resumes the scanned shop when sign-in changes the root account state.
    @Composable
    override fun CheckInLogin(onClose: () -> Unit) {
        var step by rememberSaveable { mutableStateOf(CheckInAuthStep.Login) }
        var email by rememberSaveable { mutableStateOf("") }
        val backToLogin = { step = CheckInAuthStep.Login }
        CoffeePeekBackHandler(enabled = step != CheckInAuthStep.Login) { backToLogin() }
        when (step) {
            CheckInAuthStep.Login -> login.Content(
                onClose = onClose, onSignedIn = {},
                onForgotPassword = { email = it; step = CheckInAuthStep.Recovery },
                onCreateAccount = { step = CheckInAuthStep.Register }, modifier = Modifier,
            )
            CheckInAuthStep.Register -> register.Content(
                onClose = onClose, onGoToLogin = backToLogin, onSignedIn = {}, modifier = Modifier,
            )
            CheckInAuthStep.Recovery -> recovery.Content(email, backToLogin, backToLogin, Modifier)
        }
    }

    private enum class CheckInAuthStep { Login, Register, Recovery }

    @Composable
    override fun Recovery(initialEmail: String, onClose: () -> Unit, onBackToLogin: () -> Unit) =
        recovery.Content(initialEmail, onClose, onBackToLogin, Modifier)
}
