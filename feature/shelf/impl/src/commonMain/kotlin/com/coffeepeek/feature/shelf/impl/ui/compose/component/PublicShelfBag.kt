package com.coffeepeek.feature.shelf.impl.ui.compose.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shelf.domain.model.PublicShelfItem
import com.coffeepeek.feature.shelf.impl.resources.*
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

@Composable
internal fun PublicShelfBag(value: PublicShelfItem, onOpen: () -> Unit, modifier: Modifier = Modifier.width(112.dp),
    photo: @Composable (Modifier) -> Unit = {
        ShelfPublicPhotoFallback(it) }) {
    val title = value.name.takeIf(String::isNotBlank) ?: stringResource(Res.string.shelf_bag_untitled)
    val openLabel = stringResource(Res.string.shelf_profile_open_coffee, title)
    val description = listOfNotNull(title, value.roasterName?.takeIf(String::isNotBlank)).joinToString(", ")
    val clickable = if (value.coffeeSlug.isNullOrBlank()) Modifier else
        Modifier.clickable(role = Role.Button, onClickLabel = openLabel, onClick = onOpen)
    Box(modifier.height(136.dp).then(clickable).semantics(mergeDescendants = true) { contentDescription = description }) {
        photo(Modifier.fillMaxSize())
    }
}

@Composable
private fun PublicShelfBagSample(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    PublicShelfBag(PublicShelfItem("sample", "Бразилия", "Coffee roaster", "brazil", null), {})
}

@Preview @Composable private fun PublicShelfBagLightPreview() = PublicShelfBagSample(false)
@Preview @Composable private fun PublicShelfBagDarkPreview() = PublicShelfBagSample(true)
