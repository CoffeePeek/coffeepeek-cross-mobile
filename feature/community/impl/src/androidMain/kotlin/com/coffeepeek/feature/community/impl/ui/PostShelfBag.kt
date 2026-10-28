package com.coffeepeek.feature.community.impl.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.community.api.CommunityHost
import com.coffeepeek.feature.community.impl.R

/**
 * The bag a home post was brewed from, in place of the bean title: its card, name and roaster. [onCoffee] opens the coffee
 * in the catalogue and is null when the bag is not a published catalogue coffee.
 */
@Composable
internal fun PostShelfBag(cardUrl: String, name: String?, roaster: String?, onCoffee: (() -> Unit)?, host: CommunityHost) {
    val openLabel = stringResource(R.string.community_open_coffee)
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
            host.image(cardUrl, null, Modifier.clip(RoundedCornerShape(4.dp)), ContentScale.Fit)
        }
        Column(Modifier.weight(1f)) {
            Text(name?.takeIf(String::isNotBlank) ?: stringResource(R.string.community_shelf_bag), style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            roaster?.takeIf(String::isNotBlank)?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (onCoffee != null) Icon(CoffeePeekIcons.ChevronRight, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private val previewHost = CommunityHost(image = { _, _, modifier, _ -> Box(modifier.size(38.dp, 52.dp).background(MaterialTheme.colorScheme.tertiary)) })

@PreviewLightDark @Composable private fun PostShelfBagPreview() = CoffeePeekTheme {
    PostShelfBag("card", "Эфиопия Иргачеффе", "Sibaristica", onCoffee = {}, previewHost)
}

@PreviewLightDark @Composable private fun PostShelfBagUnnamedPreview() = CoffeePeekTheme {
    PostShelfBag("card", null, null, onCoffee = null, previewHost)
}
