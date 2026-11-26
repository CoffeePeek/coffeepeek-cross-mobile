package com.coffeepeek.admin.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface IosAppleAuthProvider {
    fun signIn(nonce: String, state: String, completion: (String?, String?) -> Unit)
    fun cancel(state: String)
}

object IosAppleAuthRegistry {
    var provider: IosAppleAuthProvider? = null
}

actual object AppleAuth {
    actual fun isSupported(): Boolean = IosAppleAuthRegistry.provider != null

    actual suspend fun signIn(challenge: AppleLoginChallenge): String = withContext(Dispatchers.Main) {
        val provider = checkNotNull(IosAppleAuthRegistry.provider) { "Apple Sign-In не настроен" }
        awaitNativeAppleAuth(challenge, provider::signIn, provider::cancel)
    }
}
