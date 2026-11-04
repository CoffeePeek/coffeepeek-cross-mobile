package com.coffeepeek.admin.feature.checkin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.check_in_shelf_bag_open_coffee
import coffeepeek.composeapp.generated.resources.check_in_shelf_bag_unnamed
import com.coffeepeek.admin.utils.CpImage
import com.coffeepeek.core.designsystem.component.CoffeePeekPostShelfBagRow
import com.coffeepeek.core.designsystem.component.coffeePeekPostShelfBagCardImage
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * The bag a home check-in was brewed from, as the entry's title: its card beside «name · roaster» and the recipe
 * [details]. [onCoffee] opens the coffee in the catalogue and is null when the bag is not a published catalogue coffee.
 */
@Composable
internal fun CheckInShelfBag(
    cardUrl: String,
    name: String?,
    roaster: String?,
    details: String,
    onCoffee: (() -> Unit)?,
    image: @Composable (url: String, Modifier) -> Unit = { url, modifier -> CpImage(url, contentScale = ContentScale.Fit, modifier = modifier) },
) {
    val openLabel = stringResource(Res.string.check_in_shelf_bag_open_coffee)
    CoffeePeekPostShelfBagRow(
        title = { CheckInShelfBagTitle(name?.takeIf(String::isNotBlank) ?: stringResource(Res.string.check_in_shelf_bag_unnamed), roaster) },
        details = details.takeIf(String::isNotBlank),
        onClick = onCoffee,
        onClickLabel = openLabel.takeIf { onCoffee != null },
        showChevron = onCoffee != null,
        card = { image(cardUrl, Modifier.coffeePeekPostShelfBagCardImage()) },
    )
}

/** «Name · roaster» on one line: the roaster reads as a quieter part of the same title. */
@Composable
private fun CheckInShelfBagTitle(name: String, roaster: String?) {
    val quiet = SpanStyle(fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    val title = buildAnnotatedString {
        append(name)
        roaster?.takeIf(String::isNotBlank)?.let { withStyle(quiet) { append(" · "); append(it) } }
    }
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
}

@Composable
private fun CheckInShelfBagPreviewContent() {
    CheckInShelfBag("card", "Эфиопия Иргачеффе", "Sibaristica", "Пуровер · 18 г · 150 с · 4/5", onCoffee = {}) { _, modifier ->
        Box(modifier.background(MaterialTheme.colorScheme.tertiary))
    }
}

@Preview @Composable private fun CheckInShelfBagLightPreview() = CoffeePeekTheme(darkTheme = false) { CheckInShelfBagPreviewContent() }
@Preview @Composable private fun CheckInShelfBagDarkPreview() = CoffeePeekTheme(darkTheme = true) { CheckInShelfBagPreviewContent() }
