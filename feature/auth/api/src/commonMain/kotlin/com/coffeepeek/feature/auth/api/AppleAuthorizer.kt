package com.coffeepeek.feature.auth.api

/**
 * Native Apple authorization. Pass the server nonce and challenge id as request nonce/state, without rehashing them.
 * Returns the identity token after native state validation; cancellation throws CancellationException, not a UI error.
 * An unavailable platform supplies no authorizer, so no Apple button is rendered.
 */
interface AppleAuthorizer {
    suspend fun authorize(nonce: String, state: String): String
}
