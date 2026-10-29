package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens

/** Matches post / check-in header: avatar column and overflow menu touch target. */
val CoffeePeekPostHeaderLeadingWidth = 44.dp
val CoffeePeekPostHeaderOverflowWidth = 48.dp

/** Same width as the profile avatar; height fits a bag card photo in the feed row. */
val CoffeePeekPostShelfBagCardMaxHeight = 54.dp

val CoffeePeekPostShelfBagCardShape = RoundedCornerShape(4.dp)

/** Clip and size bounds for a bag card photo in [CoffeePeekPostShelfBagCardSlot]. */
fun Modifier.coffeePeekPostShelfBagCardImage(): Modifier = height(CoffeePeekPostShelfBagCardMaxHeight)
    .widthIn(max = CoffeePeekPostHeaderLeadingWidth)
    .wrapContentWidth(Alignment.CenterHorizontally)
    .clip(CoffeePeekPostShelfBagCardShape)

/** Avatar-width column; [card] is centered horizontally and vertically in the slot. */
@Composable
fun CoffeePeekPostShelfBagCardSlot(card: @Composable () -> Unit) {
    Box(
        Modifier.size(CoffeePeekPostHeaderLeadingWidth, CoffeePeekPostShelfBagCardMaxHeight),
        contentAlignment = Alignment.Center,
    ) {
        card()
    }
}

/**
 * Shelf bag under a post header: thumbnail in the avatar column, [title] under the author name,
 * optional [details], chevron aligned with the header overflow menu.
 */
@Composable
fun CoffeePeekPostShelfBagRow(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    details: String? = null,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    showChevron: Boolean = false,
    card: @Composable () -> Unit,
) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .then(
                if (onClick != null && onClickLabel != null) {
                    Modifier.clickable(role = Role.Button, onClickLabel = onClickLabel, onClick = onClick)
                } else Modifier,
            )
            .semantics(mergeDescendants = true) {}
            .padding(vertical = CoffeePeekDimens.spacing1),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoffeePeekPostShelfBagCardSlot(card)
        Column(
            Modifier.weight(1f).padding(start = CoffeePeekDimens.spacing3),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            title()
            if (!details.isNullOrBlank()) {
                Text(
                    details,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (showChevron) {
            Box(Modifier.size(CoffeePeekPostHeaderOverflowWidth), contentAlignment = Alignment.Center) {
                Icon(
                    CoffeePeekIcons.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
