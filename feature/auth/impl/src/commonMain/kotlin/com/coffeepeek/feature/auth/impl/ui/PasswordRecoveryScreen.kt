package com.coffeepeek.feature.auth.impl.ui

import androidx.compose.material3.MaterialTheme
import com.coffeepeek.feature.auth.impl.resources.Res
import com.coffeepeek.feature.auth.impl.resources.auth_email_invalid
import com.coffeepeek.feature.auth.impl.resources.auth_email_label
import com.coffeepeek.feature.auth.impl.resources.auth_email_placeholder
import com.coffeepeek.feature.auth.impl.resources.auth_email_required
import com.coffeepeek.feature.auth.impl.resources.auth_recovery_another_email
import com.coffeepeek.feature.auth.impl.resources.auth_recovery_back_to_login
import com.coffeepeek.feature.auth.impl.resources.auth_recovery_busy
import com.coffeepeek.feature.auth.impl.resources.auth_recovery_failed
import com.coffeepeek.feature.auth.impl.resources.auth_recovery_info
import com.coffeepeek.feature.auth.impl.resources.auth_recovery_send
import com.coffeepeek.feature.auth.impl.resources.auth_recovery_sent
import com.coffeepeek.feature.auth.impl.resources.auth_recovery_title
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.auth.impl.recovery.PasswordRecoveryAction
import com.coffeepeek.feature.auth.impl.recovery.PasswordRecoveryState
import com.coffeepeek.feature.auth.impl.recovery.PasswordRecoveryViewModel
import com.coffeepeek.feature.auth.impl.recovery.RecoveryError
import com.coffeepeek.feature.auth.impl.recovery.RecoveryStep

@Composable
internal fun PasswordRecoveryScreen(
    vm: PasswordRecoveryViewModel,
    onClose: () -> Unit,
    onBackToLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    PasswordRecoveryContent(
        state = state,
        onEmail = { vm.onAction(PasswordRecoveryAction.EmailChanged(it)) },
        onSubmit = {
            focus.clearFocus(force = true)
            keyboard?.hide()
            vm.onAction(PasswordRecoveryAction.Submit)
        },
        onAnotherEmail = { vm.onAction(PasswordRecoveryAction.RequestAnotherEmail) },
        onClose = onClose,
        onBackToLogin = onBackToLogin,
        modifier = modifier,
    )
}

@Composable
internal fun PasswordRecoveryContent(
    state: PasswordRecoveryState,
    onEmail: (String) -> Unit,
    onSubmit: () -> Unit,
    onAnotherEmail: () -> Unit,
    onClose: () -> Unit,
    onBackToLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AuthScaffold(
        title = stringResource(Res.string.auth_recovery_title),
        onClose = onClose,
        modifier = modifier,
        closeIcon = true,
    ) {
        val emailStep = state.step == RecoveryStep.Email
        Text(
            text = stringResource(if (emailStep) Res.string.auth_recovery_info else Res.string.auth_recovery_sent),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (emailStep) {
            RecoveryEmailForm(state, onEmail, onSubmit)
        } else {
            AuthPrimaryButton(stringResource(Res.string.auth_recovery_back_to_login), onBackToLogin)
            AuthFooterRow(stringResource(Res.string.auth_recovery_another_email), onAnotherEmail)
        }
    }
}

@Composable
private fun RecoveryEmailForm(state: PasswordRecoveryState, onEmail: (String) -> Unit, onSubmit: () -> Unit) {
    val emailError = when (state.error) {
        RecoveryError.EmailEmpty -> Res.string.auth_email_required
        RecoveryError.EmailInvalid -> Res.string.auth_email_invalid
        else -> null
    }
    AuthTextField(
        value = state.email,
        onValueChange = onEmail,
        placeholder = stringResource(Res.string.auth_email_placeholder),
        label = stringResource(Res.string.auth_email_label),
        errorText = emailError?.let { stringResource(it) },
        enabled = !state.busy,
        onDone = onSubmit.takeUnless { state.busy },
    )
    if (state.error == RecoveryError.SendFailed) {
        Text(
            text = stringResource(Res.string.auth_recovery_failed),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
        )
    }
    AuthPrimaryButton(
        text = stringResource(if (state.busy) Res.string.auth_recovery_busy else Res.string.auth_recovery_send),
        enabled = !state.busy,
        onClick = onSubmit,
    )
}

@Composable
private fun PasswordRecoveryPreview(dark: Boolean, state: PasswordRecoveryState) = CoffeePeekTheme(darkTheme = dark) {
    PasswordRecoveryContent(state, {}, {}, {}, {}, {})
}

@Preview @Composable private fun PasswordRecoveryLightPreview() =
    PasswordRecoveryPreview(false, PasswordRecoveryState(email = "name@example.com"))

@Preview @Composable private fun PasswordRecoveryDarkPreview() =
    PasswordRecoveryPreview(true, PasswordRecoveryState(email = "name@example.com"))

@Preview @Composable private fun PasswordRecoverySentLightPreview() =
    PasswordRecoveryPreview(false, PasswordRecoveryState(step = RecoveryStep.EmailSent))

@Preview @Composable private fun PasswordRecoverySentDarkPreview() =
    PasswordRecoveryPreview(true, PasswordRecoveryState(step = RecoveryStep.EmailSent))
