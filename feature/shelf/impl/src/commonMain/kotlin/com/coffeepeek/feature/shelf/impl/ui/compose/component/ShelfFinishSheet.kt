package com.coffeepeek.feature.shelf.impl.ui.compose.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekButton
import com.coffeepeek.core.designsystem.component.CoffeePeekRating
import com.coffeepeek.core.designsystem.component.CoffeePeekRatingSize
import com.coffeepeek.core.designsystem.component.CoffeePeekSwipeDismissModalBottomSheet
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shelf.impl.resources.*
import com.coffeepeek.feature.shelf.api.ShelfImage
import com.coffeepeek.feature.shelf.impl.ui.compose.PreviewShelfImage
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfBagItem
import com.coffeepeek.feature.shelf.impl.ui.compose.previewShelfItems
import com.coffeepeek.feature.shelf.impl.ui.compose.previewToday
import com.coffeepeek.feature.shelf.impl.ui.data.ShelfDay
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/** The short summary when a bag is finished: an optional rating and "buy again"; skipping still archives the bag. */
@Composable
internal fun ShelfFinishSheet(item: ShelfBagItem, today: ShelfDay, image: ShelfImage, onFinish: (rating: Int?, buyAgain: Boolean?) -> Unit, onDismiss: () -> Unit) {
    var rating by rememberSaveable(item.id) { mutableIntStateOf(5) }
    var buyAgain by rememberSaveable(item.id) { mutableStateOf<Boolean?>(null) }
    CoffeePeekSwipeDismissModalBottomSheet(onDismiss, stringResource(Res.string.shelf_sheet_close)) {
        ShelfFinishContent(item, today, image, rating, buyAgain, { rating = it }, { buyAgain = it },
            onConfirm = { onFinish(rating.takeIf { it > 0 }, buyAgain) }, onSkip = { onFinish(null, null) })
    }
}

@Composable
internal fun ShelfFinishContent(
    item: ShelfBagItem, today: ShelfDay, image: ShelfImage, rating: Int, buyAgain: Boolean?,
    onRating: (Int) -> Unit, onBuyAgain: (Boolean?) -> Unit, onConfirm: () -> Unit, onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(CoffeePeekDimens.spacing4),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
        ShelfFinishHeader(item, today, image)
        Text(stringResource(Res.string.shelf_finish_question), style = MaterialTheme.typography.titleMedium)
        CoffeePeekRating(rating.toDouble(), size = CoffeePeekRatingSize.Large, onRatingChange = onRating,
            contentDescription = stringResource(Res.string.shelf_finish_rating),
            // The default outline colour all but disappears on the sheet; empty stars must still read as five choices.
            inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant)
        ShelfBuyAgainChips(buyAgain, onBuyAgain)
        CoffeePeekButton(stringResource(Res.string.shelf_finish_confirm), onConfirm)
        TextButton(onSkip, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(stringResource(Res.string.shelf_finish_skip)) }
    }
}

/** The bag's card beside what is being finished and how long it stood on the shelf. */
@Composable
private fun ShelfFinishHeader(item: ShelfBagItem, today: ShelfDay, image: ShelfImage) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3), verticalAlignment = Alignment.CenterVertically) {
        ShelfBagTile(item, image, Modifier.size(64.dp, 88.dp), Alignment.Center, elevation = 6.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing1)) {
            Text(stringResource(Res.string.shelf_finish_title), Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            val muted = MaterialTheme.colorScheme.onSurfaceVariant
            Text(listOfNotNull(item.bag.roasterName, item.title()).joinToString(" · "), style = MaterialTheme.typography.bodyMedium, color = muted)
            item.shelfAgeText(today)?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = muted) }
        }
    }
}

/** Side by side and equally wide; either choice can be taken back with a second tap. */
@Composable
private fun ShelfBuyAgainChips(buyAgain: Boolean?, onBuyAgain: (Boolean?) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
        ShelfVerdictChip(stringResource(Res.string.shelf_buy_again), CoffeePeekIcons.Favorite, colors.primary, colors.onPrimary,
            buyAgain == true, { onBuyAgain(true.takeUnless { buyAgain == true }) }, Modifier.weight(1f))
        ShelfVerdictChip(stringResource(Res.string.shelf_not_again), CoffeePeekIcons.Close, colors.error, colors.onError,
            buyAgain == false, { onBuyAgain(false.takeUnless { buyAgain == false }) }, Modifier.weight(1f))
    }
}

/** A large pill tinted with its [color]; chosen, it fills with it. */
@Composable
private fun ShelfVerdictChip(
    text: String, icon: ImageVector, color: Color, onColor: Color, selected: Boolean, onClick: () -> Unit, modifier: Modifier,
) {
    val surface = MaterialTheme.colorScheme.surface
    val fill by animateColorAsState(if (selected) color else color.copy(alpha = 0.14f).compositeOver(surface), label = "verdict fill")
    val content by animateColorAsState(if (selected) onColor else MaterialTheme.colorScheme.onSurface, label = "verdict text")
    Row(modifier.heightIn(min = 52.dp).clip(CircleShape).background(fill).border(1.5.dp, color.copy(alpha = if (selected) 1f else 0.45f), CircleShape)
        .toggleable(selected, role = Role.Checkbox) { onClick() }.padding(horizontal = CoffeePeekDimens.spacing3),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2, Alignment.CenterHorizontally)) {
        Icon(icon, null, Modifier.size(20.dp), tint = if (selected) onColor else color)
        Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = content, maxLines = 1,
            overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ShelfFinishPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    ShelfFinishContent(previewShelfItems()[1], previewToday, PreviewShelfImage, 4, true, {}, {}, {}, {},
        Modifier.background(MaterialTheme.colorScheme.surface))
}

@Preview @Composable private fun ShelfFinishLightPreview() = ShelfFinishPreview(false)
@Preview @Composable private fun ShelfFinishDarkPreview() = ShelfFinishPreview(true)
