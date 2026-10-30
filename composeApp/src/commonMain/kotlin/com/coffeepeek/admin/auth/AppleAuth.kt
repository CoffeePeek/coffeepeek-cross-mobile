package com.coffeepeek.admin.auth

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coffeepeek.domain.model.AppleLoginChallenge

expect object AppleAuth {
    fun isSupported(): Boolean
    suspend fun signIn(challenge: AppleLoginChallenge): String
}

@Composable
expect fun AppleSignInButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true)
