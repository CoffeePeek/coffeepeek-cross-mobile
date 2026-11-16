package com.coffeepeek.feature.auth.impl

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
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
class NativeRegisterSessionTest {
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
    fun defaultsPreserveTheExistingConsentChoices() = runTest(dispatcher) {
        val registration = NativeRegisterSession(FakeAuthRepository(), FakeAnalytics(), apple = null)
        assertFalse(registration.current().termsAccepted)
        assertTrue(registration.current().analyticsAccepted)
        assertFalse(registration.current().canSubmit)
        assertFalse(registration.current().success)
        assertFalse(registration.appleAvailable)
        registration.close()
    }

    @Test
    fun passwordRegistrationShowsConfirmationEmailWithoutReportingSignedIn() = runTest(dispatcher) {
        val repository = FakeAuthRepository()
        val analytics = FakeAnalytics()
        val registration = NativeRegisterSession(repository, analytics, apple = null)
        var signedIn = false
        backgroundScope.launch { registration.signedIn.collect { signedIn = true } }
        fillValidForm(registration)
        registration.analyticsChanged(false)
        runCurrent()
        assertTrue(registration.current().canSubmit)
        registration.submit()
        runCurrent()
        assertTrue(registration.current().success)
        assertEquals("name@example.com", registration.current().email)
        assertEquals(listOf(false), analytics.consents)
        assertEquals(listOf("name@example.com"), analytics.signUps)
        assertEquals(1, repository.registrations.size)
        assertFalse(signedIn)
        registration.close()
    }

    @Test
    fun closingRegistrationCancelsEmailCheckBeforeCreatingAnAccount() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val repository = FakeAuthRepository().apply { emailCheckGate = gate }
        val analytics = FakeAnalytics()
        val registration = NativeRegisterSession(repository, analytics, apple = null)
        fillValidForm(registration)
        registration.submit()
        runCurrent()
        assertTrue(registration.current().busy)
        registration.close()
        gate.complete(Unit)
        runCurrent()
        assertTrue(repository.registrations.isEmpty())
        assertTrue(analytics.consents.isEmpty())
        assertFalse(registration.current().success)
    }

    private fun fillValidForm(registration: NativeRegisterSession) {
        registration.nameChanged("Name")
        registration.emailChanged(" name@example.com ")
        registration.passwordChanged("password123")
        registration.termsChanged(true)
    }
}
