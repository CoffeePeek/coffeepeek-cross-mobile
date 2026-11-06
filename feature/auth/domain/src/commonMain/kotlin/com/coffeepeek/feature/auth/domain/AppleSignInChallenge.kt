package com.coffeepeek.feature.auth.domain

/** A server-issued, one-use Apple challenge. The native request uses [nonce] and [challengeId] unchanged. */
data class AppleSignInChallenge(val challengeId: String, val nonce: String)
