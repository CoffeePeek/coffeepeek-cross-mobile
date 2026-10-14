package com.coffeepeek.admin.auth

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coffeepeek.admin.ui.screen.auth.SocialSignInBrand
import com.coffeepeek.admin.ui.screen.auth.SocialSignInButton
import com.coffeepeek.domain.model.AppleLoginChallenge
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

interface IosAppleAuthProvider {
    fun signIn(nonce: String, state: String, completion: (String?, String?) -> Unit)
    fun cancel()
}

object IosAppleAuthRegistry {
    var provider: IosAppleAuthProvider? = null
}

actual object AppleAuth {
    actual fun isSupported(): Boolean = IosAppleAuthRegistry.provider != null

    actual suspend fun signIn(challenge: AppleLoginChallenge): String = withContext(Dispatchers.Main) {
        val provider = checkNotNull(IosAppleAuthRegistry.provider) { "Apple Sign-In не настроен" }
        suspendCancellableCoroutine { continuation ->
            provider.signIn(challenge.nonce, challenge.challengeId) { token, error ->
                if (continuation.isActive) {
                    when {
                        error == "APPLE_SIGN_IN_CANCELLED" -> continuation.cancel(CancellationException("Apple Sign-In cancelled"))
                        !token.isNullOrBlank() -> continuation.resume(token)
                        else -> continuation.resumeWithException(IllegalStateException(error ?: "Не удалось войти через Apple"))
                    }
                }
            }
            continuation.invokeOnCancellation { provider.cancel() }
        }
    }
}

@Composable
actual fun AppleSignInButton(onClick: () -> Unit, modifier: Modifier, enabled: Boolean) {
    if (!AppleAuth.isSupported()) return
    SocialSignInButton(
        brand = SocialSignInBrand.Apple,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
    )
}
