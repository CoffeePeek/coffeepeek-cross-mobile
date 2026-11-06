package com.coffeepeek.feature.profile.data

import com.coffeepeek.feature.profile.api.UsernameChangeFailure
import com.coffeepeek.feature.profile.data.backend.usernameFailure
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ProfileUsernameFailureTest {
    @Test fun cooldownKeepsOnlyReasonAndRetryInstant() {
        val failure = usernameFailure(200, "Username can be changed once every 7 days. Try again after 2026-10-29T17:12:31.5305240Z.")
        assertEquals(UsernameChangeFailure.Reason.LimitReached, failure.reason)
        assertEquals("2026-10-29T17:12:31.5305240Z", failure.retryAtUtc)
        assertNull(failure.message)
    }

    @Test fun weeklyQuotaAndStatusOnlyFailuresAreRecognized() {
        for (text in listOf("Username can be changed 3 times per week.", "Username change limit reached", "Достигнут лимит смены ника")) {
            assertEquals(UsernameChangeFailure.Reason.LimitReached, usernameFailure(200, text).reason)
        }
        assertEquals(UsernameChangeFailure.Reason.LimitReached, usernameFailure(429, null).reason)
    }

    @Test fun authenticationTakesPrecedenceAndTakenAndInvalidNamesHaveSeparateReasons() {
        assertEquals(UsernameChangeFailure.Reason.SignInRequired, usernameFailure(401, "Username change limit reached").reason)
        assertEquals(UsernameChangeFailure.Reason.Taken, usernameFailure(200, "Username is already taken").reason)
        assertEquals(UsernameChangeFailure.Reason.Taken, usernameFailure(409, null).reason)
        for (status in listOf(400, 422)) assertEquals(UsernameChangeFailure.Reason.Invalid, usernameFailure(status, null).reason)
    }

    @Test fun unknownDiagnosticsAreNotPassedToPresentation() {
        val failure = usernameFailure(0, "PATCH https://private.invalid failed: internal diagnostic")
        assertEquals(UsernameChangeFailure.Reason.Unavailable, failure.reason)
        assertNull(failure.message)
        assertNull(failure.retryAtUtc)
    }
}
