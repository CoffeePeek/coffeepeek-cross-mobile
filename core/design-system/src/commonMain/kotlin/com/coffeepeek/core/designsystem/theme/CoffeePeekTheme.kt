package com.coffeepeek.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

/** Buttons and single-line fields use a pill; larger surfaces keep their own radii. */
private val CpShapes = Shapes(
    extraSmall = RoundedCornerShape(percent = 50),
    small = RoundedCornerShape(percent = 50),
    medium = RoundedCornerShape(CoffeePeekDimens.radiusMd),
    large = RoundedCornerShape(CoffeePeekDimens.radiusLg),
    extraLarge = RoundedCornerShape(CoffeePeekDimens.radius2xl),
)
internal val CoffeePeekDarkColorScheme: ColorScheme = darkColorScheme(
    primary              = CoffeePeekColors.Primary,
    onPrimary            = CoffeePeekColors.DarkTextOnPrimary,
    primaryContainer     = CoffeePeekColors.DarkSurfaceAlt,
    onPrimaryContainer   = CoffeePeekColors.DarkTextPrimary,
    secondary            = CoffeePeekColors.GoldWarm,
    onSecondary          = CoffeePeekColors.DarkTextOnPrimary,
    secondaryContainer   = CoffeePeekColors.GoldWarmHover,
    onSecondaryContainer = CoffeePeekColors.DarkTextOnPrimary,
    tertiary             = CoffeePeekColors.Success,
    onTertiary           = CoffeePeekColors.DarkTextOnPrimary,
    tertiaryContainer    = Color(0xFF14532D),
    onTertiaryContainer  = Color(0xFFDCFCE7),
    background           = CoffeePeekColors.DarkBackground,
    onBackground         = CoffeePeekColors.DarkTextPrimary,
    surface              = CoffeePeekColors.DarkSurface,
    onSurface            = CoffeePeekColors.DarkTextPrimary,
    surfaceVariant       = CoffeePeekColors.DarkBorder,
    onSurfaceVariant     = CoffeePeekColors.DarkTextSecondary,
    outline              = CoffeePeekColors.DarkControlOutline,
    outlineVariant       = CoffeePeekColors.DarkBorderSubtle,
    error                = CoffeePeekColors.ErrorTextDark,
    onError              = CoffeePeekColors.DarkTextOnPrimary,
    errorContainer       = Color(0xFF5C1D1D),
    onErrorContainer     = Color(0xFFFEE2E2),
    inverseSurface       = CoffeePeekColors.LightSurface,
    inverseOnSurface     = CoffeePeekColors.LightTextPrimary,
    inversePrimary       = CoffeePeekColors.AccentTextLight,
    surfaceTint          = Color.Transparent,
    surfaceDim           = CoffeePeekColors.DarkBackground,
    surfaceBright        = Color(0xFF3D332C),
    surfaceContainerLowest = Color(0xFF14100E),
    surfaceContainerLow  = Color(0xFF231C18),
    surfaceContainer     = CoffeePeekColors.DarkSurface,
    surfaceContainerHigh = Color(0xFF352B25),
    surfaceContainerHighest = Color(0xFF3D332C),
    scrim                = CoffeePeekColors.DarkOverlay,
)

internal val CoffeePeekLightColorScheme: ColorScheme = lightColorScheme(
    primary              = CoffeePeekColors.Primary,
    onPrimary            = CoffeePeekColors.LightTextOnPrimary,
    primaryContainer     = CoffeePeekColors.LightSurfaceAlt,
    onPrimaryContainer   = CoffeePeekColors.LightTextPrimary,
    secondary            = CoffeePeekColors.GoldWarm,
    onSecondary          = CoffeePeekColors.LightTextOnPrimary,
    secondaryContainer   = CoffeePeekColors.GoldWarmSoft,
    onSecondaryContainer = CoffeePeekColors.LightTextPrimary,
    tertiary             = CoffeePeekColors.SuccessTextLight,
    onTertiary           = Color.White,
    tertiaryContainer    = Color(0xFFDCFCE7),
    onTertiaryContainer  = Color(0xFF14532D),
    background           = CoffeePeekColors.LightBackground,
    onBackground         = CoffeePeekColors.LightTextPrimary,
    surface              = CoffeePeekColors.LightSurface,
    onSurface            = CoffeePeekColors.LightTextPrimary,
    surfaceVariant       = CoffeePeekColors.LightBadge,
    onSurfaceVariant     = CoffeePeekColors.LightTextSecondary,
    outline              = CoffeePeekColors.LightControlOutline,
    outlineVariant       = CoffeePeekColors.LightBorderSubtle,
    error                = CoffeePeekColors.ErrorTextLight,
    onError              = Color.White,
    errorContainer       = Color(0xFFFEE2E2),
    onErrorContainer     = Color(0xFF7F1D1D),
    inverseSurface       = CoffeePeekColors.DarkSurface,
    inverseOnSurface     = CoffeePeekColors.DarkTextPrimary,
    inversePrimary       = CoffeePeekColors.AccentTextDark,
    surfaceTint          = Color.Transparent,
    surfaceDim           = Color(0xFFE8E4E0),
    surfaceBright        = CoffeePeekColors.LightSurface,
    surfaceContainerLowest = CoffeePeekColors.LightSurface,
    surfaceContainerLow  = CoffeePeekColors.LightSurfaceAlt,
    surfaceContainer     = CoffeePeekColors.LightBadge,
    surfaceContainerHigh = Color(0xFFF0EDEA),
    surfaceContainerHighest = Color(0xFFE8E4E0),
    scrim                = CoffeePeekColors.LightOverlay,
)

@Composable
fun CoffeePeekTheme(
    fontFamily: FontFamily = CoffeePeekFontFamily,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CoffeePeekPlatformTheme(
        colorScheme = if (darkTheme) CoffeePeekDarkColorScheme else CoffeePeekLightColorScheme,
        typography  = coffeePeekTypography(fontFamily),
        shapes      = CpShapes,
        content     = content,
    )
}
