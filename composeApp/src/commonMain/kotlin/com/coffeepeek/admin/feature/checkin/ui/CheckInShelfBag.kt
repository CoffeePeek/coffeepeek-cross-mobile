package com.coffeepeek.admin.feature.checkin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.check_in_shelf_bag_open_coffee
import coffeepeek.composeapp.generated.resources.check_in_shelf_bag_unnamed
import com.coffeepeek.admin.utils.CpImage
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * The bag a home check-in was brewed from, in place of the bean title: its card, name and roaster. [onCoffee] opens the
 * coffee in the catalogue and is null when the bag is not a published catalogue coffee.
 */
@Composable
internal fun CheckInShelfBag(
    cardUrl: String,
    name: String?,
    roaster: String?,
    onCoffee: (() -> Unit)?,
    image: @Composable (url: String, Modifier) -> Unit = { url, modifier -> CpImage(url, contentScale = ContentScale.Fit, modifier = modifier) },
) {
    val openLabel = stringResource(Res.string.check_in_shelf_bag_open_coffee)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(CoffeePeekDimens.radiusMd))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .then(onCoffee?.let { Modifier.clickable(role = Role.Button, onClickLabel = openLabel, onClick = it) } ?: Modifier)
            .semantics(mergeDescendants = true) {}
            .padding(CoffeePeekDimens.spacing2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3),
    ) {
        // The slot only bounds the card: the card keeps its own proportions, as on the shelf.
        Box(Modifier.size(width = 40.dp, height = 52.dp), contentAlignment = Alignment.Center) {
            image(cardUrl, Modifier.clip(RoundedCornerShape(4.dp)))
        }
        Column(Modifier.weight(1f)) {
            Text(name?.takeIf(String::isNotBlank) ?: stringResource(Res.string.check_in_shelf_bag_unnamed),
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            roaster?.takeIf(String::isNotBlank)?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (onCoffee != null) Icon(CoffeePeekIcons.ChevronRight, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CheckInShelfBagPreviewContent() {
    CheckInShelfBag("card", "Эфиопия Иргачеффе", "Sibaristica", onCoffee = {}) { _, modifier ->
        Box(modifier.size(38.dp, 52.dp).background(MaterialTheme.colorScheme.tertiary))
    }
}

@Preview @Composable private fun CheckInShelfBagLightPreview() = CoffeePeekTheme(darkTheme = false) { CheckInShelfBagPreviewContent() }
@Preview @Composable private fun CheckInShelfBagDarkPreview() = CoffeePeekTheme(darkTheme = true) { CheckInShelfBagPreviewContent() }
