package com.coffeepeek.feature.shelf.impl.ui.compose.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shelf.impl.resources.Res
import com.coffeepeek.feature.shelf.impl.resources.shelf_oak_texture
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

internal val ShelfLedgeHeight = 18.dp
internal val ShelfLedgeOverlap = 8.dp
private val ShelfLedgeShape = GenericShape { size, _ ->
    val depth = size.height * (ShelfLedgeOverlap / ShelfLedgeHeight)
    val inset = minOf(depth, size.width / 2)
    moveTo(inset, 0f)
    lineTo(size.width - inset, 0f)
    lineTo(size.width, depth)
    lineTo(size.width, size.height)
    lineTo(0f, size.height)
    lineTo(0f, depth)
    close()
}

/** A light oak board with a receding top plane; bag bottoms meet its front edge. */
@Composable
internal fun ShelfLedge(modifier: Modifier = Modifier) {
    val dark = MaterialTheme.colorScheme.background.luminance() < .5f
    val shadow = Color.Black.copy(alpha = if (dark) .45f else .2f)
    val texture = painterResource(Res.drawable.shelf_oak_texture)
    Box(
        modifier.fillMaxWidth().height(ShelfLedgeHeight)
            .shadow(4.dp, ShelfLedgeShape, clip = false, ambientColor = shadow, spotColor = shadow)
            .clip(ShelfLedgeShape),
    ) {
        ShelfWoodPlane(texture, top = true, Modifier.fillMaxWidth().height(ShelfLedgeOverlap))
        ShelfWoodPlane(texture, top = false, Modifier.fillMaxWidth()
            .offset(y = ShelfLedgeOverlap).height(ShelfLedgeHeight - ShelfLedgeOverlap))
    }
}

@Composable
private fun ShelfWoodPlane(texture: Painter, top: Boolean, modifier: Modifier) {
    Image(
        painter = texture,
        contentDescription = null,
        modifier = modifier.shelfWoodLighting(top),
        alignment = if (top) Alignment.TopCenter else Alignment.BottomCenter,
        contentScale = ContentScale.Crop,
    )
}

private fun Modifier.shelfWoodLighting(top: Boolean): Modifier = drawWithCache {
    val light = Brush.verticalGradient(if (top) {
        listOf(Color.White.copy(alpha = .12f), Color.Black.copy(alpha = .05f))
    } else {
        listOf(Color(0xFF624323).copy(alpha = .2f), Color.Black.copy(alpha = .3f))
    })
    val edge = if (top) Color.White.copy(alpha = .16f) else Color(0xFF513A24).copy(alpha = .22f)
    onDrawWithContent {
        drawContent()
        drawRect(light)
        val y = if (top) size.height - .5.dp.toPx() else size.height - 1.dp.toPx()
        drawLine(edge, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
    }
}

/** Two bare beams for the empty and guest states. */
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
