package com.coffeepeek.admin.auth

import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class NativeAppleAuthAttemptTest {
    private val dispatcher = StandardTestDispatcher()
    private val replies = mutableMapOf<String, (String?, String?) -> Unit>()
    private val cancellations = mutableListOf<String>()

    @BeforeTest fun before() = Dispatchers.setMain(dispatcher)
    @AfterTest fun after() = Dispatchers.resetMain()

    private suspend fun signIn(id: String = "one") = awaitNativeAppleAuth(
        AppleLoginChallenge(id, "nonce"),
        { _, state, reply -> replies[state] = reply },
        { cancellations.add(it) },
    )

    @Test fun firstReplyWinsAndSuccessfulRequestDoesNotCancelAnotherSheet() = runTest(dispatcher) {
        val token = async { signIn() }
        runCurrent()
        replies.getValue("one")("first", null)
        replies.getValue("one")("late", null)
        runCurrent()
        assertEquals("first", token.await())
        assertTrue(cancellations.isEmpty())
    }

    @Test fun parentCancellationFinishesPlatformCleanupBeforeReturningAndIgnoresLateToken() = runTest(dispatcher) {
        val tokens = mutableListOf<String>()
        val request = launch { tokens.add(signIn()) }
        runCurrent()
        request.cancel()
        runCurrent()
        request.join()
        assertEquals(listOf("one"), cancellations)
        replies.getValue("one")("late", null)
        runCurrent()
        assertTrue(tokens.isEmpty())
    }

    @Test fun cancellationIdentifiesItsOwnRequestWhileAnotherAttemptIsPending() = runTest(dispatcher) {
        val first = launch { signIn("first") }
        val next = async { signIn("next") }
        runCurrent()
        first.cancel()
        runCurrent()
        assertEquals(listOf("first"), cancellations)
        replies.getValue("next")("next-token", null)
        runCurrent()
        assertEquals("next-token", next.await())
        assertEquals(listOf("first"), cancellations)
    }

    @Test fun platformCancellationRemainsCoroutineCancellation() = runTest(dispatcher) {
        val request = launch { signIn() }
        runCurrent()
        replies.getValue("one")(null, "APPLE_SIGN_IN_CANCELLED")
        runCurrent()
        assertTrue(request.isCancelled)
        assertEquals(listOf("one"), cancellations)
    }

    @Test fun platformErrorIsReportedWithoutCancellingTheCaller() = runTest(dispatcher) {
        var error: String? = null
        val request = launch {
            val failure = assertFailsWith<IllegalStateException> { signIn() }
            error = failure.message
            currentCoroutineContext().ensureActive()
        }
        runCurrent()
        replies.getValue("one")(null, "native failure")
        runCurrent()
        request.join()
        assertEquals("native failure", error)
        assertFalse(request.isCancelled)
        assertTrue(cancellations.isEmpty())
    }
}
