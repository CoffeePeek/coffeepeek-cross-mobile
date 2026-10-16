@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.coffeepeek.admin.analytics

import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PasswordSignUpAnalyticsTest {
    @Test fun registrationIsTrackedOnceAfterMatchingPasswordLogin() = runTest {
        val sdk = RecordingTransport()
        val service = analytics(MemoryConsent(AnalyticsConsent.Accepted), sdk)
        service.setForeground(true)
        service.restoreConsent()
        service.syncIdentity(null)
        Analytics.install(service)

        service.rememberPasswordSignUp(" Person@example.invalid ")
        runCurrent()
        assertTrue(sdk.calls.isEmpty())
        assertTrue(sdk.events.isEmpty())

        signInAnalytics("backend-id", "password", "login", "person@example.invalid")
        runCurrent()
        val event = sdk.events.single { it.first == "sign_up_completed" }.second
        assertEquals("password", event["method"])
        assertEquals("signed_in", event["auth_state"])
        assertTrue("identify:backend-id" in sdk.calls)
        assertTrue(sdk.events.none { "example.invalid" in it.second.toString() })

        signInAnalytics("backend-id", "password", "login", "person@example.invalid")
        runCurrent()
        assertEquals(1, sdk.events.count { it.first == "sign_up_completed" })
    }

    @Test fun anotherAccountOrSocialLoginDiscardsPendingRegistration() = runTest {
        for ((method, email) in listOf("password" to "other@example.invalid", "google" to "person@example.invalid")) {
            val sdk = RecordingTransport()
            val service = analytics(MemoryConsent(AnalyticsConsent.Accepted), sdk)
            service.setForeground(true); service.restoreConsent(); service.syncIdentity(null)
            Analytics.install(service)
            service.rememberPasswordSignUp("person@example.invalid")
            signInAnalytics("other-id", method, "login", email)
            runCurrent()
            signInAnalytics("registered-id", "password", "login", "person@example.invalid")
            runCurrent()
            assertTrue(sdk.events.none { it.first == "sign_up_completed" })
        }
    }

    @Test fun revokingConsentDiscardsPendingRegistration() = runTest {
        val sdk = RecordingTransport()
        val service = analytics(MemoryConsent(AnalyticsConsent.Accepted), sdk)
        service.setForeground(true); service.restoreConsent(); service.syncIdentity(null)
        Analytics.install(service)
        service.rememberPasswordSignUp("person@example.invalid")
        service.setConsent(AnalyticsConsent.Declined)
        service.setConsent(AnalyticsConsent.Accepted)
        signInAnalytics("backend-id", "password", "login", "person@example.invalid")
        runCurrent()
        assertTrue(sdk.events.none { it.first == "sign_up_completed" })
    }

    @Test fun registrationWithoutConsentIsNotReplayedAfterOptIn() = runTest {
        val sdk = RecordingTransport()
        val service = analytics(MemoryConsent(AnalyticsConsent.Declined), sdk)
        service.setForeground(true); service.restoreConsent(); service.syncIdentity(null)
        Analytics.install(service)
        service.rememberPasswordSignUp("person@example.invalid")
        service.setConsent(AnalyticsConsent.Accepted)
        signInAnalytics("backend-id", "password", "login", "person@example.invalid")
        runCurrent()
        assertTrue(sdk.events.none { it.first == "sign_up_completed" })
    }
}
