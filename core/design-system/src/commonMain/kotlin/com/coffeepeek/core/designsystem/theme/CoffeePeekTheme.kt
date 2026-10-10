package com.coffeepeek.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
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
    medium = RoundedCornerShape(CpDimens.radiusMd),
    large = RoundedCornerShape(CpDimens.radiusLg),
    extraLarge = RoundedCornerShape(CpDimens.radius2xl),
)
private val DarkColorScheme: ColorScheme = darkColorScheme(
    primary              = CpColor.Primary,
    onPrimary            = CpColor.DarkTextOnPrimary,
    primaryContainer     = CpColor.DarkSurfaceAlt,
    onPrimaryContainer   = CpColor.DarkTextPrimary,
    secondary            = CpColor.GoldWarm,
    onSecondary          = CpColor.DarkTextOnPrimary,
    secondaryContainer   = CpColor.GoldWarmHover,
    onSecondaryContainer = CpColor.DarkTextPrimary,
    tertiary             = CpColor.Success,
    onTertiary           = Color.White,
    tertiaryContainer    = Color(0xFF14532D),
    onTertiaryContainer  = Color(0xFFDCFCE7),
    background           = CpColor.DarkBackground,
    onBackground         = CpColor.DarkTextPrimary,
    surface              = CpColor.DarkSurface,
    onSurface            = CpColor.DarkTextPrimary,
    surfaceVariant       = CpColor.DarkBorder,
    onSurfaceVariant     = CpColor.DarkTextSecondary,
    outline              = CpColor.DarkBorder,
    outlineVariant       = CpColor.DarkBorderSubtle,
    error                = CpColor.Error,
    onError              = Color.White,
    scrim                = CpColor.DarkOverlay,
)

private val LightColorScheme: ColorScheme = lightColorScheme(
    primary              = CpColor.Primary,
    onPrimary            = CpColor.LightTextOnPrimary,
    primaryContainer     = CpColor.LightSurfaceAlt,
    onPrimaryContainer   = CpColor.LightTextPrimary,
    secondary            = CpColor.GoldWarm,
    onSecondary          = CpColor.LightTextOnPrimary,
    secondaryContainer   = CpColor.GoldWarmSoft,
    onSecondaryContainer = CpColor.LightTextPrimary,
    tertiary             = CpColor.Success,
    onTertiary           = Color.White,
    tertiaryContainer    = Color(0xFFDCFCE7),
    onTertiaryContainer  = Color(0xFF14532D),
    background           = CpColor.LightBackground,
    onBackground         = CpColor.LightTextPrimary,
    surface              = CpColor.LightSurface,
    onSurface            = CpColor.LightTextPrimary,
    surfaceVariant       = CpColor.LightBadge,
    onSurfaceVariant     = CpColor.LightTextSecondary,
    outline              = CpColor.LightBorder,
    outlineVariant       = CpColor.LightBorderSubtle,
    error                = CpColor.Error,
    onError              = Color.White,
    scrim                = CpColor.LightOverlay,
)

@Composable
fun CoffeePeekTheme(
    fontFamily: FontFamily = Manrope,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography  = cpTypography(fontFamily),
        shapes      = CpShapes,
        content     = content,
    )
}
