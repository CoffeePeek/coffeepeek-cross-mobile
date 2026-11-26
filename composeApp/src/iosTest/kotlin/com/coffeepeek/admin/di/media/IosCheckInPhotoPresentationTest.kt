@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.admin.di.media

import com.coffeepeek.core.designsystem.component.CoffeePeekPhotoViewerImages
import kotlinx.coroutines.flow.flowOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IosCheckInPhotoPresentationTest {
    @Test fun revokedAndDismissedViewerCannotLoadAnotherPrivateFrame() {
        var current = true
        var dismissed = 0
        var allowed: () -> Boolean = { false }
        val source = IosCheckInPhotoPresentation("ru", { current }, { dismissed++ }) { gate ->
            allowed = gate
            emptyViewer()
        }
        assertTrue(allowed())
        current = false
        assertFalse(allowed())
        source.dismiss()
        current = true
        source.dismiss()
        assertEquals(1, dismissed)
        assertFalse(allowed())
    }

    @Test fun disposedViewerIgnoresALateNativeDismissCallback() {
        var dismissed = 0
        val source = IosCheckInPhotoPresentation("ru", { true }, { dismissed++ }) { emptyViewer() }
        source.close()
        source.dismiss()
        assertFalse(source.isCurrent())
        assertEquals(0, dismissed)
    }

    private fun emptyViewer() = IosPhotoViewer(CoffeePeekPhotoViewerImages(listOf("preview")), 0) { flowOf(null) }
}
