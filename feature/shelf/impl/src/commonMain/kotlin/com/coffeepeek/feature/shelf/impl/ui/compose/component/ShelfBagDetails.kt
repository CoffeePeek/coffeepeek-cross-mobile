package com.coffeepeek.feature.shelf.impl.ui.compose.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.coffeepeek.core.designsystem.component.CoffeePeekTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekRating
import com.coffeepeek.core.designsystem.component.CoffeePeekRatingSize
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shelf.api.ShelfImage
import com.coffeepeek.feature.shelf.impl.resources.*
import com.coffeepeek.feature.shelf.impl.ui.compose.PreviewShelfImage
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfBagBrews
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfBagItem
import com.coffeepeek.feature.shelf.impl.ui.compose.previewArchiveItems
import com.coffeepeek.feature.shelf.impl.ui.compose.previewBrews
import com.coffeepeek.feature.shelf.impl.ui.compose.previewShelfItems
import com.coffeepeek.feature.shelf.impl.ui.compose.previewToday
import com.coffeepeek.feature.shelf.impl.ui.data.ShelfDay
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * The body of a bag's screen, as in the shelf spec: the card large, its name and roaster, quiet trait pills, one muted line of
 * dates and what is left, a link to the catalogue coffee ([onOpenCoffee] null without one) and the home check-ins.
 */
@Composable
internal fun ShelfBagDetails(
    item: ShelfBagItem, brews: ShelfBagBrews?, today: ShelfDay, image: ShelfImage, archived: Boolean,
    onOpenCoffee: (() -> Unit)?, onRetryBrews: () -> Unit, modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = CoffeePeekDimens.spacing4)
        .padding(bottom = CoffeePeekDimens.spacing6),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
        ShelfBagTile(item, image, Modifier.fillMaxWidth().height(320.dp).padding(horizontal = CoffeePeekDimens.spacing8,
            vertical = CoffeePeekDimens.spacing3), Alignment.Center, elevation = 14.dp)
        ShelfBagHeading(item)
        item.factLine(today, archived, brews)?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
        onOpenCoffee?.let { open ->
            TextButton(open, Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(Res.string.shelf_open_catalog), style = MaterialTheme.typography.labelLarge)
                Icon(CoffeePeekIcons.ChevronRight, null, Modifier.padding(start = CoffeePeekDimens.spacing1).size(18.dp))
            }
        }
        if (brews != null) {
            ShelfBagBrewList(brews, today, onRetryBrews, Modifier.padding(top = CoffeePeekDimens.spacing2))
        }
    }
}

/** Name, roaster, quiet trait pills and, for a finished bag, how it was rated. */
@Composable
private fun ShelfBagHeading(item: ShelfBagItem) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(item.title(), Modifier.semantics { heading() }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center)
            item.bag.roasterName?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = muted, textAlign = TextAlign.Center) }
        }
        item.traits().takeIf { it.isNotEmpty() }?.let { ShelfFactChips(it) }
        item.bag.rating?.let { CoffeePeekRating(it.toDouble(), size = CoffeePeekRatingSize.Medium,
                inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant) }
        item.bag.buyAgain?.let {
            Text(stringResource(if (it) Res.string.shelf_buy_again else Res.string.shelf_not_again), style = MaterialTheme.typography.bodyMedium,
                color = muted)
        }
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

/**
 * "На полке 9 дней · Обжарка 3 недели назад · осталось ≈ 160 г"; for a finished bag, how long it stood, when it ran out and
 * its roast. Null when nothing is known.
 */
@Composable
private fun ShelfBagItem.factLine(today: ShelfDay, archived: Boolean, brews: ShelfBagBrews?): String? = listOfNotNull(
    shelfAgeText(today),
    finishedOn?.takeIf { archived }?.let { stringResource(Res.string.shelf_finished_on, it.text(today)) },
    roastText(today),
    (brews as? ShelfBagBrews.Loaded)?.leftGrams?.takeUnless { archived }?.let { stringResource(Res.string.shelf_bag_left, shelfWeightText(it)) },
).takeIf { it.isNotEmpty() }?.joinToString(" · ")

@Composable
private fun ShelfBagDetailsPreview(dark: Boolean, item: ShelfBagItem, archived: Boolean, brews: ShelfBagBrews?) =
    CoffeePeekTheme(darkTheme = dark) {
        ShelfBagDetails(item, brews, previewToday, PreviewShelfImage, archived, onOpenCoffee = {}, onRetryBrews = {},
            Modifier.background(MaterialTheme.colorScheme.background))
    }

@Preview @Composable private fun ShelfBagDetailsLightPreview() = ShelfBagDetailsPreview(false, previewShelfItems()[1], false, previewBrews())
@Preview @Composable private fun ShelfBagDetailsDarkPreview() = ShelfBagDetailsPreview(true, previewShelfItems()[1], false, previewBrews())
@Preview @Composable private fun ShelfArchivedBagLightPreview() =
    ShelfBagDetailsPreview(false, previewArchiveItems()[0], true, ShelfBagBrews.Loaded(emptyList(), null))
@Preview @Composable private fun ShelfArchivedBagDarkPreview() =
    ShelfBagDetailsPreview(true, previewArchiveItems()[0], true, ShelfBagBrews.Loaded(emptyList(), null))
