package com.coffeepeek.admin.auth

actual object AppleAuth {
    actual fun isSupported(): Boolean = false
    actual suspend fun signIn(challenge: AppleLoginChallenge): String =
        error("Apple Sign-In is available on iOS")
}
