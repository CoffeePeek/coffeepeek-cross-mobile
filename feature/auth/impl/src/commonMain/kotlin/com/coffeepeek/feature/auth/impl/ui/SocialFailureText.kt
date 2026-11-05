package com.coffeepeek.feature.auth.impl.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.auth.domain.*
import com.coffeepeek.feature.auth.impl.resources.*
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun SocialFailureText(failure: SocialFailure?, modifier: Modifier = Modifier) {
    if (failure == null) return
    val base = socialFailureText(failure)
    val diagnostic = failure.diagnostic
    val text = if (diagnostic == null) base else {
        val domain = diagnostic.domain?.let { stringResource(Res.string.auth_social_diag_domain, it) }.orEmpty()
        val oauth = diagnostic.oauthError?.let { stringResource(Res.string.auth_social_diag_oauth, it) }.orEmpty()
        stringResource(Res.string.auth_social_diag, base, diagnostic.code, domain, oauth)
    }
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error,
        modifier = modifier.fillMaxWidth().padding(top = 6.dp))
}

@Composable
private fun socialFailureText(failure: SocialFailure): String = stringResource(when (failure.kind) {
    SocialFailureKind.AccountExists -> Res.string.auth_social_account_exists
    SocialFailureKind.EmailNotVerified -> if (failure.provider == SocialProvider.Apple) Res.string.auth_social_apple_email_not_verified else Res.string.auth_social_email_not_verified
    SocialFailureKind.Network -> Res.string.auth_login_network
    SocialFailureKind.Generic -> if (failure.provider == SocialProvider.Apple) Res.string.auth_social_apple_generic else Res.string.auth_social_generic
})

@Composable
private fun SocialFailureTextPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Surface { SocialFailureText(classifySocialFailure(SocialProvider.Apple, "Failed [ASI-H401]")) }
}

@Preview @Composable private fun SocialFailureTextLightPreview() = SocialFailureTextPreview(false)
@Preview @Composable private fun SocialFailureTextDarkPreview() = SocialFailureTextPreview(true)
