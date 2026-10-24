package com.coffeepeek.feature.auth.impl

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coffeepeek.feature.auth.api.AuthAnalytics
import com.coffeepeek.feature.auth.api.GoogleSignInLauncher
import com.coffeepeek.feature.auth.api.LoginEntry
import com.coffeepeek.feature.auth.api.PasswordRecoveryEntry
import com.coffeepeek.feature.auth.api.RegisterEntry
import com.coffeepeek.feature.auth.domain.AuthRepository
import com.coffeepeek.feature.auth.impl.login.LoginViewModel
import com.coffeepeek.feature.auth.impl.recovery.PasswordRecoveryViewModel
import com.coffeepeek.feature.auth.impl.register.RegisterViewModel
import com.coffeepeek.feature.auth.impl.ui.LoginScreen
import com.coffeepeek.feature.auth.impl.ui.PasswordRecoveryScreen
import com.coffeepeek.feature.auth.impl.ui.RegisterScreen

/** «Вход в аккаунт». The ViewModel lives in the destination's store. */
fun createLoginEntry(repository: AuthRepository, analytics: AuthAnalytics, google: GoogleSignInLauncher?): LoginEntry = object : LoginEntry {
    @Composable
    override fun Content(
        onClose: () -> Unit,
        onSignedIn: () -> Unit,
        onForgotPassword: (String) -> Unit,
        onCreateAccount: () -> Unit,
        modifier: Modifier,
    ) {
        LoginScreen(viewModel(key = "auth-login") { LoginViewModel(repository, analytics) }, onClose, onSignedIn, onForgotPassword, onCreateAccount, google, modifier)
    }
}

/** «Восстановление пароля». */
fun createPasswordRecoveryEntry(repository: AuthRepository): PasswordRecoveryEntry = object : PasswordRecoveryEntry {
    @Composable
    override fun Content(initialEmail: String, onClose: () -> Unit, onBackToLogin: () -> Unit, modifier: Modifier) {
        PasswordRecoveryScreen(
            viewModel(key = "auth-recovery") { PasswordRecoveryViewModel(initialEmail, repository) },
            onClose, onBackToLogin, modifier,
        )
    }
}

/** «Регистрация»: e-mail check, details, "check your mail". */
fun createRegisterEntry(repository: AuthRepository, analytics: AuthAnalytics, google: GoogleSignInLauncher?): RegisterEntry = object : RegisterEntry {
    @Composable
    override fun Content(onClose: () -> Unit, onGoToLogin: () -> Unit, onSignedIn: () -> Unit, modifier: Modifier) {
        RegisterScreen(viewModel(key = "auth-register") { RegisterViewModel(repository, analytics) }, onClose, onGoToLogin, onSignedIn, google, modifier)
    }
}
