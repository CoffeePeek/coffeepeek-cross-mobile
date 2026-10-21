package com.coffeepeek.feature.auth.impl.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.auth.impl.R
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
        onSubmit = { focus.clearFocus(force = true); keyboard?.hide(); vm.onAction(PasswordRecoveryAction.Submit) },
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
    AuthScaffold(onClose = onClose, modifier = modifier, mascot = false) {
        Text(
            text = stringResource(R.string.auth_recovery_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(16.dp))
        when (state.step) {
            RecoveryStep.Email -> {
                Text(stringResource(R.string.auth_recovery_info), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                AuthTextField(
                    value = state.email,
                    onValueChange = onEmail,
                    placeholder = stringResource(R.string.auth_email_placeholder),
                    label = stringResource(R.string.auth_email_label),
                )
            }
            RecoveryStep.EmailSent -> {
                Text(stringResource(R.string.auth_recovery_sent), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                AuthPrimaryButton(stringResource(R.string.auth_recovery_back_to_login), onBackToLogin)
                TextButton(onClick = onAnotherEmail) { Text(stringResource(R.string.auth_recovery_another_email)) }
            }
        }
        state.error?.let { error ->
            Spacer(Modifier.height(12.dp))
            Text(recoveryErrorText(error), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        if (state.step == RecoveryStep.Email) {
            Spacer(Modifier.height(16.dp))
            AuthPrimaryButton(
                text = stringResource(if (state.busy) R.string.auth_recovery_busy else R.string.auth_recovery_send),
                enabled = !state.busy,
                onClick = onSubmit,
            )
        }
    }
}

@Composable
private fun recoveryErrorText(error: RecoveryError): String = stringResource(
    when (error) {
        RecoveryError.EmailEmpty -> R.string.auth_email_required
        RecoveryError.EmailInvalid -> R.string.auth_email_invalid
        RecoveryError.SendFailed -> R.string.auth_recovery_failed
    },
)

@PreviewLightDark
@Composable
private fun PasswordRecoveryPreview() = CoffeePeekTheme {
    PasswordRecoveryContent(PasswordRecoveryState(email = "name@example.com"), {}, {}, {}, {}, {})
}
