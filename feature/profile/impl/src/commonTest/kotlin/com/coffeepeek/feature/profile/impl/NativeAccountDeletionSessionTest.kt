package com.coffeepeek.feature.profile.impl

import com.coffeepeek.feature.profile.api.AccountDeletionPort
import com.coffeepeek.feature.profile.api.ProfileDeletionRequest
import com.coffeepeek.feature.profile.impl.ui.FakeProfileAccounts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class NativeAccountDeletionSessionTest {
    private val dispatcher = StandardTestDispatcher()
    private val accounts = FakeProfileAccounts()
    private var requests = 0
    private val deletion = object : AccountDeletionPort {
        override suspend fun existingRequest(): Result<ProfileDeletionRequest?> = Result.success(null)
        override suspend fun request() = Result.success(ProfileDeletionRequest("")).also { requests++ }
    }
    private lateinit var session: NativeAccountDeletionSession

    @BeforeTest fun before() {
        Dispatchers.setMain(dispatcher)
        session = createNativeAccountDeletionSession(accounts, deletion)
    }

    @AfterTest fun after() {
        session.close()
        Dispatchers.resetMain()
    }

    @Test fun openingOnlyLoadsStatusAndExplicitConfirmationUsesTheSharedWriter() = runTest(dispatcher) {
        runCurrent()
        assertEquals(0, requests)
        assertTrue(session.current().canSend)
        session.send()
        runCurrent()
        assertEquals(1, requests)
        assertTrue(session.current().requested)
    }

    @Test fun replacedAccountClearsPrivateEmailAndCannotSend() = runTest(dispatcher) {
        runCurrent()
        assertEquals("anna@example.com", session.current().email)
        accounts.signIn("replacement")
        session.send()
        runCurrent()
        assertTrue(session.current().revoked)
        assertEquals("", session.current().email)
        assertFalse(session.current().canSend)
        assertEquals(0, requests)
        accounts.signIn("a")
        assertFalse(session.isCurrent())
    }

    @Test fun queuedConfirmationCannotSendForTheReplacementAccount() = runTest(dispatcher) {
        runCurrent()
        session.send()
        accounts.signIn("replacement")
        runCurrent()
        assertEquals(0, requests)
    }

    @Test fun closingRevokesAnyRetainedNativeActions() = runTest(dispatcher) {
        runCurrent()
        session.close()
        session.send()
        runCurrent()
        assertTrue(session.current().revoked)
        assertEquals(0, requests)
    }
}
