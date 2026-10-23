package com.coffeepeek.admin.ui.screen.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.auth_email_placeholder
import coffeepeek.composeapp.generated.resources.create_account
import coffeepeek.composeapp.generated.resources.forgot_password
import coffeepeek.composeapp.generated.resources.login
import coffeepeek.composeapp.generated.resources.login_subtitle
import coffeepeek.composeapp.generated.resources.login_title
import coffeepeek.composeapp.generated.resources.password
import coffeepeek.composeapp.generated.resources.step_email
import com.coffeepeek.admin.auth.AppleAuth
import com.coffeepeek.admin.auth.handleGoogleSignInResult
import com.coffeepeek.admin.auth.isGoogleSignInConfigured
import com.coffeepeek.admin.di.platformViewModel
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.icons.CpIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

private data class LoginFormState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val loginError: String? = null,
)

object AuthScreen {
    @Composable
    operator fun invoke(vm: AuthViewModel = platformViewModel()) {
        val email by vm.email.collectAsState()
        val password by vm.password.collectAsState()
        val emailError by vm.emailError.collectAsState()
        val passwordError by vm.passwordError.collectAsState()
        val loginError by vm.loginError.collectAsState()
        val appleSigningIn by vm.appleSigningIn.collectAsState()
        AuthScreenContent(
            state = LoginFormState(email, password, emailError, passwordError, loginError),
            onEmailChange = vm::onEmailChange,
            onPasswordChange = vm::onPasswordChange,
            onLogin = vm::onLoginClick,
            onClose = Navigator::closeAuth,
            onForgotPassword = { Navigator.navigate(Navigator.Screen.ForgotPassword(email)) },
            onRegister = { Navigator.popThenNavigate(Navigator.Screen.Register) },
            socialSignIn = if (AppleAuth.isSupported() || isGoogleSignInConfigured()) {
                {
                    AuthSocialSignInButtons(
                        onAppleClick = vm::onAppleLogin,
                        appleEnabled = !appleSigningIn,
                        onGoogleResult = { result ->
                            handleGoogleSignInResult(result, vm::onGoogleLogin, vm::onGoogleLoginError)
                        },
                    )
                }
            } else null,
        )
    }
}

@Composable
private fun AuthScreenContent(
    state: LoginFormState,
    onEmailChange: (String) -> Unit = {},
    onPasswordChange: (String) -> Unit = {},
    onLogin: () -> Unit = {},
    onClose: () -> Unit = {},
    onForgotPassword: () -> Unit = {},
    onRegister: () -> Unit = {},
    socialSignIn: (@Composable () -> Unit)? = null,
) {
    AuthScreenScaffold(onClose = onClose) {
        AuthHeading(stringResource(Res.string.login_title), stringResource(Res.string.login_subtitle))
        if (socialSignIn != null) {
            socialSignIn()
            AuthOrDivider()
        }
        LoginFields(state, onEmailChange, onPasswordChange)
        state.loginError?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        AuthFooterRow(null, onForgotPassword, stringResource(Res.string.forgot_password))
        AuthPrimaryButton(stringResource(Res.string.login), onLogin)
        AuthFooterRow(null, onRegister, stringResource(Res.string.create_account))
    }
}

@Composable
private fun LoginFields(state: LoginFormState, onEmailChange: (String) -> Unit, onPasswordChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(CpDimens.spacing4)) {
        AuthTextField(
            label = stringResource(Res.string.step_email),
            value = state.email,
            onValueChange = onEmailChange,
            placeholder = stringResource(Res.string.auth_email_placeholder),
            leadingIcon = CpIcons.Email,
            errorText = state.emailError,
            next = true,
        )
        AuthTextField(
            label = stringResource(Res.string.password),
            value = state.password,
            onValueChange = onPasswordChange,
            placeholder = stringResource(Res.string.password),
            leadingIcon = CpIcons.Lock,
            isPassword = true,
            errorText = state.passwordError,
        )
    }
}

@Preview(widthDp = 360, heightDp = 900)
@Composable
private fun AuthScreenLightPreview() = CoffeePeekTheme(darkTheme = false) {
    AuthScreenContent(LoginFormState())
}

@Preview(widthDp = 360, heightDp = 900)
@Composable
private fun AuthScreenDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    AuthScreenContent(LoginFormState())
}
