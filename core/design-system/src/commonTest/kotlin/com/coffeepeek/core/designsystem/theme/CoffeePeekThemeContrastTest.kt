package com.coffeepeek.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import com.coffeepeek.core.designsystem.component.CoffeePeekIconBadgePalette
import com.coffeepeek.core.designsystem.component.coffeePeekIconBadgeColors
import kotlin.test.Test
import kotlin.test.assertTrue

class CoffeePeekThemeContrastTest {
    private val schemes = listOf(CoffeePeekLightColorScheme, CoffeePeekDarkColorScheme)

    @Test fun filledControlsAndContainersHaveReadableLabels() {
        schemes.forEach { scheme ->
            pairs(scheme).forEach { (foreground, background) -> assertContrast(foreground, background, 4.5f) }
        }
    }

    @Test fun textAndUnfilledActionsRemainReadableOnEverySurface() {
        schemes.forEach { scheme ->
            surfaces(scheme).forEach { background ->
                listOf(scheme.onSurface, scheme.onSurfaceVariant, scheme.coffeePeekAccentForeground, scheme.error)
                    .forEach { assertContrast(it, background, 4.5f) }
                assertContrast(scheme.outline, background, 3f)
            }
        }
    }

    @Test fun badgesRemainDistinctInBothThemesAndOnElevatedSurfaces() {
        val badges = with(CoffeePeekIconBadgePalette) {
            listOf(Mint, Cyan, Sky, Aqua, Lavender, Gold, Emerald, Blue, Rose, Violet, BrightCyan)
        }
        schemes.forEach { scheme ->
            surfaces(scheme).forEach { surface ->
                badges.forEach { badge ->
                    val colors = coffeePeekIconBadgeColors(badge, surface)
                    assertContrast(colors.icon, colors.background.compositeOver(surface), 3f)
                }
            }
        }
    }

    @Test fun photoLabelsAndSymbolsSurviveBlackAndWhiteImages() {
        listOf(Color.Black, Color.White).forEach { photo ->
            val background = CoffeePeekColors.PhotoBadgeOverlay.compositeOver(photo)
            assertContrast(Color.White, background, 4.5f)
            assertContrast(CoffeePeekColors.Primary, background, 3f)
        }
    }

    @Test fun statusLabelsUseTheActualThemeErrorAndSuccessFills() {
        schemes.forEach { scheme ->
            listOf(true, false).forEach { open ->
                val fill = if (open) CoffeePeekColors.Success else scheme.error
                assertContrast(scheme.coffeePeekStatusForeground(open), fill.copy(alpha = 0.14f).compositeOver(scheme.surface), 4.5f)
            }
        }
    }

    @Test fun moderationPillsRemainReadableOnTintedBackgrounds() {
        schemes.forEach { scheme ->
            listOf(scheme.tertiary, scheme.coffeePeekAccentForeground, scheme.error).forEach { color ->
                assertContrast(color, color.copy(alpha = 0.12f).compositeOver(scheme.surface), 4.5f)
            }
        }
    }

    @Test fun nativeSwitchSelectedTracksContrastWithTheSystemWhiteThumb() {
        listOf(CoffeePeekColors.AccentTextLight, CoffeePeekColors.SwitchCheckedTrackDark).forEach { track ->
            assertContrast(Color.White, track, 3f)
        }
    }

    private fun pairs(s: ColorScheme) = listOf(
        s.onPrimary to s.primary, s.onPrimaryContainer to s.primaryContainer,
        s.onSecondary to s.secondary, s.onSecondaryContainer to s.secondaryContainer,
        s.onTertiary to s.tertiary, s.onTertiaryContainer to s.tertiaryContainer,
        s.onError to s.error, s.onErrorContainer to s.errorContainer,
        s.onBackground to s.background, s.inverseOnSurface to s.inverseSurface,
        s.inversePrimary to s.inverseSurface,
    )

    private fun surfaces(s: ColorScheme) = listOf(s.background, s.surface, s.surfaceVariant,
        s.surfaceDim, s.surfaceBright, s.surfaceContainerLowest, s.surfaceContainerLow,
        s.surfaceContainer, s.surfaceContainerHigh, s.surfaceContainerHighest)

    private fun assertContrast(foreground: Color, background: Color, minimum: Float) {
        val visible = foreground.compositeOver(background)
        val high = maxOf(visible.luminance(), background.luminance())
        val low = minOf(visible.luminance(), background.luminance())
        val ratio = (high + 0.05f) / (low + 0.05f)
        assertTrue(ratio >= minimum, "Contrast $ratio < $minimum: $foreground on $background")
    }
}
