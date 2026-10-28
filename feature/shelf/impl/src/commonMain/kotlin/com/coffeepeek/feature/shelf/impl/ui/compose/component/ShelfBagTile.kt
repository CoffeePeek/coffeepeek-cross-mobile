package com.coffeepeek.feature.shelf.impl.ui.compose.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shelf.api.ShelfImage
import com.coffeepeek.feature.shelf.impl.resources.Res
import com.coffeepeek.feature.shelf.impl.resources.shelf_new_badge
import com.coffeepeek.feature.shelf.impl.ui.compose.PreviewShelfImage
import com.coffeepeek.feature.shelf.impl.ui.compose.previewShelfItems
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfBagItem
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

internal val ShelfCardShape = RoundedCornerShape(4.dp)

/**
 * A bag: its card at the card's own proportions, as large as fits and placed at [alignment], with rounded corners and a
 * shadow; a paper bag with its name when there is no card. A [ghost] is the faded, dashed-outlined copy that marks where a
 * lifted bag will land.
 */
@Composable
internal fun ShelfBagTile(
    item: ShelfBagItem, image: ShelfImage, modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.BottomCenter, elevation: Dp = 4.dp, highlighted: Boolean = false, ghost: Boolean = false,
) {
    Box(modifier, contentAlignment = alignment) {
        // Wraps the card itself, so the outline and the "НОВАЯ" mark sit on the card, not on the empty slot around it.
        Box(if (ghost) Modifier.shelfGhostOutline(MaterialTheme.colorScheme.outline) else Modifier) {
            val photo = item.bag.photo
            val outline = if (highlighted) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, ShelfCardShape) else Modifier
            val faded = if (ghost) Modifier.graphicsLayer { alpha = GHOST_ALPHA } else Modifier
            if (photo != null) image(photo.url, item.title(), alignment,
                faded.shadow(if (ghost) 0.dp else elevation, ShelfCardShape).clip(ShelfCardShape).then(outline))
            else ShelfBagPlaceholder(item.title(), Modifier.fillMaxWidth(0.86f).fillMaxHeight(0.82f).then(faded).then(outline))
            if (highlighted) ShelfNewMark(Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = (-8).dp))
        }
    }
}

private const val GHOST_ALPHA = 0.3f

/** A dashed line a little outside the card. */
private fun Modifier.shelfGhostOutline(color: Color): Modifier = drawWithContent {
    drawContent()
    val inset = 3.dp.toPx()
    drawRoundRect(color, Offset(-inset, -inset), Size(size.width + inset * 2, size.height + inset * 2), CornerRadius(7.dp.toPx()),
        Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx()))))
}

@Composable
private fun ShelfNewMark(modifier: Modifier) {
    Text(stringResource(Res.string.shelf_new_badge), modifier.clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.primary)
        .padding(horizontal = 5.dp, vertical = 1.dp), color = MaterialTheme.colorScheme.onPrimary, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
}

@Composable
internal fun ShelfBagPlaceholder(title: String, modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
        color = MaterialTheme.colorScheme.secondaryContainer, shadowElevation = 2.dp) {
        Box(Modifier.padding(CoffeePeekDimens.spacing2), contentAlignment = Alignment.Center) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer,
                textAlign = TextAlign.Center, maxLines = 4, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ShelfBagTilePreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Row(Modifier.background(MaterialTheme.colorScheme.background).padding(CoffeePeekDimens.spacing4)) {
        ShelfBagTile(previewShelfItems()[0], PreviewShelfImage, Modifier.size(100.dp, 140.dp), highlighted = true)
        ShelfBagTile(previewShelfItems()[3], PreviewShelfImage, Modifier.size(100.dp, 140.dp))
        ShelfBagTile(previewShelfItems()[1], PreviewShelfImage, Modifier.size(100.dp, 140.dp), ghost = true)
    }
}

@Preview @Composable private fun ShelfBagTileLightPreview() = ShelfBagTilePreview(false)
@Preview @Composable private fun ShelfBagTileDarkPreview() = ShelfBagTilePreview(true)
