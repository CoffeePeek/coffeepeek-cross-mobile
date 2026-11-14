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
    primaryContainer     = CoffeePeekColors.DarkSurface,
    onPrimaryContainer   = CoffeePeekColors.DarkTextPrimary,
    secondary            = CoffeePeekColors.GoldWarm,
    onSecondary          = CoffeePeekColors.DarkTextOnPrimary,
    secondaryContainer   = CoffeePeekColors.GoldWarm,
    onSecondaryContainer = CoffeePeekColors.DarkTextOnPrimary,
    tertiary             = CoffeePeekColors.Success,
    onTertiary           = CoffeePeekColors.DarkTextOnPrimary,
    tertiaryContainer    = CoffeePeekColors.DarkSurface,
    onTertiaryContainer  = CoffeePeekColors.Success,
    background           = CoffeePeekColors.DarkBackground,
    onBackground         = CoffeePeekColors.DarkTextPrimary,
    surface              = CoffeePeekColors.DarkSurface,
    onSurface            = CoffeePeekColors.DarkTextPrimary,
    surfaceVariant       = CoffeePeekColors.DarkBorder,
    onSurfaceVariant     = CoffeePeekColors.DarkTextSecondary,
    outline              = CoffeePeekColors.DarkBorder,
    outlineVariant       = CoffeePeekColors.DarkBorder,
    error                = CoffeePeekColors.ErrorTextDark,
    onError              = CoffeePeekColors.DarkTextOnPrimary,
    errorContainer       = CoffeePeekColors.ErrorTextLight,
    onErrorContainer     = CoffeePeekColors.DarkTextPrimary,
    inverseSurface       = CoffeePeekColors.LightSurface,
    inverseOnSurface     = CoffeePeekColors.LightTextPrimary,
    inversePrimary       = CoffeePeekColors.LightTextPrimary,
    surfaceTint          = Color.Transparent,
    surfaceDim           = CoffeePeekColors.DarkBackground,
    surfaceBright        = CoffeePeekColors.DarkBorder,
    surfaceContainerLowest = CoffeePeekColors.DarkBackground,
    surfaceContainerLow  = CoffeePeekColors.DarkSurface,
    surfaceContainer     = CoffeePeekColors.DarkSurface,
    surfaceContainerHigh = CoffeePeekColors.DarkBorder,
    surfaceContainerHighest = CoffeePeekColors.DarkBorder,
    scrim                = CoffeePeekColors.Overlay,
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
    tertiaryContainer    = CoffeePeekColors.LightBadge,
    onTertiaryContainer  = CoffeePeekColors.SuccessTextLight,
    background           = CoffeePeekColors.LightBackground,
    onBackground         = CoffeePeekColors.LightTextPrimary,
    surface              = CoffeePeekColors.LightSurface,
    onSurface            = CoffeePeekColors.LightTextPrimary,
    surfaceVariant       = CoffeePeekColors.LightBadge,
    onSurfaceVariant     = CoffeePeekColors.LightTextSecondary,
    outline              = CoffeePeekColors.LightBorder,
    outlineVariant       = CoffeePeekColors.LightBorderSubtle,
    error                = CoffeePeekColors.ErrorTextLight,
    onError              = Color.White,
    errorContainer       = CoffeePeekColors.ErrorTextDark,
    onErrorContainer     = CoffeePeekColors.LightTextPrimary,
    inverseSurface       = CoffeePeekColors.DarkSurface,
    inverseOnSurface     = CoffeePeekColors.DarkTextPrimary,
    inversePrimary       = CoffeePeekColors.Primary,
    surfaceTint          = Color.Transparent,
    surfaceDim           = Color(0xFFE8E4E0),
    surfaceBright        = CoffeePeekColors.LightSurface,
    surfaceContainerLowest = CoffeePeekColors.LightSurface,
    surfaceContainerLow  = CoffeePeekColors.LightSurfaceAlt,
    surfaceContainer     = CoffeePeekColors.LightBadge,
    surfaceContainerHigh = Color(0xFFE8E4E0),
    surfaceContainerHighest = Color(0xFFE8E4E0),
    scrim                = CoffeePeekColors.Overlay,
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
