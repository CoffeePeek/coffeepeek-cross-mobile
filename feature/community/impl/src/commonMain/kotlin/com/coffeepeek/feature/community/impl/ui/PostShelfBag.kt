package com.coffeepeek.feature.community.impl.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.core.designsystem.component.CoffeePeekPostShelfBagRow
import com.coffeepeek.core.designsystem.component.CoffeePeekPostShelfBagTitle
import com.coffeepeek.core.designsystem.component.coffeePeekPostShelfBagCardImage
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
    val title = name?.takeIf(String::isNotBlank) ?: stringResource(Res.string.community_shelf_bag)
    val separator = stringResource(Res.string.community_details_separator)
    CoffeePeekPostShelfBagRow(
        title = { CoffeePeekPostShelfBagTitle(title, roaster, separator) },
        details = details.takeIf(String::isNotBlank),
        onClick = onCoffee,
        onClickLabel = openLabel.takeIf { onCoffee != null },
        showChevron = onCoffee != null,
        card = {
            host.image(cardUrl, null, Modifier.coffeePeekPostShelfBagCardImage(), ContentScale.Fit, null)
        },
    )
}

private val previewHost = CommunityHost(image = { _, _, modifier, _, _ ->
    Box(modifier.background(MaterialTheme.colorScheme.tertiary))
})

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
