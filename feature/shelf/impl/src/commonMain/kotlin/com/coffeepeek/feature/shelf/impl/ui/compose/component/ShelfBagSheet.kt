package com.coffeepeek.feature.shelf.impl.ui.compose.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekActionDialog
import com.coffeepeek.core.designsystem.component.CoffeePeekButton
import com.coffeepeek.core.designsystem.component.CoffeePeekDialogAction
import com.coffeepeek.core.designsystem.component.CoffeePeekRating
import com.coffeepeek.core.designsystem.component.CoffeePeekRatingSize
import com.coffeepeek.core.designsystem.component.CoffeePeekSwipeDismissModalBottomSheet
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shelf.api.ShelfImage
import com.coffeepeek.feature.shelf.impl.resources.*
import com.coffeepeek.feature.shelf.impl.ui.compose.PreviewShelfImage
import com.coffeepeek.feature.shelf.impl.ui.compose.previewArchiveItems
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfBagItem
import com.coffeepeek.feature.shelf.impl.ui.compose.previewShelfItems
import com.coffeepeek.feature.shelf.impl.ui.compose.previewToday
import com.coffeepeek.feature.shelf.impl.ui.data.ShelfDay
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/** What the bag sheet's main button does: finish a shelf bag, or put an archived one back. */
internal enum class ShelfBagSheetMode { Shelf, Archive }

/** The bag's card and details on a 0.9-height sheet; "⋯" holds the main action and deletion. */
@Composable
internal fun ShelfBagSheet(
    item: ShelfBagItem, today: ShelfDay, image: ShelfImage, mode: ShelfBagSheetMode,
    onPrimary: () -> Unit, onDelete: () -> Unit, onDismiss: () -> Unit,
) {
    var menu by remember(item.id) { mutableStateOf(false) }
    CoffeePeekSwipeDismissModalBottomSheet(onDismiss, stringResource(Res.string.shelf_sheet_close), maxSheetHeightFraction = 0.9f) {
        ShelfBagContent(item, today, image, mode, onPrimary, onMenu = { menu = true })
    }
    if (menu) CoffeePeekActionDialog(item.title(), null, listOf(
        CoffeePeekDialogAction(mode.primaryLabel(), { menu = false; onPrimary() }),
        CoffeePeekDialogAction(stringResource(Res.string.shelf_bag_delete), { menu = false; onDelete() }, destructive = true),
    ), stringResource(Res.string.shelf_cancel)) { menu = false }
}

@Composable
internal fun ShelfBagContent(
    item: ShelfBagItem, today: ShelfDay, image: ShelfImage, mode: ShelfBagSheetMode,
    onPrimary: () -> Unit, onMenu: () -> Unit, modifier: Modifier = Modifier,
) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(CoffeePeekDimens.spacing4),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
        Box(Modifier.fillMaxWidth().height(320.dp)) {
            ShelfBagTile(item, image, Modifier.fillMaxSize().padding(vertical = CoffeePeekDimens.spacing3), Alignment.Center, elevation = 14.dp)
            IconButton(onMenu, Modifier.align(Alignment.TopEnd)) {
                Icon(CoffeePeekIcons.MoreHorizontal, stringResource(Res.string.shelf_bag_more), tint = MaterialTheme.colorScheme.onSurface)
            }
        }
        Text(item.title(), Modifier.semantics { heading() }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center)
        item.bag.roasterName?.let { Text(it, style = MaterialTheme.typography.bodyLarge, color = muted, textAlign = TextAlign.Center) }
        item.traits().takeIf { it.isNotEmpty() }?.let { ShelfFactChips(it) }
        ShelfBagFacts(item, today)
        CoffeePeekButton(mode.primaryLabel(), onPrimary, Modifier.padding(top = CoffeePeekDimens.spacing2))
    }
}

/** Weight, grind, "Из каталога": quiet pills, not controls. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShelfFactChips(facts: List<String>) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing1, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing1)) {
        facts.forEach { fact ->
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                Text(fact, Modifier.padding(horizontal = CoffeePeekDimens.spacing3, vertical = CoffeePeekDimens.spacing1),
                    style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/** Dates in one muted line, then how the bag was rated when it was finished. */
@Composable
private fun ShelfBagFacts(item: ShelfBagItem, today: ShelfDay) {
    val facts = listOfNotNull(
        item.shelfAgeText(today),
        item.roastText(today),
        item.finishedOn?.let { stringResource(Res.string.shelf_finished_on, it.text(today)) },
        item.bag.buyAgain?.let { stringResource(if (it) Res.string.shelf_buy_again else Res.string.shelf_not_again) },
    )
    if (facts.isNotEmpty()) Text(facts.joinToString(" · "), style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    item.bag.rating?.let { CoffeePeekRating(it.toDouble(), size = CoffeePeekRatingSize.Medium) }
}

@Composable
private fun ShelfBagSheetMode.primaryLabel(): String = stringResource(
    when (this) {
        ShelfBagSheetMode.Shelf -> Res.string.shelf_finish_action
        ShelfBagSheetMode.Archive -> Res.string.shelf_bag_restore
    },
)

@Composable
private fun ShelfBagContentPreview(dark: Boolean, item: ShelfBagItem, mode: ShelfBagSheetMode) = CoffeePeekTheme(darkTheme = dark) {
    ShelfBagContent(item, previewToday, PreviewShelfImage, mode, {}, {}, Modifier.background(MaterialTheme.colorScheme.surface))
}

@Preview @Composable private fun ShelfBagContentLightPreview() = ShelfBagContentPreview(false, previewShelfItems()[1], ShelfBagSheetMode.Shelf)
@Preview @Composable private fun ShelfBagContentDarkPreview() = ShelfBagContentPreview(true, previewShelfItems()[1], ShelfBagSheetMode.Shelf)
@Preview @Composable private fun ShelfArchivedBagLightPreview() = ShelfBagContentPreview(false, previewArchiveItems()[0], ShelfBagSheetMode.Archive)
@Preview @Composable private fun ShelfArchivedBagDarkPreview() = ShelfBagContentPreview(true, previewArchiveItems()[0], ShelfBagSheetMode.Archive)
