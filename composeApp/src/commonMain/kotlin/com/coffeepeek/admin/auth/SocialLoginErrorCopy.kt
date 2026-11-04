package com.coffeepeek.admin.auth

import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import coffeepeek.composeapp.generated.resources.*
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Complete localized copy, prepared by the screen before an error callback needs it. */
internal data class SocialLoginErrorCopy(
    val accountExists: String, val emailUnverified: String, val network: String, val generic: String,
    val codeLabel: String, val domainLabel: String, val oauthLabel: String,
)

@Composable
internal fun socialLoginErrorCopy() = SocialLoginErrorCopy(
    accountExists = stringResource(Res.string.native_social_account_exists),
    emailUnverified = stringResource(Res.string.native_social_email_unverified),
    network = stringResource(Res.string.native_social_network),
    generic = stringResource(Res.string.native_social_failed),
    codeLabel = stringResource(Res.string.native_social_code),
    domainLabel = stringResource(Res.string.native_social_domain),
    oauthLabel = stringResource(Res.string.native_social_oauth),
)

@Composable
private fun SocialLoginErrorCopyPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    val copy = socialLoginErrorCopy()
    Surface { Text(socialLoginError("Apple", "[ASI-W]", copy)) }
}

@Preview @Composable private fun SocialLoginErrorCopyLightPreview() = SocialLoginErrorCopyPreview(false)
@Preview @Composable private fun SocialLoginErrorCopyDarkPreview() = SocialLoginErrorCopyPreview(true)
