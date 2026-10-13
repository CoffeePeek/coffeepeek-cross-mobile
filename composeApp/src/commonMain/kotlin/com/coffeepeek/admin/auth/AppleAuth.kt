package com.coffeepeek.admin.auth

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coffeepeek.domain.model.AppleLoginChallenge
import com.coffeepeek.domain.model.Session
import com.coffeepeek.domain.repository.AuthRepository

expect object AppleAuth {
    fun isSupported(): Boolean
    suspend fun signIn(challenge: AppleLoginChallenge): String
}

@Composable
expect fun AppleSignInButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true)

/** Coordinates the server challenge, native authorization, and CoffeePeek session. */
internal suspend fun AuthRepository.signInWithApple(): Session {
    val challenge = beginAppleLogin().getOrThrow()
    val token = AppleAuth.signIn(challenge)
    return appleLogin(challenge.challengeId, token).getOrThrow()
}
