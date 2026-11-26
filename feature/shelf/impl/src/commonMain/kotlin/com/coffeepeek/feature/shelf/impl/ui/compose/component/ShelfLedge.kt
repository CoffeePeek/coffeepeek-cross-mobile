package com.coffeepeek.feature.shelf.impl.ui.compose.component

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.modifier.LocalCoffeePeekGlassHazeState
import com.coffeepeek.core.designsystem.modifier.coffeePeekLiquidGlass
import dev.chrisbanes.haze.HazeState
import org.jetbrains.compose.ui.tooling.preview.Preview

internal val ShelfLedgeHeight = 18.dp
internal val ShelfLedgeOverlap = 8.dp
private val ShelfLedgeShape = RoundedCornerShape(ShelfLedgeHeight / 2)

/** A glass rail in front of the bags, retaining the shelf's existing drag geometry. */
@Composable
internal fun ShelfLedge(modifier: Modifier = Modifier, hazeState: HazeState? = LocalCoffeePeekGlassHazeState.current) {
    val fastener = MaterialTheme.colorScheme.onSurfaceVariant
    Box(modifier.fillMaxWidth().height(ShelfLedgeHeight)
        .coffeePeekLiquidGlass(ShelfLedgeShape, hazeState)
        .shelfGlassFasteners(fastener).clearAndSetSemantics { })
}

private fun Modifier.shelfGlassFasteners(color: Color): Modifier = drawWithCache {
    val radius = 3.dp.toPx()
    val inset = 12.dp.toPx()
    onDrawWithContent {
        drawContent()
        drawShelfFastener(Offset(inset, size.height / 2), radius, color)
        drawShelfFastener(Offset(size.width - inset, size.height / 2), radius, color)
    }
}

private fun DrawScope.drawShelfFastener(center: Offset, radius: Float, color: Color) {
    drawCircle(Color.Black.copy(alpha = .12f), radius + .5.dp.toPx(), center + Offset(0f, 1.dp.toPx()))
    drawCircle(Brush.linearGradient(listOf(Color.White.copy(alpha = .9f), color.copy(alpha = .7f)),
        center - Offset(radius, radius), center + Offset(radius, radius)), radius, center)
    val slot = Offset(radius * .5f, -radius * .5f)
    drawLine(color.copy(alpha = .8f), center - slot, center + slot, .75.dp.toPx())
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
private fun ShelfLedgePreview(dark: Boolean) {
    CoffeePeekTheme(darkTheme = dark) {
        Column(Modifier.background(MaterialTheme.colorScheme.background).padding(CoffeePeekDimens.spacing4)) {
            ShelfEmptyShelves()
        }
    }
}

@Preview @Composable private fun ShelfLedgeLightPreview() = ShelfLedgePreview(false)
@Preview @Composable private fun ShelfLedgeDarkPreview() = ShelfLedgePreview(true)
