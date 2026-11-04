package com.coffeepeek.admin.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GoogleSignInResultTest {
    @Test
    fun theInternalCancellationTokenNeverReachesVisibleCallbacks() {
        val received = mutableListOf<String>()
        handleGoogleSignInResult(Result.failure(IllegalStateException(GOOGLE_SIGN_IN_CANCELLED)),
            { received += "success:$it" }, { received += "failure:$it" })
        assertTrue(received.isEmpty())
    }

    @Test
    fun aSuccessfulTokenOnlyReachesTheSuccessCallback() {
        val received = mutableListOf<String>()
        handleGoogleSignInResult(Result.success("fake-token"), { received += it }, { received += "failure" })
        assertEquals(listOf("fake-token"), received)
    }

    @Test
    fun machineDiagnosticsRemainAvailableForTheLocalizedFailureFormatter() {
        val received = mutableListOf<String>()
        val raw = "private details [GSI-E-4 D:com.google.GIDSignIn]"
        handleGoogleSignInResult(Result.failure(IllegalStateException(raw)), {}, { received += it })
        assertEquals(listOf(raw), received)
    }

    @Test
    fun aMissingProviderMessageUsesAnInternalCodeRatherThanRussianDisplayCopy() {
        val received = mutableListOf<String>()
        handleGoogleSignInResult(Result.failure(IllegalStateException()), {}, { received += it })
        assertEquals(listOf("GOOGLE_SIGN_IN_FAILED"), received)
    }
}
