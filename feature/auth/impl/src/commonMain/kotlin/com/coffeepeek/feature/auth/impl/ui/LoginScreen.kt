package com.coffeepeek.feature.auth.impl.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ButtonDefaults
import com.coffeepeek.feature.auth.api.GoogleSignInLauncher
import com.coffeepeek.feature.auth.impl.resources.Res
import com.coffeepeek.feature.auth.impl.resources.auth_email_invalid
import com.coffeepeek.feature.auth.impl.resources.auth_email_label
import com.coffeepeek.feature.auth.impl.resources.auth_email_placeholder
import com.coffeepeek.feature.auth.impl.resources.auth_email_required
import com.coffeepeek.feature.auth.impl.resources.auth_forgot_password
import com.coffeepeek.feature.auth.impl.resources.auth_login
import com.coffeepeek.feature.auth.impl.resources.auth_login_email_not_confirmed
import com.coffeepeek.feature.auth.impl.resources.auth_login_network
import com.coffeepeek.feature.auth.impl.resources.auth_login_no_account
import com.coffeepeek.feature.auth.impl.resources.auth_login_register
import com.coffeepeek.feature.auth.impl.resources.auth_login_title
import com.coffeepeek.feature.auth.impl.resources.auth_login_unknown
import com.coffeepeek.feature.auth.impl.resources.auth_login_wrong_credentials
import com.coffeepeek.feature.auth.impl.resources.auth_password_label
import com.coffeepeek.feature.auth.impl.resources.auth_password_required
import com.coffeepeek.feature.auth.impl.resources.auth_password_too_short
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.expandVertically
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.coffeepeek.core.designsystem.component.CoffeePeekTextButton
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
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val submit: () -> Unit = {
        if (!state.signingIn) {
            focus.clearFocus()
            keyboard?.hide()
            onSubmit()
        }
    }
    AuthScaffold(stringResource(Res.string.auth_login_title), onClose, modifier) {
        LoginFields(state, onEmail, onPassword, submit, onForgotPassword)
        LoginFailureMessage(state.failure)
        AuthPrimaryButton(text = stringResource(Res.string.auth_login), onClick = submit, loading = state.signingIn)
        LoginRegistrationPrompt(onCreateAccount, enabled = !state.signingIn)
        if (onGoogle != null || onApple != null) {
            AuthOrDivider()
            SocialSignInButtons(onGoogle, onApple, !state.signingIn)
            SocialFailureText(state.socialFailure)
        }
    }
}

@Composable
private fun LoginRegistrationPrompt(onRegister: () -> Unit, enabled: Boolean) {
    val colors = MaterialTheme.colorScheme
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.auth_login_no_account),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        CoffeePeekTextButton(
            onClick = onRegister,
            enabled = enabled,
            colors = ButtonDefaults.textButtonColors(contentColor = colors.onSurface),
        ) {
            Text(
                text = stringResource(Res.string.auth_login_register),
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun LoginFields(
    state: LoginState, onEmail: (String) -> Unit, onPassword: (String) -> Unit, onSubmit: () -> Unit,
    onForgotPassword: () -> Unit,
) {
    val password = remember { FocusRequester() }
    val enabled = !state.signingIn
    Column {
        AuthTextField(
            label = stringResource(Res.string.auth_email_label),
            value = state.email,
            onValueChange = onEmail,
            placeholder = stringResource(Res.string.auth_email_placeholder),
            errorText = state.emailIssue?.let { emailError(it) },
            next = true,
            enabled = enabled,
            onNext = { password.requestFocus() },
        )
        AuthTextField(
            label = stringResource(Res.string.auth_password_label),
            value = state.password,
            onValueChange = onPassword,
            placeholder = stringResource(Res.string.auth_password_label),
            fieldModifier = Modifier.focusRequester(password),
            isPassword = true,
            errorText = state.passwordIssue?.let { passwordError(it) },
            enabled = enabled,
            onDone = onSubmit,
            labelAction = { AuthFieldLabelLink(stringResource(Res.string.auth_forgot_password), onForgotPassword, enabled) },
        )
    }
}

@Composable
private fun LoginFailureMessage(failure: LoginFailure?) {
    val failureText = failure?.let { loginFailureText(it) }
    AnimatedVisibility(visible = failureText != null, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
        var lastFailureText by remember { mutableStateOf(failureText.orEmpty()) }
        SideEffect { if (failureText != null) lastFailureText = failureText }
        Text(failureText ?: lastFailureText, Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
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
        LoginFailure.EmailNotConfirmed -> Res.string.auth_login_email_not_confirmed
        LoginFailure.Network -> Res.string.auth_login_network
        LoginFailure.Unknown -> Res.string.auth_login_unknown
    },
)

@Composable
private fun LoginPreview(dark: Boolean, state: LoginState = LoginState(), showSocial: Boolean = false) =
    CoffeePeekTheme(darkTheme = dark) {
    LoginContent(state, {}, {}, {}, {}, {}, {}, onGoogle = if (showSocial) ({}) else null,
        onApple = if (showSocial) ({}) else null)
}

@Preview @Composable private fun LoginLightPreview() = LoginPreview(false)
@Preview @Composable private fun LoginDarkPreview() = LoginPreview(true)
@Preview @Composable private fun LoginFailureLightPreview() =
    LoginPreview(false, LoginState(email = "name@example.com", failure = LoginFailure.EmailNotConfirmed), showSocial = true)
@Preview @Composable private fun LoginFailureDarkPreview() =
    LoginPreview(true, LoginState(email = "name@example.com", failure = LoginFailure.EmailNotConfirmed), showSocial = true)
