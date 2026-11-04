package com.coffeepeek.feature.shelf.impl.ui.compose.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shelf.domain.model.PublicShelfItem
import com.coffeepeek.feature.shelf.impl.resources.*
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

@Composable
internal fun PublicShelfBag(value: PublicShelfItem, onOpen: () -> Unit, modifier: Modifier = Modifier.width(112.dp),
    onShelf: Boolean = false,
    photo: @Composable (Modifier) -> Unit = {
        ShelfBagPlaceholder(value.name.takeIf(String::isNotBlank) ?: stringResource(Res.string.shelf_bag_untitled), it) }) {
    val title = value.name.takeIf(String::isNotBlank) ?: stringResource(Res.string.shelf_bag_untitled)
    val openLabel = stringResource(Res.string.shelf_profile_open_coffee, title)
    val clickable = if (value.coffeeSlug.isNullOrBlank()) Modifier else
        Modifier.clickable(role = Role.Button, onClickLabel = openLabel, onClick = onOpen)
    Column(modifier.then(clickable).padding(bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        photo(Modifier.fillMaxWidth().height(136.dp))
        if (onShelf) Spacer(Modifier.height(36.dp))
        Text(title, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        value.roasterName?.takeIf(String::isNotBlank)?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun PublicShelfBagSample(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    PublicShelfBag(PublicShelfItem("sample", "Бразилия", "Coffee roaster", "brazil", null), {})
}

@Preview @Composable private fun PublicShelfBagLightPreview() = PublicShelfBagSample(false)
@Preview @Composable private fun PublicShelfBagDarkPreview() = PublicShelfBagSample(true)
