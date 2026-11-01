package com.coffeepeek.admin.auth

// Internal legacy bridge token: filtered before any localized UI callback.
internal const val GOOGLE_SIGN_IN_CANCELLED = "Вход через Google отменён"

fun handleGoogleSignInResult(
    result: Result<String>,
    onSuccess: (String) -> Unit,
    onFailure: (String) -> Unit,
) {
    result.fold(
        onSuccess = onSuccess,
        onFailure = { error ->
            if (error.message == GOOGLE_SIGN_IN_CANCELLED) return
            val message = error.message?.takeIf { it.isNotBlank() }
                ?: "GOOGLE_SIGN_IN_FAILED"
            onFailure(message)
        },
    )
}
