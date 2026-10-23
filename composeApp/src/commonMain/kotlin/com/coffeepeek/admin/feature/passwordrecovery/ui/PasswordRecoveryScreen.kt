package com.coffeepeek.admin.feature.passwordrecovery.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.auth_email_placeholder
import coffeepeek.composeapp.generated.resources.auth_recovery_another_email
import coffeepeek.composeapp.generated.resources.auth_recovery_busy
import coffeepeek.composeapp.generated.resources.auth_recovery_description
import coffeepeek.composeapp.generated.resources.auth_recovery_login
import coffeepeek.composeapp.generated.resources.auth_recovery_send
import coffeepeek.composeapp.generated.resources.auth_recovery_sent
import coffeepeek.composeapp.generated.resources.auth_recovery_title
import coffeepeek.composeapp.generated.resources.step_email
import com.coffeepeek.admin.di.platformViewModel
import com.coffeepeek.admin.ui.icons.CpIcons
import com.coffeepeek.admin.ui.screen.auth.AuthFooterRow
import com.coffeepeek.admin.ui.screen.auth.AuthHeading
import com.coffeepeek.admin.ui.screen.auth.AuthPrimaryButton
import com.coffeepeek.admin.ui.screen.auth.AuthScreenScaffold
import com.coffeepeek.admin.ui.screen.auth.AuthTextField
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.core.parameter.parametersOf

@Composable
internal fun PasswordRecoveryScreen(initialEmail: String = "", onClose: () -> Unit, onLogin: () -> Unit) {
    val vm: PasswordRecoveryViewModel = platformViewModel(parameters = { parametersOf(initialEmail) })
    val state by vm.state.collectAsState()
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    PasswordRecoveryScreenContent(
        state = state,
        onClose = onClose,
        onLogin = onLogin,
        onEmailChange = vm::onEmailChange,
        onAnotherEmail = vm::requestAnotherEmail,
        onSubmit = {
            focus.clearFocus(force = true)
            keyboard?.hide()
            vm.submit()
        },
    )
}

@Composable
private fun PasswordRecoveryScreenContent(
    state: PasswordRecoveryState,
    onClose: () -> Unit = {},
    onLogin: () -> Unit = {},
    onEmailChange: (String) -> Unit = {},
    onAnotherEmail: () -> Unit = {},
    onSubmit: () -> Unit = {},
) {
    AuthScreenScaffold(onClose = onClose) {
        AuthHeading(
            title = stringResource(Res.string.auth_recovery_title),
            subtitle = stringResource(if (state.step == RecoveryStep.Email) Res.string.auth_recovery_description else Res.string.auth_recovery_sent),
        )
        if (state.step == RecoveryStep.Email) {
            AuthTextField(
                value = state.email,
                onValueChange = onEmailChange,
                placeholder = stringResource(Res.string.auth_email_placeholder),
                label = stringResource(Res.string.step_email),
                leadingIcon = CpIcons.Email,
            )
        }
        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        if (state.step == RecoveryStep.Email) {
            AuthPrimaryButton(
                text = stringResource(if (state.busy) Res.string.auth_recovery_busy else Res.string.auth_recovery_send),
                enabled = !state.busy,
                onClick = onSubmit,
            )
        } else {
            AuthPrimaryButton(stringResource(Res.string.auth_recovery_login), onLogin)
            AuthFooterRow(null, onAnotherEmail, stringResource(Res.string.auth_recovery_another_email))
        }
    }
}

@Composable
private fun PasswordRecoveryPreview(dark: Boolean, step: RecoveryStep) = CoffeePeekTheme(darkTheme = dark) {
    PasswordRecoveryScreenContent(PasswordRecoveryState(step = step))
}

@Preview(widthDp = 360, heightDp = 900) @Composable
private fun PasswordRecoveryLightPreview() = PasswordRecoveryPreview(false, RecoveryStep.Email)
@Preview(widthDp = 360, heightDp = 900) @Composable
private fun PasswordRecoveryDarkPreview() = PasswordRecoveryPreview(true, RecoveryStep.Email)
@Preview(widthDp = 360, heightDp = 900) @Composable
private fun PasswordRecoverySentLightPreview() = PasswordRecoveryPreview(false, RecoveryStep.EmailSent)
@Preview(widthDp = 360, heightDp = 900) @Composable
private fun PasswordRecoverySentDarkPreview() = PasswordRecoveryPreview(true, RecoveryStep.EmailSent)
