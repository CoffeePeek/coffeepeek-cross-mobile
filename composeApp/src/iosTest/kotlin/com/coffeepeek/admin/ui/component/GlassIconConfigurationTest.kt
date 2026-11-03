package com.coffeepeek.admin.ui.component

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.coffeepeek.admin.theme.toUIColor
import com.coffeepeek.core.designsystem.theme.CoffeePeekColors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import platform.UIKit.UIAccessibilityTraitNotEnabled
import platform.UIKit.UIAccessibilityTraitSelected
import platform.UIKit.UIButton
import platform.UIKit.UIDevice
import platform.UIKit.accessibilityTraits

/** UIKit configuration contracts; no UI host, rendering, DI or real favorite writes. */
class GlassIconConfigurationTest {
    private val colors = lightColorScheme(primary = Color(0xFFE6B800), onSurface = Color(0xFF201A16))

    @Test
    fun bothFavoriteSelectionFormsKeepBrandGold() {
        listOf(GlassControlIcon.Favorite to true, GlassControlIcon.FavoriteFilled to false).forEach { (icon, selected) ->
            val configuration = glassIconConfiguration(icon, selected, false, colors)
            assertEquals(CoffeePeekColors.Like.toUIColor(), configuration.baseForegroundColor)
            if (UIDevice.currentDevice.systemVersion.substringBefore('.').toInt() >= 26) {
                assertFalse(configuration.automaticallyUpdateForSelection)
            }
        }
    }

    @Test
    fun mapSelectionUsesAccentAndResetRestoresNeutralForeground() {
        val selected = glassIconConfiguration(GlassControlIcon.Zones, true, false, colors)
        val cleared = glassIconConfiguration(GlassControlIcon.Zones, false, false, colors)
        assertEquals(colors.primary.toUIColor(), selected.baseForegroundColor)
        assertEquals(colors.onSurface.toUIColor(), cleared.baseForegroundColor)
    }

    @Test
    fun loadingFavoriteReservesBrandedLoaderSlotAndKeepsGold() {
        val configuration = glassIconConfiguration(GlassControlIcon.FavoriteFilled, true, true, colors)
        assertFalse(configuration.showsActivityIndicator)
        assertNotNull(configuration.image)
        assertEquals(CoffeePeekColors.Like.toUIColor(), configuration.baseForegroundColor)
    }

    @Test
    fun favoriteSelectionKeepsPlateNeutralAndAccessibilitySelected() {
        listOf(GlassControlIcon.Favorite to true, GlassControlIcon.FavoriteFilled to false).forEach { (icon, selected) ->
            val button = iconButton(icon, selected)
            assertFalse(button.selected)
            assertTrue(button.accessibilityTraits and UIAccessibilityTraitSelected != 0uL)
            assertEquals(CoffeePeekColors.Like.toUIColor(), button.configuration?.baseForegroundColor)
        }
    }

    @Test
    fun mapRetainsVisualSelectionAndClearsItOnReset() {
        val button = iconButton(GlassControlIcon.Zones, true)
        assertTrue(button.selected)
        assertTrue(button.accessibilityTraits and UIAccessibilityTraitSelected != 0uL)
        button.updateGlassIconSelection(GlassControlIcon.Zones, false)
        assertFalse(button.selected)
        assertTrue(button.accessibilityTraits and UIAccessibilityTraitSelected == 0uL)
    }

    @Test
    fun loadingFavoriteRetainsDisabledAndSelectedAccessibility() {
        val button = iconButton(GlassControlIcon.FavoriteFilled, true, loading = true)
        assertFalse(button.enabled)
        assertFalse(button.selected)
        assertTrue(button.accessibilityTraits and UIAccessibilityTraitSelected != 0uL)
        assertTrue(button.accessibilityTraits and UIAccessibilityTraitNotEnabled != 0uL)
    }

    private fun iconButton(icon: GlassControlIcon, selected: Boolean, loading: Boolean = false): UIButton =
        UIButton.buttonWithConfiguration(glassIconConfiguration(icon, selected, loading, colors), null).apply {
            enabled = !loading
            updateGlassIconSelection(icon, selected)
        }
}
