package com.coffeepeek.admin.auth

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coffeepeek.domain.model.AppleLoginChallenge

actual object AppleAuth {
    actual fun isSupported(): Boolean = false
    actual suspend fun signIn(challenge: AppleLoginChallenge): String =
        error("Apple Sign-In is available on iOS")
}

@Composable
actual fun AppleSignInButton(onClick: () -> Unit, modifier: Modifier, enabled: Boolean) = Unit
