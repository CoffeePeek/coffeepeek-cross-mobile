package com.coffeepeek.admin.auth


/** The server nonce and challenge id the native Apple sheet is started with. */
data class AppleLoginChallenge(val challengeId: String, val nonce: String)

expect object AppleAuth {
    fun isSupported(): Boolean
    suspend fun signIn(challenge: AppleLoginChallenge): String
}

