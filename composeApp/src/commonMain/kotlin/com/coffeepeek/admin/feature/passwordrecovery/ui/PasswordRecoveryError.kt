package com.coffeepeek.admin.feature.passwordrecovery.ui

import androidx.compose.runtime.Composable
import coffeepeek.composeapp.generated.resources.*
import com.coffeepeek.admin.ui.component.InputValidationLabels
import com.coffeepeek.admin.ui.component.inputValidationLabels
import com.coffeepeek.admin.ui.component.message
import com.coffeepeek.admin.utils.InputValidationIssue
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

internal sealed interface PasswordRecoveryError {
    data class Validation(val issue: InputValidationIssue) : PasswordRecoveryError
    data object SendFailed : PasswordRecoveryError
}

@Composable
internal fun passwordRecoveryMessage(error: PasswordRecoveryError): String = error.message(
    inputValidationLabels(), stringResource(Res.string.legacy_recovery_send_failed),
)

internal fun PasswordRecoveryError.message(validation: InputValidationLabels, sendFailed: String): String = when (this) {
    is PasswordRecoveryError.Validation -> issue.message(validation)
    PasswordRecoveryError.SendFailed -> sendFailed
}
