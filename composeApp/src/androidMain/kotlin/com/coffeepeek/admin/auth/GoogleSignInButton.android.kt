package com.coffeepeek.admin.auth

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coffeepeek.BuildConfig
import com.coffeepeek.admin.ui.screen.auth.SocialSignInBrand
import com.coffeepeek.admin.ui.screen.auth.SocialSignInButton

actual fun isGoogleSignInConfigured(): Boolean =
    BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()

@Composable
actual fun GoogleSignInButton(
    onResult: (Result<String>) -> Unit,
    modifier: Modifier,
) {
    val webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
    if (webClientId.isBlank()) return

    val launchSignIn = GoogleAuth.rememberGoogleSignInLauncher(
        webClientId = webClientId,
        onResult = onResult,
    )
    SocialSignInButton(
        brand = SocialSignInBrand.Google,
        onClick = launchSignIn,
        modifier = modifier,
    )
}
