package com.coffeepeek.feature.auth.impl.ui

import androidx.compose.foundation.layout.Arrangement
import com.coffeepeek.feature.auth.api.GoogleSignInLauncher
import com.coffeepeek.feature.auth.impl.resources.Res
import com.coffeepeek.feature.auth.impl.resources.auth_create_account
import com.coffeepeek.feature.auth.impl.resources.auth_email_invalid
import com.coffeepeek.feature.auth.impl.resources.auth_email_label
import com.coffeepeek.feature.auth.impl.resources.auth_email_placeholder
import com.coffeepeek.feature.auth.impl.resources.auth_email_required
import com.coffeepeek.feature.auth.impl.resources.auth_forgot_password
import com.coffeepeek.feature.auth.impl.resources.auth_login
import com.coffeepeek.feature.auth.impl.resources.auth_login_network
import com.coffeepeek.feature.auth.impl.resources.auth_login_subtitle
import com.coffeepeek.feature.auth.impl.resources.auth_login_title
import com.coffeepeek.feature.auth.impl.resources.auth_login_unknown
import com.coffeepeek.feature.auth.impl.resources.auth_login_wrong_credentials
import com.coffeepeek.feature.auth.impl.resources.auth_password_label
import com.coffeepeek.feature.auth.impl.resources.auth_password_required
import com.coffeepeek.feature.auth.impl.resources.auth_password_too_short
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.auth.domain.EmailIssue
import com.coffeepeek.feature.auth.domain.LoginFailure
import com.coffeepeek.feature.auth.domain.PasswordIssue
import com.coffeepeek.feature.auth.impl.login.LoginAction
import com.coffeepeek.feature.auth.impl.login.LoginEvent
import com.coffeepeek.feature.auth.impl.login.LoginState
import com.coffeepeek.feature.auth.impl.login.LoginViewModel

@Composable
internal fun LoginScreen(
    vm: LoginViewModel,
    onClose: () -> Unit,
    onSignedIn: () -> Unit,
    onForgotPassword: (String) -> Unit,
    onCreateAccount: () -> Unit,
    google: GoogleSignInLauncher?,
    modifier: Modifier = Modifier,
    appleAvailable: Boolean = false,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val currentOnSignedIn by rememberUpdatedState(onSignedIn)
    LaunchedEffect(vm, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            vm.events.collect { event ->
                when (event) {
                    LoginEvent.SignedIn -> currentOnSignedIn()
                }
            }
        }
    }
    LoginContent(
        state = state,
        onEmail = { vm.onAction(LoginAction.EmailChanged(it)) },
        onPassword = { vm.onAction(LoginAction.PasswordChanged(it)) },
        onSubmit = { vm.onAction(LoginAction.Submit) },
        onClose = onClose,
        onForgotPassword = { onForgotPassword(state.email) },
        onCreateAccount = onCreateAccount,
        onGoogle = google?.rememberSignIn(
            onToken = { vm.onAction(LoginAction.GoogleToken(it)) },
            onFailure = { vm.onAction(LoginAction.GoogleFailed(it)) },
        ),
        modifier = modifier,
        onApple = if (appleAvailable) ({ vm.onAction(LoginAction.AppleSignIn) }) else null,
    )
}

@Composable
internal fun LoginContent(
    state: LoginState,
    onEmail: (String) -> Unit,
    onPassword: (String) -> Unit,
    onSubmit: () -> Unit,
    onClose: () -> Unit,
    onForgotPassword: () -> Unit,
    onCreateAccount: () -> Unit,
    onGoogle: (() -> Unit)?,
    modifier: Modifier = Modifier,
    onApple: (() -> Unit)? = null,
) {
    AuthScaffold(onClose = onClose, modifier = modifier) {
        AuthHeading(stringResource(Res.string.auth_login_title), stringResource(Res.string.auth_login_subtitle))
        if (onGoogle != null || onApple != null) {
            SocialSignInButtons(onGoogle, onApple, !state.signingIn)
            AuthOrDivider()
        }
        LoginFields(state, onEmail, onPassword)
        SocialFailureText(state.socialFailure)
        state.failure?.let { failure ->
            Text(text = loginFailureText(failure), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        AuthFooterRow(text = stringResource(Res.string.auth_forgot_password), onClick = onForgotPassword)
        AuthPrimaryButton(text = stringResource(Res.string.auth_login), onClick = onSubmit, loading = state.signingIn)
        AuthFooterRow(text = stringResource(Res.string.auth_create_account), onClick = onCreateAccount)
    }
}

@Composable
private fun LoginFields(state: LoginState, onEmail: (String) -> Unit, onPassword: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing4)) {
        AuthTextField(
            label = stringResource(Res.string.auth_email_label),
            value = state.email,
            onValueChange = onEmail,
            placeholder = stringResource(Res.string.auth_email_placeholder),
            leadingIcon = CoffeePeekIcons.Email,
            errorText = state.emailIssue?.let { emailError(it) },
            next = true,
            enabled = !state.signingIn,
        )
        AuthTextField(
            label = stringResource(Res.string.auth_password_label),
            value = state.password,
            onValueChange = onPassword,
            placeholder = stringResource(Res.string.auth_password_label),
            leadingIcon = CoffeePeekIcons.Lock,
            isPassword = true,
            errorText = state.passwordIssue?.let { passwordError(it) },
            enabled = !state.signingIn,
        )
    }
}

@Composable
internal fun emailError(issue: EmailIssue): String = stringResource(
    if (issue == EmailIssue.Empty) Res.string.auth_email_required else Res.string.auth_email_invalid,
)

@Composable
internal fun passwordError(issue: PasswordIssue): String = when (issue) {
    PasswordIssue.Empty -> stringResource(Res.string.auth_password_required)
    is PasswordIssue.TooShort -> stringResource(Res.string.auth_password_too_short, issue.minLength)
}

@Composable
private fun loginFailureText(failure: LoginFailure): String = stringResource(
    when (failure) {
        LoginFailure.WrongCredentials -> Res.string.auth_login_wrong_credentials
        LoginFailure.Network -> Res.string.auth_login_network
        LoginFailure.Unknown -> Res.string.auth_login_unknown
    },
)

@Composable
private fun LoginPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    LoginContent(LoginState(email = "name@example.com", emailIssue = EmailIssue.Invalid), {}, {}, {}, {}, {}, {}, onGoogle = {}, onApple = {})
}

@Preview @Composable private fun LoginLightPreview() = LoginPreview(false)
@Preview @Composable private fun LoginDarkPreview() = LoginPreview(true)
