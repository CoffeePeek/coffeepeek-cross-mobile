package com.coffeepeek.core.session

import platform.Security.errSecAuthFailed
import platform.Security.errSecInteractionNotAllowed
import platform.Security.errSecItemNotFound
import platform.Security.errSecSuccess
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KeychainDeleteStatusTest {
    @Test fun aRemovedOrAlreadyMissingItemIsAHandledLogout() {
        assertTrue(keychainDeleteSucceeded(errSecSuccess))
        assertTrue(keychainDeleteSucceeded(errSecItemNotFound))
    }

    @Test fun aRealStorageFailureIsNotAHandledLogout() {
        assertFalse(keychainDeleteSucceeded(errSecInteractionNotAllowed))
        assertFalse(keychainDeleteSucceeded(errSecAuthFailed))
    }
}
