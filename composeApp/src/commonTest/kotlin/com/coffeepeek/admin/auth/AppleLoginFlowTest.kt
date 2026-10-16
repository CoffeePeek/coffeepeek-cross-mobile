package com.coffeepeek.admin.auth

import com.coffeepeek.domain.model.AppleLoginChallenge
import com.coffeepeek.domain.model.Session
import com.coffeepeek.domain.repository.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class AppleLoginFlowTest {
    @Test
    fun exchangesTheNativeTokenWithItsOriginalChallengeAndReturnsTheSession() = runTest {
        val repository = AppleFlowRepository()

        val session = repository.signInWithApple { challenge ->
            repository.calls += "native"
            assertSame(repository.challenge, challenge)
            assertEquals("server-nonce", challenge.nonce)
            "native-token"
        }

        assertSame(repository.session, session)
        assertEquals(listOf("challenge", "native", "exchange"), repository.calls)
        assertEquals(listOf("challenge-id" to "native-token"), repository.exchanges)
    }

    @Test
    fun challengeFailureHasItsStageAndNeverStartsNativeAuthorization() = runTest {
        val failure = IllegalStateException("network timeout")
        val repository = AppleFlowRepository().apply {
            begin = { Result.failure(failure) }
        }

        val error = assertFailsWith<IllegalStateException> {
            repository.signInWithApple { error("Native authorization must not start") }
        }

        assertEquals("network timeout [ASI-CH]", error.message)
        assertSame(failure, error.cause)
        assertEquals(listOf("challenge"), repository.calls)
    }

    @Test
    fun thrownChallengeFailureAlsoKeepsItsCauseAndStage() = runTest {
        val failure = IllegalStateException("challenge service unavailable")
        val repository = AppleFlowRepository().apply { begin = { throw failure } }

        val error = assertFailsWith<IllegalStateException> {
            repository.signInWithApple { error("Native authorization must not start") }
        }

        assertEquals("challenge service unavailable [ASI-CH]", error.message)
        assertSame(failure, error.cause)
        assertEquals(listOf("challenge"), repository.calls)
    }

    @Test
    fun nativeFailureIsNotWrappedAndNeverExchangesAToken() = runTest {
        val failure = IllegalStateException("Native callback rejected [ASI-S]")
        val repository = AppleFlowRepository()

        val error = assertFailsWith<IllegalStateException> {
            repository.signInWithApple {
                repository.calls += "native"
                throw failure
            }
        }

        assertSame(failure, error)
        assertEquals(listOf("challenge", "native"), repository.calls)
        assertEquals(emptyList(), repository.exchanges)
    }

    @Test
    fun exchangeFailureKeepsTheAccountErrorAndAddsItsStage() = runTest {
        val failure = IllegalStateException("An account with this email already exists")
        val repository = AppleFlowRepository().apply { exchange = { _, _ -> Result.failure(failure) } }

        val error = assertFailsWith<IllegalStateException> {
            repository.signInWithApple {
                repository.calls += "native"
                "native-token"
            }
        }

        assertEquals("An account with this email already exists [ASI-X]", error.message)
        assertSame(failure, error.cause)
        assertEquals(listOf("challenge", "native", "exchange"), repository.calls)
    }

    @Test
    fun thrownExchangeFailureAlsoKeepsItsCauseAndStage() = runTest {
        val failure = IllegalStateException("session storage unavailable")
        val repository = AppleFlowRepository().apply { exchange = { _, _ -> throw failure } }

        val error = assertFailsWith<IllegalStateException> {
            repository.signInWithApple { "native-token" }
        }

        assertEquals("session storage unavailable [ASI-X]", error.message)
        assertSame(failure, error.cause)
    }

    @Test
    fun cancellationAtEveryPhasePropagatesUnchangedAndStopsTheFlow() = runTest {
        for (phase in listOf("challenge-result", "challenge-throw", "native", "exchange-result", "exchange-throw")) {
            val cancelled = CancellationException("cancelled at $phase")
            val repository = AppleFlowRepository().apply {
                when (phase) {
                    "challenge-result" -> begin = { Result.failure(cancelled) }
                    "challenge-throw" -> begin = { throw cancelled }
                    "exchange-result" -> exchange = { _, _ -> Result.failure(cancelled) }
                    "exchange-throw" -> exchange = { _, _ -> throw cancelled }
                }
            }

            val error = assertFailsWith<CancellationException> {
                repository.signInWithApple {
                    repository.calls += "native"
                    if (phase == "native") throw cancelled
                    "native-token"
                }
            }

            assertSame(cancelled, error)
            val expectedCalls = when {
                phase.startsWith("challenge") -> listOf("challenge")
                phase == "native" -> listOf("challenge", "native")
                else -> listOf("challenge", "native", "exchange")
            }
            assertEquals(expectedCalls, repository.calls)
        }
    }

    @Test
    fun retryGetsANewChallengeAndCanSucceedAfterAnExchangeFailure() = runTest {
        val repository = AppleFlowRepository()
        var attempts = 0
        repository.begin = {
            attempts++
            Result.success(AppleLoginChallenge("challenge-$attempts", "nonce-$attempts"))
        }
        repository.exchange = { challengeId, _ ->
            if (challengeId == "challenge-1") Result.failure(IllegalStateException("Expired challenge"))
            else Result.success(repository.session)
        }
        val authorize: suspend (AppleLoginChallenge) -> String = { challenge ->
            repository.calls += "native"
            "token-for-${challenge.nonce}"
        }

        assertFailsWith<IllegalStateException> { repository.signInWithApple(authorize) }
        assertSame(repository.session, repository.signInWithApple(authorize))
        assertEquals(
            listOf("challenge-1" to "token-for-nonce-1", "challenge-2" to "token-for-nonce-2"),
            repository.exchanges,
        )
        assertEquals(listOf("challenge", "native", "exchange", "challenge", "native", "exchange"), repository.calls)
    }
}

private class AppleFlowRepository : AuthRepository {
    val challenge = AppleLoginChallenge("challenge-id", "server-nonce")
    val session = Session("access-token", "refresh-token", "backend-user")
    val calls = mutableListOf<String>()
    val exchanges = mutableListOf<Pair<String, String>>()
    var begin: suspend () -> Result<AppleLoginChallenge> = { Result.success(challenge) }
    var exchange: suspend (String, String) -> Result<Session> = { _, _ -> Result.success(session) }

    override suspend fun beginAppleLogin(): Result<AppleLoginChallenge> {
        calls += "challenge"
        return begin()
    }

    override suspend fun appleLogin(challengeId: String, idToken: String): Result<Session> {
        calls += "exchange"
        exchanges += challengeId to idToken
        return exchange(challengeId, idToken)
    }

    override suspend fun login(email: String, password: String): Result<Session> = error("Unused")
    override suspend fun googleLogin(idToken: String): Result<Session> = error("Unused")
    override suspend fun register(userName: String, email: String, password: String): Result<Unit> = error("Unused")
    override suspend fun isEmailTaken(email: String): Result<Boolean> = error("Unused")
    override suspend fun logout(): Result<Unit> = error("Unused")
}
