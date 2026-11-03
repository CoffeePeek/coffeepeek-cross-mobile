package com.coffeepeek.feature.collections.impl.ui.compose.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.collections.domain.model.SharedCollection
import com.coffeepeek.feature.collections.impl.resources.*
import com.coffeepeek.feature.collections.impl.ui.model.CollectionPosterFormat
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

@Composable
internal fun CollectionPosterThumbnail(collection: SharedCollection, format: CollectionPosterFormat, page: Int,
    maxHeight: Dp, onOpen: () -> Unit, poster: @Composable (SharedCollection, CollectionPosterFormat, Int) -> Unit) {
    val openLabel = stringResource(Res.string.collections_expand_poster)
    CollectionPosterFrame(format, maxHeight, Modifier.fillMaxWidth()) {
        Box(Modifier.clip(RoundedCornerShape(20.dp)).clickable(onClickLabel = openLabel, role = Role.Button, onClick = onOpen)) {
            poster(collection, format, page)
        }
    }
}

@Composable
private fun CollectionPosterThumbnailSample(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    val collection = SharedCollection("sample", "Мои любимые места", "https://coffeepeek.by", emptyList())
    val resources = collectionPosterResources(collection, CollectionPosterFormat.Story)
    CollectionPosterThumbnail(collection, CollectionPosterFormat.Story, 0, 300.dp, {}, { value, format, page ->
        Image(CollectionPosterPainter(value, format, page, resources), value.title,
            Modifier.fillMaxWidth().aspectRatio(format.width.toFloat() / format.height))
    })
}
@Preview @Composable private fun CollectionPosterThumbnailLightPreview() = CollectionPosterThumbnailSample(false)
@Preview @Composable private fun CollectionPosterThumbnailDarkPreview() = CollectionPosterThumbnailSample(true)
