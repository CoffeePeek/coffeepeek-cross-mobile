package com.coffeepeek.admin.auth

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class SessionRealtimeReconnectTest {
    @Test fun transportFailureRetriesWhileSessionIsCurrent() = runTest {
        var attempts = 0
        var pauses = 0
        reconnectSession(
            isCurrent = { attempts < 2 },
            connect = { attempts++; error("Transport disconnected") },
            pause = { pauses++ },
        )
        assertEquals(2, attempts)
        assertEquals(1, pauses)
    }

    @Test fun normalDisconnectAlsoRetries() = runTest {
        var attempts = 0
        var pauses = 0
        reconnectSession({ attempts < 2 }, { attempts++ }, { pauses++ })
        assertEquals(2, attempts)
        assertEquals(1, pauses)
    }

    @Test fun cancellationEscapesWithoutRetryOrPause() = runTest {
        val cancellation = CancellationException("Connection replaced")
        var pauses = 0
        val actual = assertFailsWith<CancellationException> {
            reconnectSession({ true }, { throw cancellation }, { pauses++ })
        }
        assertSame(cancellation, actual)
        assertEquals(0, pauses)
    }

    @Test fun obsoleteSessionDoesNotOpenConnection() = runTest {
        reconnectSession({ false }, { error("Unexpected connection") }, { error("Unexpected retry") })
    }
}
