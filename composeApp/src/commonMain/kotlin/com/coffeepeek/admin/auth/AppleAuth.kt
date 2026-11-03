package com.coffeepeek.admin.auth

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** The server nonce and challenge id the native Apple sheet is started with. */
data class AppleLoginChallenge(val challengeId: String, val nonce: String)

expect object AppleAuth {
    fun isSupported(): Boolean
    suspend fun signIn(challenge: AppleLoginChallenge): String
}

@Composable
expect fun AppleSignInButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true)
