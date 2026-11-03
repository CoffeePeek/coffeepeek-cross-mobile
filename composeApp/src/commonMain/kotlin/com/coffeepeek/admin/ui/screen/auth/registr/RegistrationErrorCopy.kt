package com.coffeepeek.admin.ui.screen.auth.registr

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import coffeepeek.composeapp.generated.resources.*
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.auth.domain.RegistrationIssue
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Prepared in the screen so validation and failed requests never load resources in the ViewModel. */
internal data class RegistrationErrorCopy(
    val checkFailed: String, val registrationFailed: String, val nameRequired: String, val emailRequired: String,
    val emailInvalid: String, val passwordTooShort: String, val termsRequired: String,
)

@Composable
internal fun registrationErrorCopy() = RegistrationErrorCopy(
    checkFailed = stringResource(Res.string.maybe_later),
    registrationFailed = stringResource(Res.string.error_registr),
    nameRequired = stringResource(Res.string.error_enter_name),
    emailRequired = stringResource(Res.string.error_email_name),
    emailInvalid = stringResource(Res.string.error_valid_email),
    passwordTooShort = stringResource(Res.string.error_enter_password_length),
    termsRequired = stringResource(Res.string.error_term_of_user),
)

internal fun RegistrationIssue.message(copy: RegistrationErrorCopy): String = when (this) {
    RegistrationIssue.NameRequired -> copy.nameRequired
    RegistrationIssue.EmailRequired -> copy.emailRequired
    RegistrationIssue.EmailInvalid -> copy.emailInvalid
    RegistrationIssue.PasswordTooShort -> copy.passwordTooShort
    RegistrationIssue.TermsRequired -> copy.termsRequired
}

@Composable
private fun RegistrationErrorsPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    val copy = registrationErrorCopy()
    Column { RegistrationIssue.entries.forEach { Text(it.message(copy)) } }
}

@Preview @Composable private fun RegistrationErrorsLightPreview() = RegistrationErrorsPreview(false)
@Preview @Composable private fun RegistrationErrorsDarkPreview() = RegistrationErrorsPreview(true)
