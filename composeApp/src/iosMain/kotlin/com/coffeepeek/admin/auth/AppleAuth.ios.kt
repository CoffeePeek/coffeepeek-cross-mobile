@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.admin.auth

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.viewinterop.UIKitView
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.domain.model.AppleLoginChallenge
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.UIKit.UIControl
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

interface IosAppleAuthProvider {
    fun makeButton(dark: Boolean, onClick: () -> Unit): UIControl
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
    val provider = IosAppleAuthRegistry.provider ?: return
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    key(dark) {
        UIKitView(
            factory = { provider.makeButton(dark, onClick) },
            modifier = modifier.fillMaxWidth().height(CpDimens.authPrimaryBtnHeight),
            update = { it.enabled = enabled; it.alpha = if (enabled) 1.0 else 0.5 },
            properties = UIKitInteropProperties(isNativeAccessibilityEnabled = true),
        )
    }
}
