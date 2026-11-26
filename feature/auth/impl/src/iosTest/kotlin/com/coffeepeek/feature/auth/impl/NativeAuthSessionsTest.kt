package com.coffeepeek.feature.auth.impl

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class NativeAuthSessionsTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun closingLoginCancelsTheViewModelBeforeReportingALateSignIn() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val repository = FakeAuthRepository().apply { this.gate = gate }
        val analytics = FakeAnalytics()
        val login = NativeLoginSession(repository, analytics, apple = null)
        login.emailChanged("name@example.com")
        login.passwordChanged("password123")
        login.submit()
        runCurrent()

        assertTrue(login.current().busy)
        login.close()
        gate.complete(Unit)
        runCurrent()
        assertTrue(analytics.signedIn.isEmpty())
    }

    @Test
    fun closingRecoveryCancelsTheRequestWithItsPrefilledEmail() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val repository = FakeAuthRepository().apply { this.gate = gate }
        val recovery = NativePasswordRecoverySession("name@example.com", repository)
        recovery.submit()
        runCurrent()

        assertEquals(listOf("name@example.com"), repository.resets)
        assertTrue(recovery.current().busy)
        recovery.close()
        gate.complete(Unit)
        runCurrent()
        assertFalse(recovery.current().emailSent)
    }
}
