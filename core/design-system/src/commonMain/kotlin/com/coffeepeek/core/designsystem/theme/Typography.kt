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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.ui.tooling.preview.Preview

fun cpTypography(fontFamily: FontFamily): Typography {
    val manrope = fontFamily
    return Typography(
        displayLarge = TextStyle(
            fontFamily = manrope,
            fontSize = 88.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.sp,
            lineHeight = 84.sp,
        ),
        headlineLarge = TextStyle(
            fontFamily = manrope,
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.sp,
        ),
        headlineMedium = TextStyle(
            fontFamily = manrope,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.sp,
        ),
        headlineSmall = TextStyle(
            fontFamily = manrope,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.sp,
        ),
        titleLarge = TextStyle(
            fontFamily = manrope,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.sp,
        ),
        titleMedium = TextStyle(
            fontFamily = manrope,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.sp,
        ),
        titleSmall = TextStyle(
            fontFamily = manrope,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.sp,
        ),
        bodyLarge = TextStyle(
            fontFamily = manrope,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.sp,
            lineHeight = (16 * 1.5).sp,
        ),
        bodyMedium = TextStyle(
            fontFamily = manrope,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.sp,
            lineHeight = (14 * 1.5).sp,
        ),
        bodySmall = TextStyle(
            fontFamily = manrope,
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.sp,
        ),
        labelLarge = TextStyle(
            fontFamily = manrope,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.sp,
        ),
        labelMedium = TextStyle(
            fontFamily = manrope,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.sp,
        ),
        labelSmall = TextStyle(
            fontFamily = manrope,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.sp,
        ),
    )
}

@Composable
private fun BrandTypographyPreviewContent(darkTheme: Boolean) = CoffeePeekTheme(darkTheme = darkTheme) {
    Surface {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(FontWeight.Light, FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold,
                FontWeight.Bold, FontWeight.ExtraBold).forEach { weight ->
                Text("Manrope ${weight.weight}: Кофе Coffee", fontWeight = weight,
                    style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Preview @Composable private fun BrandTypographyLightPreview() = BrandTypographyPreviewContent(false)
@Preview @Composable private fun BrandTypographyDarkPreview() = BrandTypographyPreviewContent(true)
