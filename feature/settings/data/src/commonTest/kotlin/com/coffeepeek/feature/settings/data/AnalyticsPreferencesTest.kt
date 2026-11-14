package com.coffeepeek.feature.settings.data

import com.coffeepeek.room.model.Setting
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class AnalyticsPreferencesTest {
    @Test fun defaultComesFromServerAndFalseSurvivesReinstall() = runTest {
        val f = PreferenceFixture()
        f.repository.prepare().getOrThrow()
        assertTrue(f.repository.load("A").getOrThrow().enabled)
        f.repository.set("A", false).getOrThrow()
        val reinstalled = createAnalyticsPreferences(f.client, f.sessions, PreferenceSettings())
        reinstalled.prepare().getOrThrow()
        assertFalse(reinstalled.load("A").getOrThrow().enabled)
    }

    @Test fun failureNeverInventsAnEnabledServerPreference() = runTest {
        val f = PreferenceFixture().apply { fail = true }
        assertTrue(f.repository.load("A").isFailure)
    }

    @Test fun offlineDisableStaysPendingAndRetriesAfterRestart() = runTest {
        val f = PreferenceFixture().apply { fail = true }
        assertTrue(f.repository.set("A", false).isFailure)
        f.fail = false
        val restarted = createAnalyticsPreferences(f.client, f.sessions, f.settings)
        assertFalse(restarted.load("A").getOrThrow().enabled)
        assertFalse(f.server.getValue("A"))
        assertTrue(f.settings.readAll().isEmpty())
    }

    @Test fun pendingChoiceIsNotAppliedToAnotherAccount() = runTest {
        val f = PreferenceFixture().apply { fail = true }
        f.repository.set("A", false)
        f.sessions.switch("B"); f.fail = false
        assertTrue(f.repository.load("B").getOrThrow().enabled)
        assertTrue(f.requests.isEmpty())
        assertTrue(f.repository.set("A", false).isFailure)
        f.sessions.switch("A")
        assertFalse(f.repository.load("A").getOrThrow().enabled)
    }

    @Test fun localDeclineMigratesOnlyToTheRestoredAccount() = runTest {
        val f = PreferenceFixture()
        f.settings.save(Setting("coffeepeek.analyticsConsent.v1", "declined"))
        f.repository.prepare().getOrThrow()
        f.sessions.switch("B")
        assertTrue(f.repository.load("B").getOrThrow().enabled)
        f.sessions.switch("A")
        assertFalse(f.repository.load("A").getOrThrow().enabled)
        assertNull(f.settings.read("coffeepeek.analyticsConsent.v1"))
    }

    @Test fun newerServerRefusalWinsOverLegacyAcceptance() = runTest {
        val f = PreferenceFixture()
        f.server["A"] = false; f.explicit += "A"
        f.settings.save(Setting("coffeepeek.analyticsConsent.v1", "accepted"))
        f.repository.prepare().getOrThrow()
        assertFalse(f.repository.load("A").getOrThrow().enabled)
        assertTrue(f.settings.readAll().isEmpty())
    }

    @Test fun unboundGuestChoiceDoesNotLeakIntoFutureLogin() = runTest {
        val f = PreferenceFixture()
        f.sessions.switch(null)
        f.settings.save(Setting("coffeepeek.analyticsConsent.v1", "declined"))
        f.repository.prepare().getOrThrow()
        f.sessions.switch("A")
        assertTrue(f.repository.load("A").getOrThrow().enabled)
        assertTrue(f.requests.isEmpty())
    }

    @Test fun wrongResponseOwnerAndCancelledRequestsAreRejected() = runTest {
        val f = PreferenceFixture().apply { responseOwner = "B" }
        assertTrue(f.repository.load("A").isFailure)
        val cancelled = CancellationException("cancelled")
        f.beforeResponse = { throw cancelled }
        assertFailsWith<CancellationException> { f.repository.load("A") }
    }

    @Test fun responseAfterAccountSwitchCannotRestoreOldAccount() = runTest {
        val f = PreferenceFixture()
        val started = CompletableDeferred<Unit>(); val release = CompletableDeferred<Unit>()
        f.beforeResponse = { started.complete(Unit); release.await() }
        val result = async { f.repository.load("A") }
        started.await(); f.sessions.switch("B"); release.complete(Unit)
        assertTrue(result.await().isFailure)
    }
}
