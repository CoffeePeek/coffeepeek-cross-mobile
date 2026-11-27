package com.coffeepeek.admin.location

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LocationPermissionRequestTest {
    @Test fun firstRequestDoesNotMistakeMissingRationaleForPermanentDenial() {
        assertEquals(LocationPermissionAction.Request, locationPermissionAction(false, false, false))
    }

    @Test fun aSoftDenialStillAllowsTheNextExplicitRequest() {
        assertEquals(LocationPermissionOutcome.Denied, locationPermissionOutcome(false, true))
        assertEquals(LocationPermissionAction.Request, locationPermissionAction(false, true, true))
    }

    @Test fun aPermanentDenialOffersSettingsWithoutAttemptingAnotherSystemPrompt() {
        assertEquals(LocationPermissionOutcome.PermanentlyDenied, locationPermissionOutcome(false, false))
        assertEquals(LocationPermissionAction.Settings, locationPermissionAction(false, true, false))
        assertEquals(LocationPermissionAction.Granted, locationPermissionAction(true, true, false))
    }

    @Test fun settingsGrantReachesTheSameLiveRequestExactlyOnce() {
        val request = LocationPermissionRequest()
        assertTrue(request.start(1))
        assertTrue(request.deliver(LocationPermissionOutcome.PermanentlyDenied))
        assertFalse(request.deliver(LocationPermissionOutcome.PermanentlyDenied))
        assertTrue(request.deliver(LocationPermissionOutcome.Granted))
        assertFalse(request.deliver(LocationPermissionOutcome.Granted))
        assertFalse(request.start(1))
    }

    @Test fun aNewExplicitTapIsNotBlockedByThePreviousDenial() {
        val request = LocationPermissionRequest()
        assertTrue(request.start(1))
        assertTrue(request.deliver(LocationPermissionOutcome.Denied))
        assertTrue(request.start(2))
        assertTrue(request.deliver(LocationPermissionOutcome.Granted))
        assertFalse(request.start(1))
    }

    @Test fun settingsGrantWaitsUntilTheAppResumesBeforeDeliveringTheLocationAction() {
        val request = LocationPermissionRequest()
        assertTrue(request.start(1))
        assertTrue(request.deliver(LocationPermissionOutcome.PermanentlyDenied))
        assertFalse(request.deliver(LocationPermissionOutcome.Granted, active = false))
        assertTrue(request.deliver(LocationPermissionOutcome.Granted, active = true))
        assertFalse(request.deliver(LocationPermissionOutcome.Granted, active = true))
    }

    @Test fun unrequestedAndDisposedSourcesIgnoreAuthorizationChanges() {
        val request = LocationPermissionRequest()
        assertFalse(request.start(0))
        assertFalse(request.deliver(LocationPermissionOutcome.Granted))
        assertTrue(request.start(1))
        request.close()
        assertFalse(request.deliver(LocationPermissionOutcome.Granted))
        assertFalse(request.start(2))
    }
}
