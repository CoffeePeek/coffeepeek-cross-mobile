package com.coffeepeek.admin.auth

import kotlinx.coroutines.*

/** The platform UI port, including cancellation, is invoked only by its Main-owned request. */
internal suspend fun awaitNativeAppleAuth(challenge: AppleLoginChallenge,
    begin: (String, String, (String?, String?) -> Unit) -> Unit,
    cancel: (String) -> Unit,
): String = withContext(Dispatchers.Main) {
    coroutineScope {
        val response = CompletableDeferred<Result<String>>(coroutineContext[Job])
        try {
            begin(challenge.nonce, challenge.challengeId) { token, error -> completeAppleAuth(response, token, error) }
            response.await().getOrThrow()
        } finally {
            withContext(NonCancellable) {
                if (!response.isCompleted || response.isCancelled) cancel(challenge.challengeId)
                response.cancel()
            }
        }
    }
}

private fun completeAppleAuth(response: CompletableDeferred<Result<String>>, token: String?, error: String?) {
    if (!response.isActive) return
    when {
        error == "APPLE_SIGN_IN_CANCELLED" -> response.cancel(CancellationException("Apple Sign-In cancelled"))
        !token.isNullOrBlank() -> response.complete(Result.success(token))
        else -> response.complete(Result.failure(IllegalStateException(error ?: "Не удалось войти через Apple")))
    }
}
