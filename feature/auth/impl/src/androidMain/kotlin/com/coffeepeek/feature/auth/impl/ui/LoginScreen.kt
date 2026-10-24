package com.coffeepeek.feature.auth.impl.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
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
import com.coffeepeek.feature.auth.impl.R
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
    googleWebClientId: String,
    modifier: Modifier = Modifier,
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
        onGoogle = if (googleWebClientId.isBlank()) null else rememberGoogleSignIn(
            googleWebClientId,
            onToken = { vm.onAction(LoginAction.GoogleToken(it)) },
            onFailure = { vm.onAction(LoginAction.GoogleFailed(it)) },
        ),
        modifier = modifier,
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
) {
    AuthScaffold(onClose = onClose, modifier = modifier) {
        AuthHeading(stringResource(R.string.auth_login_title), stringResource(R.string.auth_login_subtitle))
        if (onGoogle != null) {
            GoogleSignInButton(onClick = onGoogle, enabled = !state.signingIn)
            AuthOrDivider()
        }
        Column(verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing4)) {
            AuthTextField(
                label = stringResource(R.string.auth_email_label),
                value = state.email,
                onValueChange = onEmail,
                placeholder = stringResource(R.string.auth_email_placeholder),
                leadingIcon = CoffeePeekIcons.Email,
                errorText = state.emailIssue?.let { emailError(it) },
                next = true,
                enabled = !state.signingIn,
            )
            AuthTextField(
                label = stringResource(R.string.auth_password_label),
                value = state.password,
                onValueChange = onPassword,
                placeholder = stringResource(R.string.auth_password_label),
                leadingIcon = CoffeePeekIcons.Lock,
                isPassword = true,
                errorText = state.passwordIssue?.let { passwordError(it) },
                enabled = !state.signingIn,
            )
        }
        SocialFailureText(state.socialFailure)
        state.failure?.let { failure ->
            Text(text = loginFailureText(failure), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        AuthFooterRow(text = stringResource(R.string.auth_forgot_password), onClick = onForgotPassword)
        AuthPrimaryButton(text = stringResource(R.string.auth_login), onClick = onSubmit, loading = state.signingIn)
        AuthFooterRow(text = stringResource(R.string.auth_create_account), onClick = onCreateAccount)
    }
}

@Composable
internal fun emailError(issue: EmailIssue): String = stringResource(
    if (issue == EmailIssue.Empty) R.string.auth_email_required else R.string.auth_email_invalid,
)

@Composable
internal fun passwordError(issue: PasswordIssue): String = when (issue) {
    PasswordIssue.Empty -> stringResource(R.string.auth_password_required)
    is PasswordIssue.TooShort -> stringResource(R.string.auth_password_too_short, issue.minLength)
}

@Composable
private fun loginFailureText(failure: LoginFailure): String = stringResource(
    when (failure) {
        LoginFailure.WrongCredentials -> R.string.auth_login_wrong_credentials
        LoginFailure.Network -> R.string.auth_login_network
        LoginFailure.Unknown -> R.string.auth_login_unknown
    },
)

@PreviewLightDark
@Composable
private fun LoginPreview() = CoffeePeekTheme {
    LoginContent(LoginState(email = "name@example.com", emailIssue = EmailIssue.Invalid), {}, {}, {}, {}, {}, {}, onGoogle = {})
}
