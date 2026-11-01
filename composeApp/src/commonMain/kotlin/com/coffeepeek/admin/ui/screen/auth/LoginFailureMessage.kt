package com.coffeepeek.admin.ui.screen.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import coffeepeek.composeapp.generated.resources.*
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.auth.domain.LoginFailure
import org.jetbrains.compose.ui.tooling.preview.Preview

internal data class LoginFailureCopy(val wrongCredentials: String, val network: String, val unknown: String)

@Composable
internal fun loginFailureMessage(failure: LoginFailure): String = failure.message(LoginFailureCopy(
    wrongCredentials = stringResource(Res.string.no_login),
    network = stringResource(Res.string.legacy_auth_login_network),
    unknown = stringResource(Res.string.legacy_auth_login_unknown),
))

internal fun LoginFailure.message(copy: LoginFailureCopy): String = when (this) {
    LoginFailure.WrongCredentials -> copy.wrongCredentials
    LoginFailure.Network -> copy.network
    LoginFailure.Unknown -> copy.unknown
}

@Composable
private fun LoginFailureMessagesPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Column { LoginFailure.entries.forEach { Text(loginFailureMessage(it)) } }
}

@Preview @Composable private fun LoginFailureMessagesLightPreview() = LoginFailureMessagesPreview(false)
@Preview @Composable private fun LoginFailureMessagesDarkPreview() = LoginFailureMessagesPreview(true)
