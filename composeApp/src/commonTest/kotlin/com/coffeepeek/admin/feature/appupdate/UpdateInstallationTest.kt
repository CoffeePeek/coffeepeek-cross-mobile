package com.coffeepeek.admin.feature.appupdate

import com.coffeepeek.admin.feature.appupdate.domain.InstallationState
import com.coffeepeek.admin.feature.appupdate.domain.isCompatibleUpdate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UpdateInstallationTest {
    @Test
    fun acceptsOnlyNewerOwnPackageWithTrustedSigningLineage() {
        fun accepts(
            packageName: String = "com.coffeepeek",
            version: Long = 141,
            current: Long = 140,
            installed: Set<String> = setOf("trusted"),
            signers: Set<String> = setOf("trusted"),
            history: Set<String> = signers,
        ) = isCompatibleUpdate("com.coffeepeek", 141, current, packageName, version, installed, signers, history)
        assertTrue(accepts())
        assertTrue(accepts(signers = setOf("rotated"), history = setOf("trusted", "rotated")))
        assertFalse(accepts(packageName = "other.app"))
        assertFalse(accepts(version = 142))
        assertFalse(accepts(current = 141))
        assertFalse(accepts(signers = setOf("untrusted")))
        assertFalse(accepts(installed = emptySet()))
        assertFalse(accepts(signers = emptySet(), history = setOf("trusted")))
        assertTrue(accepts(installed = setOf("a", "b"), signers = setOf("a", "b")))
        assertFalse(accepts(installed = setOf("a", "b"), signers = setOf("a"), history = setOf("a", "b")))
    }

    @Test
    fun unknownDownloadSizeAndProgressBounds() {
        assertNull(InstallationState(downloadedBytes = 10).progress)
        assertNull(InstallationState(totalBytes = -1).progress)
        assertEquals(0.5f, InstallationState(downloadedBytes = 50, totalBytes = 100).progress)
        assertEquals(1f, InstallationState(downloadedBytes = 200, totalBytes = 100).progress)
        assertEquals(0f, InstallationState(downloadedBytes = -1, totalBytes = 100).progress)
    }
}
