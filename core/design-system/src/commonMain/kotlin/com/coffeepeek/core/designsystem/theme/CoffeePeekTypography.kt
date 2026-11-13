package com.coffeepeek.core.designsystem.theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.ui.tooling.preview.Preview

fun coffeePeekTypography(fontFamily: FontFamily): Typography {
    val defaults = Typography()
    return Typography(
        displayLarge = coffeePeekTextStyle(fontFamily, 88, FontWeight.ExtraBold, 84.sp),
        displayMedium = defaults.displayMedium.copy(fontFamily = fontFamily),
        displaySmall = defaults.displaySmall.copy(fontFamily = fontFamily),
        headlineLarge = coffeePeekTextStyle(fontFamily, 36, FontWeight.Bold),
        headlineMedium = coffeePeekTextStyle(fontFamily, 24, FontWeight.Bold),
        headlineSmall = coffeePeekTextStyle(fontFamily, 20, FontWeight.Bold),
        titleLarge = coffeePeekTextStyle(fontFamily, 20, FontWeight.Bold),
        titleMedium = coffeePeekTextStyle(fontFamily, 16, FontWeight.SemiBold),
        titleSmall = coffeePeekTextStyle(fontFamily, 14, FontWeight.SemiBold),
        bodyLarge = coffeePeekTextStyle(fontFamily, 16, FontWeight.Normal, 24.sp),
        bodyMedium = coffeePeekTextStyle(fontFamily, 14, FontWeight.Normal, 21.sp),
        bodySmall = coffeePeekTextStyle(fontFamily, 13, FontWeight.Normal),
        labelLarge = coffeePeekTextStyle(fontFamily, 16, FontWeight.SemiBold),
        labelMedium = coffeePeekTextStyle(fontFamily, 14, FontWeight.Medium),
        labelSmall = coffeePeekTextStyle(fontFamily, 12, FontWeight.Medium),
    )
}

private fun coffeePeekTextStyle(
    family: FontFamily,
    size: Int,
    weight: FontWeight,
    lineHeight: TextUnit = TextUnit.Unspecified,
): TextStyle = TextStyle(
    fontFamily = family,
    fontSize = size.sp,
    fontWeight = weight,
    letterSpacing = 0.sp,
    lineHeight = lineHeight,
)

@Composable
private fun CoffeePeekTypographyPreviewContent(darkTheme: Boolean) = CoffeePeekTheme(darkTheme = darkTheme) {
    Surface {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(FontWeight.Light, FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold,
                FontWeight.Bold, FontWeight.ExtraBold).forEach { weight ->
                Text("Кофе · Кава · Coffee · ${weight.weight}", fontWeight = weight,
                    style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Preview @Composable private fun CoffeePeekTypographyLightPreview() = CoffeePeekTypographyPreviewContent(false)
@Preview @Composable private fun CoffeePeekTypographyDarkPreview() = CoffeePeekTypographyPreviewContent(true)
