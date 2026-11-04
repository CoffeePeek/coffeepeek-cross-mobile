package com.coffeepeek.feature.collections.impl.ui.compose.component

import androidx.compose.foundation.Image
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekImagePlaceholder
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.collections.api.CollectionPhotoDecoder
import com.coffeepeek.feature.collections.domain.model.*
import com.coffeepeek.feature.collections.domain.repository.SavedCollectionsRepository
import kotlinx.coroutines.CancellationException
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Authenticated bytes stay in this feature; neither URLs nor credentials enter Kamel. */
@Composable
internal fun SavedCollectionCoverImage(summary: SavedCollectionSummary?, input: CollectionCoverInput?,
    repository: SavedCollectionsRepository, decode: CollectionPhotoDecoder, modifier: Modifier) {
    var image by remember(summary?.id, summary?.cover, input) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(summary?.id, summary?.cover, input) {
        val bytes = input?.bytes?.copyOf() ?: summary?.cover?.let { repository.cover(summary.id, it).getOrNull() }
        try { image = bytes?.let { decode(it) } }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { image = null }
        finally { bytes?.fill(0) }
    }
    DisposableEffect(summary?.id, summary?.cover, input) { onDispose { image = null } }
    SavedCoverImageContent(image, modifier)
}

@Composable
private fun SavedCoverImageContent(bitmap: ImageBitmap?, modifier: Modifier) {
    if (bitmap == null) CoffeePeekImagePlaceholder(modifier.clearAndSetSemantics {})
    else Image(bitmap, null, modifier, contentScale = ContentScale.Crop)
}

@Preview @Composable private fun SavedCoverLightPreview() = CoffeePeekTheme(darkTheme = false) {
    SavedCoverImageContent(null, Modifier.size(120.dp))
}
@Preview @Composable private fun SavedCoverDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    SavedCoverImageContent(null, Modifier.size(120.dp))
}
