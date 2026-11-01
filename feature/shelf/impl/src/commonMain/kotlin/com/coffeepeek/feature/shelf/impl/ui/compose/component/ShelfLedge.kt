package com.coffeepeek.feature.shelf.impl.ui.compose.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekFrostedSurface
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.LocalCoffeePeekOpaqueGlassOverride
import dev.chrisbanes.haze.HazeState
import org.jetbrains.compose.ui.tooling.preview.Preview

internal val ShelfLedgeHeight = 40.dp
internal val ShelfLedgeOverlap = 8.dp
private val ShelfLedgeCornerRadius = 14.dp
private val ShelfLedgeShape = RoundedCornerShape(ShelfLedgeCornerRadius)

/** A frosted front rail: the lower part of each bag remains visible behind its glass. */
@Composable
internal fun ShelfLedge(modifier: Modifier = Modifier, hazeState: HazeState? = null) {
    val dark = MaterialTheme.colorScheme.background.luminance() < .5f
    val tint = Color(0xFFA6AEB3).copy(alpha = if (dark) .32f else .24f)
    val shadow = Color.Black.copy(alpha = if (dark) .4f else .2f)
    Box(modifier.fillMaxWidth().height(ShelfLedgeHeight)
        .shadow(8.dp, ShelfLedgeShape, clip = false, ambientColor = shadow, spotColor = shadow)) {
        CoffeePeekFrostedSurface(Modifier.matchParentSize(), ShelfLedgeShape, hazeState, tint, blurRadius = 3.dp) {
            Canvas(Modifier.matchParentSize()) { drawShelfRailBevel(dark) }
        }
        Canvas(Modifier.matchParentSize()) {
            val inset = 12.dp.toPx()
            listOf(inset, size.width - inset).forEach { drawShelfFastener(Offset(it, size.height / 2)) }
        }
    }
}

private fun DrawScope.drawShelfRailBevel(dark: Boolean) {
    val radius = CornerRadius(ShelfLedgeCornerRadius.toPx())
    drawRoundRect(Brush.verticalGradient(listOf(
        Color.White.copy(alpha = if (dark) .18f else .4f), Color.White.copy(alpha = .03f),
        Color.Black.copy(alpha = if (dark) .16f else .08f),
    )), cornerRadius = radius)
    val stroke = 1.dp.toPx()
    drawRoundRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = .7f), Color.White.copy(alpha = .12f),
        Color.Black.copy(alpha = .22f))), topLeft = Offset(stroke / 2, stroke / 2),
        size = Size(size.width - stroke, size.height - stroke), cornerRadius = radius, style = Stroke(stroke))
    val inset = (ShelfLedgeCornerRadius + 2.dp).toPx()
    drawLine(Color.White.copy(alpha = .4f), Offset(inset, 1.5.dp.toPx()),
        Offset(size.width - inset, 1.5.dp.toPx()), strokeWidth = .5.dp.toPx())
}

private fun DrawScope.drawShelfFastener(center: Offset) {
    val radius = 5.dp.toPx()
    drawCircle(Color.Black.copy(alpha = .3f), radius + .5.dp.toPx(), center + Offset(0f, .75.dp.toPx()))
    drawCircle(Color(0xFF69747D), radius, center)
    drawCircle(Brush.linearGradient(listOf(Color(0xFFF3F6F8), Color(0xFFC5CDD3), Color(0xFF838F99)),
        start = center - Offset(0f, radius), end = center + Offset(0f, radius)), radius - .5.dp.toPx(), center)
    drawCircle(Color.White.copy(alpha = .7f), radius - .25.dp.toPx(), center, style = Stroke(.5.dp.toPx()))
    val notch = Offset(3.dp.toPx(), 0f)
    drawLine(Color(0xFF4C5861), center - notch, center + notch, strokeWidth = 1.2.dp.toPx(), cap = StrokeCap.Round)
    val lip = Offset(0f, .75.dp.toPx())
    drawLine(Color.White.copy(alpha = .55f), center - notch + lip, center + notch + lip,
        strokeWidth = .4.dp.toPx(), cap = StrokeCap.Round)
}

/** Two bare rails for the empty and guest states. */
@Composable
internal fun ShelfEmptyShelves(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(top = 96.dp), verticalArrangement = Arrangement.spacedBy(112.dp)) {
        ShelfLedge()
        ShelfLedge()
    }
}

@Composable
private fun ShelfLedgePreview(dark: Boolean, opaque: Boolean = false) {
    CompositionLocalProvider(LocalCoffeePeekOpaqueGlassOverride provides opaque) {
        CoffeePeekTheme(darkTheme = dark) {
            Column(Modifier.background(MaterialTheme.colorScheme.background).padding(CoffeePeekDimens.spacing4)) { ShelfEmptyShelves() }
        }
    }
}

@Preview @Composable private fun ShelfLedgeLightPreview() = ShelfLedgePreview(false)
@Preview @Composable private fun ShelfLedgeDarkPreview() = ShelfLedgePreview(true)
@Preview @Composable private fun ShelfLedgeOpaqueLightPreview() = ShelfLedgePreview(false, true)
@Preview @Composable private fun ShelfLedgeOpaqueDarkPreview() = ShelfLedgePreview(true, true)
