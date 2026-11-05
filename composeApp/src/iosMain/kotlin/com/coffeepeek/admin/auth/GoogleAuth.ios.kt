package com.coffeepeek.admin.auth

import com.coffeepeek.admin.config.AppConfig
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object IosGoogleAuthBridge {
    private var configured = false
    private var signInHandler: ((((String?, String?) -> Unit)) -> Unit)? = null
    private var signOutHandler: (() -> Unit)? = null

    fun configure(
        isConfigured: Boolean,
        signIn: ((((String?, String?) -> Unit)) -> Unit)?,
        signOut: (() -> Unit)?,
    ) {
        configured = isConfigured && !AppConfig.isDebug
        signInHandler = signIn
        signOutHandler = signOut
    }

    fun isConfigured(): Boolean = configured && signInHandler != null

    suspend fun signIn(): Result<String> = suspendCancellableCoroutine { continuation ->
        val handler = signInHandler
        if (!configured || handler == null) {
            continuation.resume(Result.failure(IllegalStateException("Google Sign-In не настроен")))
            return@suspendCancellableCoroutine
        }
        handler { token, error ->
            if (!continuation.isActive) return@handler
            val result = when {
                !token.isNullOrBlank() -> Result.success(token)
                !error.isNullOrBlank() -> Result.failure(IllegalStateException(error))
                else -> Result.failure(IllegalStateException("Не удалось получить Google token"))
            }
            continuation.resume(result)
        }
    }

    fun signOut() {
        signOutHandler?.invoke()
    }
}

actual object GoogleAuth {
    actual fun isSupported(): Boolean = IosGoogleAuthBridge.isConfigured()

    actual suspend fun signIn(): Result<String> = IosGoogleAuthBridge.signIn()

    actual suspend fun signOut() {
        IosGoogleAuthBridge.signOut()
    }
}
