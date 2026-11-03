package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coffeepeek.core.designsystem.resources.Res
import com.coffeepeek.core.designsystem.resources.coffeepeek_image_placeholder_brand
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Existing application brand fallback; image-specific accessibility text belongs to the caller. */
@Composable
fun CoffeePeekImagePlaceholder(
    modifier: Modifier = Modifier,
    labelSize: TextUnit = 18.sp,
    contentDescription: String? = null,
) {
    val brand = stringResource(Res.string.coffeepeek_image_placeholder_brand)
    val painter = coffeePeekImagePlaceholderPainter(labelSize)
    Box(modifier.fillMaxSize().paint(painter, sizeToIntrinsics = false).semantics {
        text = AnnotatedString(brand)
        contentDescription?.let { this.contentDescription = it }
    })
}

/** Shared rendering for on-screen placeholders and fixed-pixel exported artwork. */
@Composable
fun coffeePeekImagePlaceholderPainter(labelSize: TextUnit = 18.sp,
    darkBackground: Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f,
    density: Density = LocalDensity.current): Painter {
    val brand = stringResource(Res.string.coffeepeek_image_placeholder_brand)
    val measurer = rememberTextMeasurer()
    val style = MaterialTheme.typography.bodyLarge.copy(fontSize = labelSize, fontWeight = FontWeight.Black,
        letterSpacing = 1.4.sp, textAlign = TextAlign.Center)
    return remember(brand, measurer, style, darkBackground, density) {
        CoffeePeekImagePlaceholderPainter(brand, measurer, style, darkBackground, density)
    }
}

private class CoffeePeekImagePlaceholderPainter(private val brand: String, private val measurer: TextMeasurer,
    private val style: TextStyle, private val darkBackground: Boolean, private val textDensity: Density) : Painter() {
    override val intrinsicSize = Size.Unspecified

    override fun DrawScope.onDraw() {
        drawRect(if (darkBackground) Color.White else Color.Black)
        val label = measurer.measure(AnnotatedString(brand), style, maxLines = 1, density = textDensity)
        val fitScale = minOf(1f, size.width * 0.85f / label.size.width, size.height * 0.85f / label.size.height)
        withTransform({
            translate(size.width / 2f, size.height / 2f)
            scale(fitScale, fitScale, Offset.Zero)
        }) {
            drawText(label, if (darkBackground) Color.Black else Color.White,
                Offset(-label.size.width / 2f, -label.size.height / 2f))
        }
    }
}

@Preview @Composable private fun CoffeePeekImagePlaceholderLightPreview() = CoffeePeekTheme(darkTheme = false) {
    CoffeePeekImagePlaceholder(Modifier.height(300.dp), labelSize = 24.sp)
}
@Preview @Composable private fun CoffeePeekImagePlaceholderDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    CoffeePeekImagePlaceholder(Modifier.height(300.dp), labelSize = 24.sp)
}
