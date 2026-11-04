package com.coffeepeek.feature.collections.impl.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekImagePlaceholder
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.collections.domain.model.*
import com.coffeepeek.feature.collections.impl.ui.compose.component.SavedCollectionCard
import com.coffeepeek.feature.collections.impl.resources.*
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

@Composable
internal fun SavedCollectionFeatured(value: SavedCollectionSummary?, open: (String) -> Unit, modifier: Modifier = Modifier,
    image: @Composable (Modifier) -> Unit = { CoffeePeekImagePlaceholder(it) }) {
    if (value == null || value.visibility != SavedCollectionVisibility.Public) return
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(Res.string.collections_featured_title), style = MaterialTheme.typography.titleLarge)
        SavedCollectionCard(value, { open(value.id) }, Modifier.widthIn(max = 220.dp), image)
    }
}

@Composable private fun SavedFeaturedSample(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    SavedCollectionFeatured(SavedCollectionSummary("sample", "Кофейный маршрут", visibility = SavedCollectionVisibility.Public, itemCount = 5), {})
}
@Preview @Composable private fun SavedFeaturedLightPreview() = SavedFeaturedSample(false)
@Preview @Composable private fun SavedFeaturedDarkPreview() = SavedFeaturedSample(true)
