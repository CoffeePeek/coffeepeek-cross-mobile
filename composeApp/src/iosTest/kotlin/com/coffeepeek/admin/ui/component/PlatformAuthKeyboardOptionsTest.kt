@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.coffeepeek.admin.ui.component

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.textContentType
import androidx.compose.ui.text.input.usingNativeTextInput
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import platform.UIKit.UITextContentTypeNewPassword
import platform.UIKit.UITextContentTypePassword
import platform.UIKit.UITextContentTypeUsername

class PlatformAuthKeyboardOptionsTest {
    @Test
    fun emailKeepsComposePasteHandlingAndUsernameAutofillThroughFieldDefaults() {
        val options = platformTextInputOptions(platformAuthKeyboardOptions(
            KeyboardOptions(keyboardType = KeyboardType.Email, autoCorrectEnabled = false, imeAction = ImeAction.Next),
            newPassword = false,
        ))
        val platformOptions = assertNotNull(options.platformImeOptions)

        assertFalse(platformOptions.usingNativeTextInput)
        assertEquals(UITextContentTypeUsername, platformOptions.textContentType)
        assertEquals(KeyboardType.Email, options.keyboardType)
        assertEquals(ImeAction.Next, options.imeAction)
        assertEquals(false, options.autoCorrectEnabled)
        assertEquals(KeyboardCapitalization.None, options.capitalization)
    }

    @Test
    fun loginAndRegistrationPasswordsRetainTheirAutofillRoles() {
        for (newPassword in listOf(false, true)) {
            val options = platformTextInputOptions(platformAuthKeyboardOptions(
                KeyboardOptions(keyboardType = KeyboardType.Password),
                newPassword = newPassword,
            ))
            val platformOptions = assertNotNull(options.platformImeOptions)

            assertFalse(platformOptions.usingNativeTextInput)
            assertEquals(if (newPassword) UITextContentTypeNewPassword else UITextContentTypePassword,
                platformOptions.textContentType)
        }
    }

    @Test
    fun ordinaryTextStillUsesNativeEditingAndSuggestions() {
        val options = platformTextInputOptions(KeyboardOptions())

        assertTrue(assertNotNull(options.platformImeOptions).usingNativeTextInput)
        assertEquals(true, options.autoCorrectEnabled)
        assertEquals(KeyboardCapitalization.Sentences, options.capitalization)
    }
}
