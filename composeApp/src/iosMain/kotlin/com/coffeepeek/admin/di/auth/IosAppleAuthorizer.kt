package com.coffeepeek.admin.di.auth

import com.coffeepeek.admin.auth.AppleAuth
import com.coffeepeek.domain.model.AppleLoginChallenge
import com.coffeepeek.feature.auth.api.AppleAuthorizer

/** The feature never knows the legacy model or the Swift provider registry. */
internal object IosAppleAuthorizer : AppleAuthorizer {
    override suspend fun authorize(nonce: String, state: String): String =
        AppleAuth.signIn(AppleLoginChallenge(challengeId = state, nonce = nonce))
}
