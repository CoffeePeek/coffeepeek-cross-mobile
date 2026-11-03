package com.coffeepeek.admin.ui.component

import androidx.compose.runtime.Composable
import coffeepeek.composeapp.generated.resources.*
import com.coffeepeek.admin.utils.InputValidationIssue
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

internal data class InputValidationLabels(
    val requiredEmail: String,
    val invalidEmail: String,
    val requiredPassword: String,
    val passwordLength: String,
    val invalidPhone: String,
    val invalidUrl: String,
    val invalidInstagram: String,
)

@Composable
internal fun inputValidationLabels() = InputValidationLabels(
    stringResource(Res.string.error_enter_email),
    stringResource(Res.string.error_valid_email),
    stringResource(Res.string.error_enter_password),
    stringResource(Res.string.legacy_input_password_length),
    stringResource(Res.string.error_invalid_phone),
    stringResource(Res.string.legacy_input_url_invalid),
    stringResource(Res.string.error_invalid_instagram),
)

@Composable
internal fun inputValidationMessage(issue: InputValidationIssue?): String? = issue?.message(inputValidationLabels())

internal fun InputValidationIssue.message(labels: InputValidationLabels): String = when (this) {
    InputValidationIssue.EmailRequired -> labels.requiredEmail
    InputValidationIssue.EmailInvalid -> labels.invalidEmail
    InputValidationIssue.PasswordRequired -> labels.requiredPassword
    is InputValidationIssue.PasswordTooShort -> labels.passwordLength.replace("%1\$s", minLength.toString())
    InputValidationIssue.PhoneInvalid -> labels.invalidPhone
    InputValidationIssue.UrlInvalid -> labels.invalidUrl
    InputValidationIssue.InstagramInvalid -> labels.invalidInstagram
}
