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
private val DarkColorScheme: ColorScheme = darkColorScheme(
    primary              = CoffeePeekColors.Primary,
    onPrimary            = CoffeePeekColors.DarkTextOnPrimary,
    primaryContainer     = CoffeePeekColors.DarkSurfaceAlt,
    onPrimaryContainer   = CoffeePeekColors.DarkTextPrimary,
    secondary            = CoffeePeekColors.GoldWarm,
    onSecondary          = CoffeePeekColors.DarkTextOnPrimary,
    secondaryContainer   = CoffeePeekColors.GoldWarmHover,
    onSecondaryContainer = CoffeePeekColors.DarkTextPrimary,
    tertiary             = CoffeePeekColors.Success,
    onTertiary           = Color.White,
    tertiaryContainer    = Color(0xFF14532D),
    onTertiaryContainer  = Color(0xFFDCFCE7),
    background           = CoffeePeekColors.DarkBackground,
    onBackground         = CoffeePeekColors.DarkTextPrimary,
    surface              = CoffeePeekColors.DarkSurface,
    onSurface            = CoffeePeekColors.DarkTextPrimary,
    surfaceVariant       = CoffeePeekColors.DarkBorder,
    onSurfaceVariant     = CoffeePeekColors.DarkTextSecondary,
    outline              = CoffeePeekColors.DarkBorder,
    outlineVariant       = CoffeePeekColors.DarkBorderSubtle,
    error                = CoffeePeekColors.Error,
    onError              = Color.White,
    scrim                = CoffeePeekColors.DarkOverlay,
)

private val LightColorScheme: ColorScheme = lightColorScheme(
    primary              = CoffeePeekColors.Primary,
    onPrimary            = CoffeePeekColors.LightTextOnPrimary,
    primaryContainer     = CoffeePeekColors.LightSurfaceAlt,
    onPrimaryContainer   = CoffeePeekColors.LightTextPrimary,
    secondary            = CoffeePeekColors.GoldWarm,
    onSecondary          = CoffeePeekColors.LightTextOnPrimary,
    secondaryContainer   = CoffeePeekColors.GoldWarmSoft,
    onSecondaryContainer = CoffeePeekColors.LightTextPrimary,
    tertiary             = CoffeePeekColors.Success,
    onTertiary           = Color.White,
    tertiaryContainer    = Color(0xFFDCFCE7),
    onTertiaryContainer  = Color(0xFF14532D),
    background           = CoffeePeekColors.LightBackground,
    onBackground         = CoffeePeekColors.LightTextPrimary,
    surface              = CoffeePeekColors.LightSurface,
    onSurface            = CoffeePeekColors.LightTextPrimary,
    surfaceVariant       = CoffeePeekColors.LightBadge,
    onSurfaceVariant     = CoffeePeekColors.LightTextSecondary,
    outline              = CoffeePeekColors.LightBorder,
    outlineVariant       = CoffeePeekColors.LightBorderSubtle,
    error                = CoffeePeekColors.Error,
    onError              = Color.White,
    scrim                = CoffeePeekColors.LightOverlay,
)

@Composable
fun CoffeePeekTheme(
    fontFamily: FontFamily = CoffeePeekFontFamily,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CoffeePeekPlatformTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography  = coffeePeekTypography(fontFamily),
        shapes      = CpShapes,
        content     = content,
    )
}
