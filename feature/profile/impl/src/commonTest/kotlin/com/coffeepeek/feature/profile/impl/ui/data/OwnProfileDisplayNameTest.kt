package com.coffeepeek.feature.profile.impl.ui.data

import com.coffeepeek.feature.profile.api.OwnProfile
import com.coffeepeek.feature.profile.domain.ProfileAddress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OwnProfileDisplayNameTest {
    private val own = OwnProfile("Anna Coffee", "private@example.invalid", null, null, 0, 0, 0, 0,
        ProfileAddress("anna-coffee", "/users/anna-coffee", 1, false))

    @Test fun theEditableNameWinsOverTheUrlSlugAndAnOlderPublicProfile() {
        assertEquals("Anna Coffee", ownProfileDisplayName(own, "Old nickname"))
        assertEquals("anna-coffee", own.address?.slug)
    }

    @Test fun theOwnNameDoesNotWaitForThePublicProfile() {
        assertEquals("Anna Coffee", ownProfileDisplayName(own, null))
    }

    @Test fun aBlankOwnNameFallsBackToThePublicName() {
        assertEquals("Public name", ownProfileDisplayName(own.copy(userName = " \t"), "Public name"))
    }

    @Test fun blankNamesFallBackToTheCanonicalSlugWithoutAddingAnAtSign() {
        assertEquals("anna-coffee", ownProfileDisplayName(own.copy(userName = ""), " \n"))
        assertEquals("anna-coffee", ownProfileDisplayName(own.copy(userName = ""), null))
    }

    @Test fun aMissingAddressDoesNotPreventUsingThePublicName() {
        assertEquals("Public name", ownProfileDisplayName(own.copy(userName = "", address = null), "Public name"))
    }

    @Test fun missingPublicNamesNeverFallBackToThePrivateEmail() {
        assertEquals("", ownProfileDisplayName(own.copy(userName = " ", address = null), null))
        assertEquals("", ownProfileDisplayName(own.copy(userName = "", address = own.address?.copy(slug = " ")), ""))
    }

    @Test fun aSignedOutIdentityCannotReuseAStalePublicName() {
        assertNull(ownProfileDisplayName(null, "Previous account"))
    }
}
