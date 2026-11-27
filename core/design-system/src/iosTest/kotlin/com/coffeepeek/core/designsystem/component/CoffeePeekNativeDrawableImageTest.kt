@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.core.designsystem.component

import kotlinx.cinterop.useContents
import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import platform.UIKit.UIImageRenderingMode.UIImageRenderingModeAlwaysTemplate

/** Decode contracts only: no view, window, composition or screenshot. */
class CoffeePeekNativeDrawableImageTest {
    @Test fun rasterFallbackIsDecodedWithoutEnteringTheXmlParser() {
        val bytes = Base64.decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR4nGNgYGD4DwABBAEAX+XDSwAAAABJRU5ErkJggg==")
        val image = coffeePeekNativeDrawableImage(bytes, 28f, 1f)
        assertEquals(UIImageRenderingModeAlwaysTemplate, image.renderingMode)
        assertTrue(image.size.useContents { width > 0 && height > 0 })
    }

    @Test fun vectorArtworkKeepsTheRequestedPointSizeAndTemplateTint() {
        val bytes = """<vector xmlns:android="http://schemas.android.com/apk/res/android"
            android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
            <path android:fillColor="#000000" android:pathData="M0,0h24v24h-24z"/>
            </vector>""".encodeToByteArray()
        val image = coffeePeekNativeDrawableImage(bytes, 28f, 2f)
        assertEquals(UIImageRenderingModeAlwaysTemplate, image.renderingMode)
        assertEquals(28.0, image.size.useContents { width })
        assertEquals(28.0, image.size.useContents { height })
    }

    @Test fun emptyArtworkIsRejectedBeforeCallingPlatformDecoders() {
        assertFailsWith<IllegalArgumentException> { coffeePeekNativeDrawableImage(byteArrayOf()) }
    }
}
