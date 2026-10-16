package com.coffeepeek.admin.auth

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coffeepeek.domain.model.AppleLoginChallenge
import com.coffeepeek.domain.model.Session
import com.coffeepeek.domain.repository.AuthRepository
import kotlinx.coroutines.CancellationException

expect object AppleAuth {
    fun isSupported(): Boolean
    suspend fun signIn(challenge: AppleLoginChallenge): String
}

@Composable
expect fun AppleSignInButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true)

/** Coordinates the server challenge, native authorization, and CoffeePeek session. */
internal suspend fun AuthRepository.signInWithApple(
    authorize: suspend (AppleLoginChallenge) -> String = { AppleAuth.signIn(it) },
): Session {
    val challenge = try {
        beginAppleLogin().getOrThrow()
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        throw IllegalStateException("${error.message.orEmpty()} [ASI-CH]", error)
    }
    val token = authorize(challenge)
    return try {
        appleLogin(challenge.challengeId, token).getOrThrow()
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        throw IllegalStateException("${error.message.orEmpty()} [ASI-X]", error)
    }
}
