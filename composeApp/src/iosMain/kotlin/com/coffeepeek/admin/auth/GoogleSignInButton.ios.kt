package com.coffeepeek.admin.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.coffeepeek.admin.ui.screen.auth.SocialSignInBrand
import com.coffeepeek.admin.ui.screen.auth.SocialSignInButton
import kotlinx.coroutines.launch

actual fun isGoogleSignInConfigured(): Boolean = IosGoogleAuthBridge.isConfigured()

@Composable
actual fun GoogleSignInButton(
    onResult: (Result<String>) -> Unit,
    modifier: Modifier,
) {
    val scope = rememberCoroutineScope()
    var signingIn by remember { mutableStateOf(false) }

    SocialSignInButton(
        brand = SocialSignInBrand.Google,
        onClick = {
            if (!signingIn) {
                signingIn = true
                scope.launch {
                    try { onResult(GoogleAuth.signIn()) }
                    finally { signingIn = false }
                }
            }
        },
        enabled = !signingIn,
        modifier = modifier,
    )
}
