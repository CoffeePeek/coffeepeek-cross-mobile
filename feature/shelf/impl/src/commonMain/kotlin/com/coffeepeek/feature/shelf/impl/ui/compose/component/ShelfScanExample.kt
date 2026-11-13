package com.coffeepeek.feature.shelf.impl.ui.compose.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shelf.impl.resources.*
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/** A deliberately drawn explanation of the photo becoming a cropped card on a shelf, not a sample scan. */
@Composable
internal fun ShelfScanExample(modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    val ink = MaterialTheme.colorScheme.onSurface
    val background = MaterialTheme.colorScheme.surfaceVariant
    val label = stringResource(Res.string.shelf_scan_example_accessibility)
    Column(modifier.fillMaxWidth().background(background, RoundedCornerShape(CoffeePeekDimens.cardRadius))
        .padding(CoffeePeekDimens.spacing3).semantics { contentDescription = label }) {
        Canvas(Modifier.fillMaxWidth().height(120.dp)) { drawExample(accent, ink) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(Res.string.shelf_scan_example_before), Modifier.weight(1f), textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium)
            Text(stringResource(Res.string.shelf_scan_example_after), Modifier.weight(1f), textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium)
        }
    }
}

private fun DrawScope.drawExample(accent: Color, ink: Color) {
    val unit = size.height / 120f
    val left = size.width * 0.25f
    val right = size.width * 0.75f
    drawRect(ink.copy(alpha = 0.12f), Offset(left - 55 * unit, 5 * unit),
        androidx.compose.ui.geometry.Size(110 * unit, 105 * unit))
    drawBag(left - 38 * unit, 18 * unit, 76 * unit, 86 * unit, ink.copy(alpha = 0.7f), accent, unit)
    drawLine(accent, Offset(left + 47 * unit, 58 * unit), Offset(right - 50 * unit, 58 * unit), 2 * unit)
    drawLine(accent, Offset(right - 57 * unit, 52 * unit), Offset(right - 50 * unit, 58 * unit), 2 * unit)
    drawLine(accent, Offset(right - 57 * unit, 64 * unit), Offset(right - 50 * unit, 58 * unit), 2 * unit)
    drawLine(ink.copy(alpha = 0.7f), Offset(right - 55 * unit, 107 * unit), Offset(right + 55 * unit, 107 * unit), 5 * unit)
    drawBag(right - 32 * unit, 13 * unit, 64 * unit, 94 * unit, ink, accent, unit)
}

private fun DrawScope.drawBag(x: Float, y: Float, width: Float, height: Float, ink: Color, accent: Color, unit: Float) {
    val bag = Path().apply {
        moveTo(x + width * 0.08f, y); lineTo(x + width * 0.92f, y)
        lineTo(x + width, y + height); lineTo(x, y + height); close()
    }
    drawPath(bag, ink.copy(alpha = 0.18f))
    drawPath(bag, ink, style = Stroke(2 * unit))
    drawLine(ink, Offset(x + width * 0.08f, y + height * 0.13f), Offset(x + width * 0.92f, y + height * 0.13f), unit)
    drawCircle(accent, width * 0.16f, Offset(x + width * 0.5f, y + height * 0.48f))
    drawLine(ink, Offset(x + width * 0.22f, y + height * 0.7f), Offset(x + width * 0.78f, y + height * 0.7f), unit)
}

@Composable
private fun ShelfScanExamplePreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) { ShelfScanExample() }

@Preview @Composable private fun ShelfScanExampleLightPreview() = ShelfScanExamplePreview(false)
@Preview @Composable private fun ShelfScanExampleDarkPreview() = ShelfScanExamplePreview(true)
