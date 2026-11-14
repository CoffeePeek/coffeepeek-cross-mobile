package com.coffeepeek.feature.auth.impl

import com.coffeepeek.feature.auth.api.AppleAuthorizer
import com.coffeepeek.feature.auth.domain.AuthRepository
import com.coffeepeek.feature.auth.domain.SignedInAccount
import kotlinx.coroutines.CancellationException

/** One challenge per attempt; no cached nonce or token, and native cancellation never becomes a failed sign-in. */
internal suspend fun signInWithApple(repository: AuthRepository, authorizer: AppleAuthorizer): Result<SignedInAccount> = try {
    val challenge = appleStep("CH") { repository.beginAppleSignIn().getOrThrow() }
    val token = authorizer.authorize(challenge.nonce, challenge.challengeId)
    Result.success(appleStep("X") { repository.signInWithApple(challenge.challengeId, token).getOrThrow() })
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (failure: Exception) {
    Result.failure(failure)
}

private suspend fun <T> appleStep(stage: String, block: suspend () -> T): T = try {
    block()
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (failure: Exception) {
    throw IllegalStateException("${failure.message.orEmpty()} [ASI-$stage]", failure)
}
