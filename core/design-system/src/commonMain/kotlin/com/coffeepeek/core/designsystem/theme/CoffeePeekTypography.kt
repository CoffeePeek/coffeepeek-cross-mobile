package com.coffeepeek.core.designsystem.theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.ui.tooling.preview.Preview

fun coffeePeekTypography(fontFamily: FontFamily): Typography {
    return Typography(
        displayLarge = coffeePeekTextStyle(fontFamily, 88, FontWeight.ExtraBold, 96.sp),
        displayMedium = coffeePeekTextStyle(fontFamily, 44, FontWeight.Bold, 52.sp),
        displaySmall = coffeePeekTextStyle(fontFamily, 36, FontWeight.Bold, 44.sp),
        headlineLarge = coffeePeekTextStyle(fontFamily, 34, FontWeight.Bold, 41.sp),
        headlineMedium = coffeePeekTextStyle(fontFamily, 28, FontWeight.Bold, 34.sp),
        headlineSmall = coffeePeekTextStyle(fontFamily, 22, FontWeight.Bold, 28.sp),
        titleLarge = coffeePeekTextStyle(fontFamily, 22, FontWeight.Bold, 28.sp),
        titleMedium = coffeePeekTextStyle(fontFamily, 17, FontWeight.SemiBold, 22.sp),
        titleSmall = coffeePeekTextStyle(fontFamily, 15, FontWeight.SemiBold, 20.sp),
        bodyLarge = coffeePeekTextStyle(fontFamily, 17, FontWeight.Normal, 24.sp),
        bodyMedium = coffeePeekTextStyle(fontFamily, 15, FontWeight.Normal, 20.sp),
        bodySmall = coffeePeekTextStyle(fontFamily, 13, FontWeight.Normal, 18.sp),
        labelLarge = coffeePeekTextStyle(fontFamily, 17, FontWeight.SemiBold, 22.sp),
        labelMedium = coffeePeekTextStyle(fontFamily, 15, FontWeight.Medium, 20.sp),
        labelSmall = coffeePeekTextStyle(fontFamily, 12, FontWeight.Medium, 16.sp),
    )
}

private fun coffeePeekTextStyle(
    family: FontFamily,
    size: Int,
    weight: FontWeight,
    lineHeight: TextUnit,
): TextStyle = TextStyle(
    fontFamily = family,
    fontSize = size.sp,
    fontWeight = weight,
    letterSpacing = 0.sp,
    lineHeight = lineHeight,
)

@Composable
private fun CoffeePeekTypographyPreviewContent(darkTheme: Boolean, fontScale: Float = 1f) = CoffeePeekTheme(darkTheme = darkTheme) {
    CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale)) {
        Surface {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val typography = MaterialTheme.typography
                listOf(typography.headlineLarge, typography.headlineMedium, typography.headlineSmall,
                    typography.titleLarge, typography.titleMedium, typography.titleSmall,
                    typography.bodyLarge, typography.bodyMedium, typography.bodySmall,
                    typography.labelLarge, typography.labelMedium, typography.labelSmall).forEach { style ->
                    Text("Кофе · Кава · Coffee — ${style.fontSize.value}/${style.lineHeight.value}", style = style)
                }
            }
        }
    }
}

@Preview @Composable private fun CoffeePeekTypographyLightPreview() = CoffeePeekTypographyPreviewContent(false)
@Preview @Composable private fun CoffeePeekTypographyDarkPreview() = CoffeePeekTypographyPreviewContent(true)
@Preview @Composable private fun CoffeePeekTypographyLargeLightPreview() = CoffeePeekTypographyPreviewContent(false, 2f)
@Preview @Composable private fun CoffeePeekTypographyLargeDarkPreview() = CoffeePeekTypographyPreviewContent(true, 2f)
