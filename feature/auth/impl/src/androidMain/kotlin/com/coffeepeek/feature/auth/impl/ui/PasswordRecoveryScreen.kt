package com.coffeepeek.feature.auth.impl.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
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
    AuthScaffold(onClose = onClose, modifier = modifier) {
        AuthHeading(
            title = stringResource(R.string.auth_recovery_title),
            subtitle = stringResource(if (state.step == RecoveryStep.Email) R.string.auth_recovery_info else R.string.auth_recovery_sent),
        )
        if (state.step == RecoveryStep.Email) {
            AuthTextField(
                value = state.email,
                onValueChange = onEmail,
                placeholder = stringResource(R.string.auth_email_placeholder),
                label = stringResource(R.string.auth_email_label),
                leadingIcon = CoffeePeekIcons.Email,
            )
        }
        state.error?.let { error ->
            Text(recoveryErrorText(error), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        if (state.step == RecoveryStep.Email) {
            AuthPrimaryButton(
                text = stringResource(if (state.busy) R.string.auth_recovery_busy else R.string.auth_recovery_send),
                enabled = !state.busy,
                onClick = onSubmit,
            )
        } else {
            AuthPrimaryButton(stringResource(R.string.auth_recovery_back_to_login), onBackToLogin)
            AuthFooterRow(stringResource(R.string.auth_recovery_another_email), onAnotherEmail)
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
