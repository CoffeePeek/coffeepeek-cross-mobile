package com.coffeepeek.feature.shelf.impl.ui.compose.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekRating
import com.coffeepeek.core.designsystem.component.CoffeePeekRatingSize
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shelf.api.ShelfImage
import com.coffeepeek.feature.shelf.impl.resources.Res
import com.coffeepeek.feature.shelf.impl.resources.shelf_bag_open
import com.coffeepeek.feature.shelf.impl.ui.compose.PreviewShelfImage
import com.coffeepeek.feature.shelf.impl.ui.compose.previewArchiveItems
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfBagItem
import com.coffeepeek.feature.shelf.impl.ui.compose.previewToday
import com.coffeepeek.feature.shelf.impl.ui.data.ShelfDay
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/** A finished bag: card, name, "roaster · days on the shelf" and the rating it got. */
@Composable
internal fun ShelfArchiveRow(item: ShelfBagItem, today: ShelfDay, image: ShelfImage, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().clip(RoundedCornerShape(CoffeePeekDimens.radiusMd))
        .clickable(onClickLabel = stringResource(Res.string.shelf_bag_open), onClick = onClick)
        .padding(vertical = CoffeePeekDimens.spacing2),
        horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3), verticalAlignment = Alignment.CenterVertically) {
        ShelfBagTile(item, image, Modifier.size(56.dp, 72.dp), Alignment.Center)
        Column(Modifier.weight(1f)) {
            Text(item.title(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            val details = listOfNotNull(item.bag.roasterName, item.daysOnShelf(today)?.let { shelfDaysText(it) })
            if (details.isNotEmpty()) Text(details.joinToString(" · "), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        item.bag.rating?.let { CoffeePeekRating(it.toDouble(), size = CoffeePeekRatingSize.Small) }
    }
}

@Composable
private fun ShelfArchiveRowPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Column(Modifier.background(MaterialTheme.colorScheme.background).padding(CoffeePeekDimens.spacing4)) {
        previewArchiveItems().forEach { ShelfArchiveRow(it, previewToday, PreviewShelfImage, {}) }
    }
}

@Preview @Composable private fun ShelfArchiveRowLightPreview() = ShelfArchiveRowPreview(false)
@Preview @Composable private fun ShelfArchiveRowDarkPreview() = ShelfArchiveRowPreview(true)
