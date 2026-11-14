package com.coffeepeek.core.designsystem.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CoffeePeekTypographyTest {
    @Test fun everySemanticRoleHasReadableSizeAndExplicitLeading() {
        val typography = coffeePeekTypography(FontFamily.Monospace)
        val roles = with(typography) { listOf(displayLarge, displayMedium, displaySmall,
            headlineLarge, headlineMedium, headlineSmall, titleLarge, titleMedium, titleSmall,
            bodyLarge, bodyMedium, bodySmall, labelLarge, labelMedium, labelSmall) }
        roles.forEach { style ->
            assertEquals(FontFamily.Monospace, style.fontFamily)
            assertTrue(style.fontSize.value >= 11f)
            assertTrue(style.lineHeight.value > style.fontSize.value)
            assertTrue((style.fontWeight ?: FontWeight.Normal) >= FontWeight.Normal)
        }
    }

    @Test fun bodyActionsAndSupportingTextRetainTheirHierarchy() {
        val t = coffeePeekTypography(FontFamily.Default)
        assertTrue(t.bodyLarge.fontSize.value >= 17f)
        assertTrue(t.labelLarge.fontSize >= t.bodyLarge.fontSize)
        assertTrue(t.bodyLarge.fontSize > t.bodyMedium.fontSize)
        assertTrue(t.bodyMedium.fontSize > t.bodySmall.fontSize)
        assertTrue(t.headlineLarge.fontSize > t.headlineMedium.fontSize)
        assertTrue(t.headlineMedium.fontSize > t.headlineSmall.fontSize)
    }
}
