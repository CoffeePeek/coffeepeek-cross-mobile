package com.coffeepeek.admin.di.auth

import androidx.compose.runtime.Composable
import com.coffeepeek.BuildConfig
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.screen.auth.AuthScreenRenderer
import com.coffeepeek.feature.auth.api.AuthAnalytics
import com.coffeepeek.feature.auth.api.GoogleSignInConfig
import com.coffeepeek.feature.auth.api.LoginEntry
import com.coffeepeek.feature.auth.api.PasswordRecoveryEntry
import com.coffeepeek.feature.auth.api.RegisterEntry
import com.coffeepeek.feature.auth.domain.AuthRepository
import com.coffeepeek.feature.auth.impl.createLoginEntry
import com.coffeepeek.feature.auth.impl.createPasswordRecoveryEntry
import com.coffeepeek.feature.auth.impl.createRegisterEntry

/** Android routes sign-in, registration and recovery to the migrated entries; where each button leads stays with the navigator. */
internal class AndroidAuthScreenRenderer(
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
    )

    @Composable
    override fun Register() = register.Content(
        onClose = Navigator::closeAuth,
        onGoToLogin = { Navigator.popThenNavigate(Navigator.Screen.Auth) },
        onSignedIn = { Navigator.navigate(Navigator.Screen.Main) },
    )

    @Composable
    override fun Recovery(initialEmail: String, onClose: () -> Unit, onBackToLogin: () -> Unit) =
        recovery.Content(initialEmail, onClose, onBackToLogin)
}

internal object AppGoogleSignInConfig : GoogleSignInConfig {
    override val webClientId: String get() = BuildConfig.GOOGLE_WEB_CLIENT_ID
}

internal fun androidAuthScreenRenderer(repository: AuthRepository, analytics: AuthAnalytics): AuthScreenRenderer =
    AndroidAuthScreenRenderer(
        login = createLoginEntry(repository, analytics, AppGoogleSignInConfig),
        register = createRegisterEntry(repository, analytics, AppGoogleSignInConfig),
        recovery = createPasswordRecoveryEntry(repository),
    )
