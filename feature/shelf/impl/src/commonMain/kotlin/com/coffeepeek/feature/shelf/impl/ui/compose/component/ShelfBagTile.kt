package com.coffeepeek.feature.shelf.impl.ui.compose.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shelf.api.ShelfImage
import com.coffeepeek.feature.shelf.impl.ui.compose.PreviewShelfImage
import com.coffeepeek.feature.shelf.impl.ui.compose.previewShelfItems
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfBagItem
import org.jetbrains.compose.ui.tooling.preview.Preview

/** A bag standing on the shelf: its card, or a paper bag with its name when there is no card. */
@Composable
internal fun ShelfBagTile(item: ShelfBagItem, image: ShelfImage, modifier: Modifier = Modifier, alignment: Alignment = Alignment.BottomCenter) {
    Box(modifier, contentAlignment = alignment) {
        val photo = item.bag.photo
        if (photo != null) image(photo.url, item.title(), alignment, Modifier.fillMaxSize())
        else ShelfBagPlaceholder(item.title(), Modifier.fillMaxWidth(0.86f).fillMaxHeight(0.82f))
    }
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
        ShelfBagTile(previewShelfItems()[0], PreviewShelfImage, Modifier.size(100.dp, 140.dp))
        ShelfBagTile(previewShelfItems()[3], PreviewShelfImage, Modifier.size(100.dp, 140.dp))
    }
}

@Preview @Composable private fun ShelfBagTileLightPreview() = ShelfBagTilePreview(false)
@Preview @Composable private fun ShelfBagTileDarkPreview() = ShelfBagTilePreview(true)
