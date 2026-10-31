package com.coffeepeek.admin.ui.component

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.coffeepeek.admin.theme.toUIColor
import com.coffeepeek.core.designsystem.theme.CoffeePeekColors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import platform.UIKit.UIDevice

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
    fun loadingFavoriteKeepsSpinnerAndGold() {
        val configuration = glassIconConfiguration(GlassControlIcon.FavoriteFilled, true, true, colors)
        assertTrue(configuration.showsActivityIndicator)
        assertEquals(CoffeePeekColors.Like.toUIColor(), configuration.baseForegroundColor)
    }
}
