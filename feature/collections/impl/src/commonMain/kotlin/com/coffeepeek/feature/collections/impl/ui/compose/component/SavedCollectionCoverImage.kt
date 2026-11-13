package com.coffeepeek.feature.collections.impl.ui.compose.component

import androidx.compose.foundation.Image
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekImagePlaceholder
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.collections.api.CollectionPhotoDecoder
import com.coffeepeek.feature.collections.domain.model.*
import com.coffeepeek.feature.collections.domain.repository.SavedCollectionsRepository
import kotlinx.coroutines.CancellationException
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Authenticated custom-cover bytes stay in this feature; shop photos use the public image loader. */
@Composable
internal fun SavedCollectionCoverImage(summary: SavedCollectionSummary?, input: CollectionCoverInput?,
    repository: SavedCollectionsRepository, decode: CollectionPhotoDecoder, modifier: Modifier,
    resolvedUrls: MutableMap<String, String> = mutableMapOf()) {
    var image by remember(summary?.id, summary?.revision, summary?.itemCount, summary?.cover, input) { mutableStateOf<ImageBitmap?>(null) }
    var photoUrl by remember(summary?.id, summary?.revision, summary?.itemCount, summary?.cover, input) { mutableStateOf<String?>(null) }
    LaunchedEffect(summary?.id, summary?.revision, summary?.itemCount, summary?.cover, input) {
        image = null
        photoUrl = null
        var bytes: ByteArray? = null
        try {
            if (input != null || summary?.cover != null) {
                bytes = input?.bytes?.copyOf() ?: summary?.let { storedCoverBytes(it, repository) }
                image = bytes?.let { decode(it) }
            } else if (summary != null) photoUrl = defaultShopPhotoUrl(summary, repository)
        }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) {
            image = null
            photoUrl = null
        }
        finally { bytes?.fill(0) }
    }
    DisposableEffect(summary?.id, summary?.revision, summary?.itemCount, summary?.cover, input) {
        onDispose {
            image = null
            photoUrl = null
        }
    }
    SavedCoverImageContent(image, photoUrl, modifier)
}

private suspend fun storedCoverBytes(summary: SavedCollectionSummary, repository: SavedCollectionsRepository): ByteArray? {
    val cover = summary.cover ?: return null
    val result = repository.cover(summary.id, cover)
    result.exceptionOrNull()?.let { if (it is CancellationException) throw it }
    return result.getOrNull()
}

internal suspend fun defaultShopPhotoUrl(summary: SavedCollectionSummary, repository: SavedCollectionsRepository,
    resolvedUrls: MutableMap<String, String> = mutableMapOf()): String? {
    if (summary.itemCount == 0) return null
    // Screen-scoped dedupe for the heavy detail fetch: one request per (id, revision) per screen.
    val cacheKey = "${summary.id}:${summary.revision}"
    resolvedUrls[cacheKey]?.let { return it }
    val detail = if (summary.visibility == SavedCollectionVisibility.Private) repository.owned(summary.id)
        else repository.public(summary.id)
    val collection = detail.getOrElse { failure -> if (failure is CancellationException) throw failure else return null }
    val url = collection.places.firstOrNull { it.kind == CollectionPlaceKind.CoffeeShop }?.photoUrl?.takeIf(String::isNotBlank)
        ?: return null
    resolvedUrls[cacheKey] = url
    return url
}

@Composable
private fun SavedCoverImageContent(bitmap: ImageBitmap?, photoUrl: String?, modifier: Modifier) {
    when {
        bitmap != null -> Image(bitmap, null, modifier, contentScale = ContentScale.Crop)
        photoUrl != null -> KamelImage(resource = { asyncPainterResource(photoUrl) }, contentDescription = null,
            modifier = modifier, contentScale = ContentScale.Crop,
            onLoading = { CoffeePeekImagePlaceholder(Modifier.fillMaxSize()) },
            onFailure = { CoffeePeekImagePlaceholder(Modifier.fillMaxSize()) })
        else -> CoffeePeekImagePlaceholder(modifier.clearAndSetSemantics {})
    }
}

@Preview @Composable private fun SavedCoverLightPreview() = CoffeePeekTheme(darkTheme = false) {
    SavedCoverImageContent(null, null, Modifier.size(120.dp))
}
@Preview @Composable private fun SavedCoverDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    SavedCoverImageContent(null, null, Modifier.size(120.dp))
}
