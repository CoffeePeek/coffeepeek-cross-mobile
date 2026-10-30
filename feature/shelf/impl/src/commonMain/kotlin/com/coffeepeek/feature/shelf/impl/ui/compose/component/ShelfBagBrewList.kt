package com.coffeepeek.feature.shelf.impl.ui.compose.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.coffeepeek.core.designsystem.component.CoffeePeekTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekInfoItem
import com.coffeepeek.core.designsystem.component.CoffeePeekLoader
import com.coffeepeek.core.designsystem.component.CoffeePeekRating
import com.coffeepeek.core.designsystem.component.CoffeePeekRatingDisplay
import com.coffeepeek.core.designsystem.component.CoffeePeekRatingSize
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.text.coffeePeekPluralForm
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shelf.impl.resources.*
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfBagBrews
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfBrewItem
import com.coffeepeek.feature.shelf.impl.ui.compose.previewBrews
import com.coffeepeek.feature.shelf.impl.ui.compose.previewToday
import com.coffeepeek.feature.shelf.impl.ui.data.ShelfDay
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.math.roundToInt

/** "Дома с этой пачкой": the bag's home check-ins, newest first, or why there are none to show. */
@Composable
internal fun ShelfBagBrewList(brews: ShelfBagBrews, today: ShelfDay, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(Res.string.shelf_brews_title), Modifier.weight(1f).semantics { heading() },
                style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            (brews as? ShelfBagBrews.Loaded)?.brews?.size?.takeIf { it > 0 }?.let {
                Text(stringResource(coffeePeekPluralForm(it, Res.string.shelf_brews_count_one, Res.string.shelf_brews_count_few,
                    Res.string.shelf_brews_count_many), it), style = MaterialTheme.typography.bodySmall, color = muted)
            }
        }
        when (brews) {
            ShelfBagBrews.Loading -> Row(horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2),
                verticalAlignment = Alignment.CenterVertically) {
                CoffeePeekLoader(stringResource(Res.string.shelf_brews_loading), size = 18.dp)
                Text(stringResource(Res.string.shelf_brews_loading), style = MaterialTheme.typography.bodyMedium, color = muted)
            }
            ShelfBagBrews.Failed -> Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(Res.string.shelf_brews_failed), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = muted)
                TextButton(onRetry, Modifier.heightIn(min = 48.dp)) { Text(stringResource(Res.string.shelf_retry)) }
            }
            is ShelfBagBrews.Loaded -> if (brews.brews.isEmpty()) Text(stringResource(Res.string.shelf_brews_empty),
                style = MaterialTheme.typography.bodyMedium, color = muted) else ShelfBrewRows(brews.brews, today)
        }
    }
}

@Composable
private fun ShelfBrewRows(brews: List<ShelfBrewItem>, today: ShelfDay) {
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background, RoundedCornerShape(CoffeePeekDimens.radiusLg))) {
        brews.forEachIndexed { index, brew ->
            if (index > 0) HorizontalDivider(Modifier.padding(start = 60.dp), color = MaterialTheme.colorScheme.outlineVariant)
            ShelfBrewRow(brew, today)
        }
    }
}

/** "V60 · 15 г" over "Вчера", with the taste score at the end. */
@Composable
private fun ShelfBrewRow(brew: ShelfBrewItem, today: ShelfDay) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = CoffeePeekDimens.spacing3, vertical = CoffeePeekDimens.spacing2)
        .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
        CoffeePeekInfoItem(brew.title(), brew.day?.let { shelfBrewDayText(it, today) }.orEmpty(), Modifier.weight(1f)) {
            Box(Modifier.size(36.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center) {
                Icon(CoffeePeekIcons.Coffee, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
        brew.score?.let { CoffeePeekRating(it.toDouble(), size = CoffeePeekRatingSize.Small, display = CoffeePeekRatingDisplay.Compact) }
    }
}

@Composable
private fun ShelfBrewItem.title(): String {
    val name = method ?: stringResource(Res.string.shelf_brew_untitled)
    return doseGrams?.let { "$name · ${stringResource(Res.string.shelf_brew_dose, shelfDoseText(it))}" } ?: name
}

/** "15" or "15,5": whole grams without a fraction, otherwise one decimal. */
internal fun shelfDoseText(grams: Double): String {
    val tenths = (grams * 10).roundToInt()
    return if (tenths % 10 == 0) "${tenths / 10}" else "${tenths / 10},${tenths % 10}"
}

@Composable
private fun shelfBrewDayText(day: ShelfDay, today: ShelfDay): String = when (today.epochDay - day.epochDay) {
    0L -> stringResource(Res.string.shelf_brew_today)
    1L -> stringResource(Res.string.shelf_brew_yesterday)
    else -> day.text(today)
}

@Composable
private fun ShelfBagBrewListPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Column(Modifier.background(MaterialTheme.colorScheme.surface).padding(CoffeePeekDimens.spacing4),
        verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing6)) {
        ShelfBagBrewList(previewBrews(), previewToday, {})
        ShelfBagBrewList(ShelfBagBrews.Loaded(emptyList(), null), previewToday, {})
        ShelfBagBrewList(ShelfBagBrews.Failed, previewToday, {})
    }
}

@Preview @Composable private fun ShelfBagBrewListLightPreview() = ShelfBagBrewListPreview(false)
@Preview @Composable private fun ShelfBagBrewListDarkPreview() = ShelfBagBrewListPreview(true)
