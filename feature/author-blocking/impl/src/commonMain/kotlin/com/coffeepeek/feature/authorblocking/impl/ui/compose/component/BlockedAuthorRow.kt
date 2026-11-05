package com.coffeepeek.feature.authorblocking.impl.ui.compose.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.*
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.authorblocking.api.AuthorReference
import com.coffeepeek.feature.authorblocking.api.BlockedAuthorItem
import com.coffeepeek.feature.authorblocking.impl.resources.*
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun BlockedAuthorRow(author: BlockedAuthorItem, enabled: Boolean, onUnblock: (AuthorReference) -> Unit) {
    CoffeePeekCard(shape = RoundedCornerShape(CoffeePeekDimens.cardRadius),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), elevation = 0.dp,
        contentPadding = PaddingValues(CoffeePeekDimens.spacing3)) {
        FlowRow(itemVerticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2, Alignment.End),
            verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
            Row(Modifier.weight(1f).widthIn(min = 140.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
                CoffeePeekAvatar(author.name) { modifier -> author.avatarUrl?.let { CoffeePeekImage(it, modifier = modifier) } }
                BlockedAuthorIdentity(author, Modifier.weight(1f))
            }
            CoffeePeekTextAction(stringResource(Res.string.author_blocking_unblock), enabled = enabled,
                onClick = { onUnblock(AuthorReference(author.id, author.slug, author.name, author.avatarUrl)) })
        }
    }
}

@Composable
private fun BlockedAuthorIdentity(author: BlockedAuthorItem, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing1)) {
        BasicText(author.name, style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold), maxLines = 2, overflow = TextOverflow.Ellipsis)
        author.slug?.let { BasicText("@$it", style = MaterialTheme.typography.bodySmall.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant), maxLines = 1, overflow = TextOverflow.Ellipsis) }
    }
}

@Preview @Composable private fun BlockedAuthorRowLightPreview() = CoffeePeekTheme(darkTheme = false) {
    BlockedAuthorRow(BlockedAuthorItem("a", "Анна", "anna", null), true, {})
}
@Preview @Composable private fun BlockedAuthorRowDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    BlockedAuthorRow(BlockedAuthorItem("a", "Анна", "anna", null), false, {})
}
