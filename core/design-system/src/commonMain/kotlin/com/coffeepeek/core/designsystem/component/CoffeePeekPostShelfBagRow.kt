package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.background
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme

/** Matches post / check-in header: avatar column and overflow menu touch target. */
val CoffeePeekPostHeaderLeadingWidth = 44.dp
val CoffeePeekPostHeaderOverflowWidth = 48.dp

/** Same width as the profile avatar; height fits a bag card photo in the feed row. */
val CoffeePeekPostShelfBagCardMaxHeight = 54.dp

val CoffeePeekPostShelfBagCardShape = RoundedCornerShape(4.dp)

@Composable
fun CoffeePeekPostShelfBagTitle(name: String, roaster: String?, separator: String) {
    val quiet = SpanStyle(fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    val title = buildAnnotatedString {
        append(name)
        roaster?.takeIf(String::isNotBlank)?.let {
            withStyle(quiet) {
                append(separator)
                append(it)
            }
        }
    }
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

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
                CoffeePeekIcon(
                    CoffeePeekIcons.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PostShelfBagRowPreview(darkTheme: Boolean) = CoffeePeekTheme(darkTheme = darkTheme) {
    CoffeePeekPostShelfBagRow(
        title = { CoffeePeekPostShelfBagTitle("Ethiopia", "Sample Roaster", " · ") },
        modifier = Modifier.widthIn(max = 320.dp).background(MaterialTheme.colorScheme.surface),
        details = "V60 · 15 g · 150 s",
        showChevron = true,
    ) {
        Box(Modifier.size(32.dp, 48.dp).clip(CoffeePeekPostShelfBagCardShape)
            .background(MaterialTheme.colorScheme.tertiaryContainer))
    }
}

@Preview
@Composable
private fun PostShelfBagRowLightPreview() = PostShelfBagRowPreview(darkTheme = false)

@Preview
@Composable
private fun PostShelfBagRowDarkPreview() = PostShelfBagRowPreview(darkTheme = true)
