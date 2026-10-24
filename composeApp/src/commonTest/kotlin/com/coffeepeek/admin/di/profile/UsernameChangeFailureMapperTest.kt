package com.coffeepeek.admin.di.profile

import com.coffeepeek.api.utils.ApiException
import com.coffeepeek.feature.profile.api.UsernameChangeFailure
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

/** Common adapter contract, with fake repositories only. */
class UsernameChangeFailureMapperTest {
    @Test fun cancellationDoesNotBecomeAUsernameValidationError() {
        val cancelled = kotlinx.coroutines.CancellationException("cancelled")
        kotlin.test.assertSame(cancelled, kotlin.test.assertFailsWith<kotlinx.coroutines.CancellationException> {
            cancelled.toUsernameChangeFailure()
        })
    }

    @Test fun legacyCooldownMessageKeepsOnlyTheReasonAndRetryInstant() {
        val error = ApiException("Username can be changed once every 7 days. Try again after 2026-10-29T17:12:31.5305240Z.")
        val mapped = error.toUsernameChangeFailure()
        assertEquals(UsernameChangeFailure.Reason.LimitReached, mapped.reason)
        assertEquals("2026-10-29T17:12:31.5305240Z", mapped.retryAtUtc)
        assertNull(mapped.message)
    }

    @Test fun weeklyQuotaAndStatusOnlyFailuresAreRecognized() {
        val messages = listOf("Username can be changed 3 times per week.", "Username change limit reached", "Достигнут лимит смены ника")
        for (message in messages) {
            assertEquals(UsernameChangeFailure.Reason.LimitReached, ApiException(message).toUsernameChangeFailure().reason)
        }
        assertEquals(UsernameChangeFailure.Reason.LimitReached, ApiException(null, 429).toUsernameChangeFailure().reason)
    }

    @Test fun authenticationErrorsTakePrecedenceOverServerText() {
        assertEquals(
            UsernameChangeFailure.Reason.SignInRequired,
            ApiException("Username change limit reached", 401).toUsernameChangeFailure().reason,
        )
    }

    @Test fun takenAndInvalidNamesHaveSeparateReasons() {
        assertEquals(UsernameChangeFailure.Reason.Taken, ApiException("Username is already taken").toUsernameChangeFailure().reason)
        assertEquals(UsernameChangeFailure.Reason.Taken, ApiException(null, 409).toUsernameChangeFailure().reason)
        assertEquals(UsernameChangeFailure.Reason.Invalid, ApiException(null, 400).toUsernameChangeFailure().reason)
        assertEquals(UsernameChangeFailure.Reason.Invalid, ApiException(null, 422).toUsernameChangeFailure().reason)
    }

    @Test fun unknownDiagnosticsAreNotPassedToPresentation() {
        val mapped = IllegalStateException("PATCH https://private.invalid failed: internal diagnostic").toUsernameChangeFailure()
        assertEquals(UsernameChangeFailure.Reason.Unavailable, mapped.reason)
        assertNull(mapped.message)
        assertNull(mapped.retryAtUtc)
    }

    @Test fun anAlreadyMappedFailureIsPreserved() {
        val failure = UsernameChangeFailure(UsernameChangeFailure.Reason.LimitReached, "2026-10-29T20:12:31+03:00")
        assertSame(failure, failure.toUsernameChangeFailure())
    }
}
