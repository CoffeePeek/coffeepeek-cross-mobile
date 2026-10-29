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
import org.jetbrains.compose.resources.stringResource
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
import com.coffeepeek.feature.community.api.CommunityHost
import com.coffeepeek.feature.community.impl.resources.*

/**
 * The bag a home post was brewed from, as the post's title: its card beside «name · roaster» and the recipe [details].
 * [onCoffee] opens the coffee in the catalogue and is null when the bag is not a published catalogue coffee.
 */
@Composable
internal fun PostShelfBag(
    cardUrl: String, name: String?, roaster: String?, details: String, onCoffee: (() -> Unit)?, host: CommunityHost,
) {
    val openLabel = stringResource(Res.string.community_open_coffee)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(CoffeePeekDimens.radiusMd))
            .then(onCoffee?.let { Modifier.clickable(role = Role.Button, onClickLabel = openLabel, onClick = it) } ?: Modifier)
            .semantics(mergeDescendants = true) {}
            .padding(vertical = CoffeePeekDimens.spacing1),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3),
    ) {
        // The slot only bounds the card: the card keeps its own proportions, as on the shelf.
        Box(Modifier.size(width = 40.dp, height = 54.dp), contentAlignment = Alignment.Center) {
            host.image(cardUrl, null, Modifier.clip(RoundedCornerShape(4.dp)), ContentScale.Fit)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            PostShelfBagTitle(name?.takeIf(String::isNotBlank) ?: stringResource(Res.string.community_shelf_bag), roaster)
            if (details.isNotBlank()) Text(details, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (onCoffee != null) Icon(CoffeePeekIcons.ChevronRight, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** «Name · roaster» on one line: the roaster reads as a quieter part of the same title. */
@Composable
private fun PostShelfBagTitle(name: String, roaster: String?) {
    val separator = stringResource(Res.string.community_details_separator)
    val quiet = SpanStyle(fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    val title = buildAnnotatedString {
        append(name)
        roaster?.takeIf(String::isNotBlank)?.let { withStyle(quiet) { append(separator); append(it) } }
    }
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
}

private val previewHost = CommunityHost(image = { _, _, modifier, _ -> Box(modifier.size(40.dp, 54.dp).background(MaterialTheme.colorScheme.tertiary)) })

@Composable private fun PostShelfBagPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    PostShelfBag("card", "Эфиопия Иргачеффе", "Sibaristica", "Пуровер · 18 г · 150 с · 4/5", onCoffee = {}, previewHost)
}
@Preview @Composable private fun PostShelfBagLightPreview() = PostShelfBagPreview(false)
@Preview @Composable private fun PostShelfBagDarkPreview() = PostShelfBagPreview(true)

@Composable private fun PostShelfBagUnnamedPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    PostShelfBag("card", null, null, "", onCoffee = null, previewHost)
}
@Preview @Composable private fun PostShelfBagUnnamedLightPreview() = PostShelfBagUnnamedPreview(false)
@Preview @Composable private fun PostShelfBagUnnamedDarkPreview() = PostShelfBagUnnamedPreview(true)
