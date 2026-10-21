package com.coffeepeek.feature.auth.impl.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekColors
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
        Text(
            text = stringResource(R.string.auth_login_title),
            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.52).sp, lineHeight = 28.6.sp),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = stringResource(R.string.auth_login_subtitle),
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 21.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = CoffeePeekDimens.spacing5),
        )

        if (onGoogle != null) {
            SocialRow { GoogleSignInButton(onClick = onGoogle, enabled = !state.signingIn) }
            Spacer(Modifier.height(14.dp))
            AuthOrDivider()
            Spacer(Modifier.height(14.dp))
        }
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
        Spacer(Modifier.height(14.dp))
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
        SocialFailureText(state.socialFailure)
        AnimatedVisibility(visible = state.failure != null) {
            Text(
                text = state.failure?.let { loginFailureText(it) }.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            )
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 14.dp), horizontalArrangement = Arrangement.End) {
            Text(
                text = stringResource(R.string.auth_forgot_password),
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                color = CoffeePeekColors.Primary,
                textAlign = TextAlign.End,
                modifier = Modifier.clickable(onClick = onForgotPassword),
            )
        }
        AuthPrimaryButton(text = stringResource(R.string.auth_login), onClick = onSubmit, loading = state.signingIn)
        AuthFooterLink(text = stringResource(R.string.auth_create_account), onClick = onCreateAccount)
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
