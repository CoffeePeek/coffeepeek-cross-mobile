package com.coffeepeek.feature.collections.impl.ui.compose.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.coffeepeek.core.designsystem.component.CoffeePeekPhotoViewer
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.collections.domain.model.SharedCollection
import com.coffeepeek.feature.collections.impl.ui.model.CollectionPosterFormat
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun CollectionPosterViewer(collection: SharedCollection, format: CollectionPosterFormat, page: Int,
    onDismiss: () -> Unit, poster: @Composable (SharedCollection, CollectionPosterFormat, Int) -> Unit) {
    val count = collectionPosterResources(collection, format).layout.pages.size
    CoffeePeekPhotoViewer(count, page, onDismiss) { index, modifier ->
        CollectionPosterFrame(format, Dp.Infinity, modifier) { poster(collection, format, index) }
    }
}

@Composable
private fun CollectionPosterViewerSample(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    val collection = SharedCollection("sample", "Мои любимые места", "https://coffeepeek.by", listOf(
        com.coffeepeek.feature.collections.domain.model.CollectionPlace(
            com.coffeepeek.feature.collections.domain.model.CollectionPlaceKind.CoffeeShop, "sample", "Кофейня")))
    val resources = collectionPosterResources(collection, CollectionPosterFormat.Story)
    CollectionPosterViewer(collection, CollectionPosterFormat.Story, 0, {}) { value, format, page ->
        Image(CollectionPosterPainter(value, format, page, resources), value.title,
            Modifier.fillMaxWidth().aspectRatio(format.width.toFloat() / format.height))
    }
}
@Preview @Composable private fun CollectionPosterViewerLightPreview() = CollectionPosterViewerSample(false)
@Preview @Composable private fun CollectionPosterViewerDarkPreview() = CollectionPosterViewerSample(true)
