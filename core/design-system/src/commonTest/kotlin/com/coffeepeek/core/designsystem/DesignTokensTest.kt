package com.coffeepeek.core.designsystem

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekColors
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.coffeePeekTypography
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class DesignTokensTest {
    @Test fun sharedDimensionsKeepLegacyValues() {
        assertEquals(44.dp, CoffeePeekDimens.controlHeight)
        assertEquals(22.dp, CoffeePeekDimens.buttonRadius)
        assertEquals(16.dp, CoffeePeekDimens.radiusLg)
        assertEquals(24.dp, CoffeePeekDimens.cardPadding)
    }

    @Test fun typographyUsesSuppliedFontInsteadOfApplicationResources() {
        val typography = coffeePeekTypography(FontFamily.Monospace)
        assertEquals(FontFamily.Monospace, typography.bodyLarge.fontFamily)
        assertEquals(16.sp, typography.bodyLarge.fontSize)
        assertEquals(24.sp, typography.bodyLarge.lineHeight)
        assertEquals(FontFamily.Monospace, typography.labelLarge.fontFamily)
    }

    @Test fun iconsKeepIntentionalAliases() {
        assertSame(CoffeePeekIcons.Star, CoffeePeekIcons.StarOutline)
        assertSame(CoffeePeekIcons.Location, CoffeePeekIcons.MyLocation)
        assertTrue(CoffeePeekIcons.Close.defaultWidth.value > 0)
    }

    @Test fun paletteKeepsBrandColor() {
        assertEquals(0xFFEAB308.toInt(), CoffeePeekColors.Primary.toArgb())
    }
}
