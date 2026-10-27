package com.coffeepeek.admin.notifications

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NativePushBridgeTest {
    @AfterTest fun clearAccount() = NativePushBridge.setAccount(null)

    @Test fun registrationStatusBelongsToTheCurrentAccountAndClearsOnSuccess() {
        NativePushBridge.setAccount("alice")
        NativePushBridge.updateRegistrationStatus("alice", "APNS_PENDING")
        assertEquals("APNS_PENDING", NativePushBridge.state.value.registrationDiagnostic)
        NativePushBridge.updateRegistrationStatus("bob", "FCM_7")
        assertEquals("APNS_PENDING", NativePushBridge.state.value.registrationDiagnostic)
        NativePushBridge.updateToken("alice", "token")
        assertNull(NativePushBridge.state.value.registrationDiagnostic)
        NativePushBridge.updateRegistrationStatus("alice", "FCM_7")
        assertNull(NativePushBridge.state.value.registrationDiagnostic)
        assertEquals("token", NativePushBridge.state.value.token)
    }

    @Test fun accountSwitchClearsStatusAndIgnoresLateNativeCallbacks() {
        NativePushBridge.setAccount("alice")
        NativePushBridge.updateRegistrationStatus("alice", "RESET_7")
        NativePushBridge.setAccount("bob")
        NativePushBridge.updateRegistrationStatus("alice", "APNS_3000")
        NativePushBridge.updateToken("alice", "old-token")
        assertNull(NativePushBridge.state.value.registrationDiagnostic)
        assertNull(NativePushBridge.state.value.token)
        NativePushBridge.updateRegistrationStatus("bob", "FCM_PENDING")
        assertEquals("FCM_PENDING", NativePushBridge.state.value.registrationDiagnostic)
    }
}
