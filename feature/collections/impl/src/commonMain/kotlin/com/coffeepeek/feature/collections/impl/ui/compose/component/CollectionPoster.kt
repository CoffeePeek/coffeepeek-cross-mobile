package com.coffeepeek.feature.collections.impl.ui.compose.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.*
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekLoader
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.collections.domain.model.*
import com.coffeepeek.feature.collections.domain.repository.CollectionsRepository
import com.coffeepeek.feature.collections.impl.ui.model.CollectionPosterFormat
import com.coffeepeek.feature.collections.impl.resources.*
import io.github.alexzhirkevich.qrose.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import com.coffeepeek.feature.collections.api.CollectionPhotoDecoder
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.sync.withLock
import kotlin.time.TimeSource
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

@Composable
internal fun collectionPosterLabels(collection: SharedCollection?, format: CollectionPosterFormat): CollectionPosterLabels {
    val count = collection?.places?.size ?: 0
    val pages = ((count + format.perPage - 1) / format.perPage).coerceAtLeast(1)
    return CollectionPosterLabels(stringResource(Res.string.collections_shop), stringResource(Res.string.collections_roaster),
        stringResource(Res.string.collections_qr_hint), stringResource(Res.string.collections_count, count),
        (1..pages).map { stringResource(Res.string.collections_page, it, pages) })
}

@Composable
internal fun CollectionPoster(collection: SharedCollection, format: CollectionPosterFormat, page: Int,
    repository: CollectionsRepository, decodePhoto: CollectionPhotoDecoder, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val typography = MaterialTheme.typography.bodyLarge
    val labels = collectionPosterLabels(collection, format)
    key(collection, format, page, labels, typography) {
        val initial = remember { CollectionPosterPainter(collection, format, page, labels, QrCodePainter(collection.url), measurer, typography) }
        val painter by produceState<Painter>(initial) {
            value = preparePoster(collection, format, page, repository, decodePhoto, labels, measurer, typography, 4_000L)
        }
        Image(painter, stringResource(Res.string.collections_poster_description, collection.title),
            modifier.fillMaxWidth().aspectRatio(format.width.toFloat() / format.height))
    }
}

internal suspend fun exportPosters(collection: SharedCollection, format: CollectionPosterFormat, repository: CollectionsRepository, decodePhoto: CollectionPhotoDecoder,
    labels: CollectionPosterLabels, measurer: TextMeasurer, typography: TextStyle): List<ByteArray> {
    val count = (collection.places.size + format.perPage - 1) / format.perPage
    val started = TimeSource.Monotonic.markNow()
    return (0 until count).map { page ->
        currentCoroutineContext().ensureActive()
        preparePoster(collection, format, page, repository, decodePhoto, labels, measurer, typography,
            (12_000L - started.elapsedNow().inWholeMilliseconds).coerceAtLeast(0))
            .toByteArray(format.width, format.height, ImageFormat.PNG)
    }
}

private suspend fun preparePoster(collection: SharedCollection, format: CollectionPosterFormat, page: Int,
    repository: CollectionsRepository, decodePhoto: CollectionPhotoDecoder, labels: CollectionPosterLabels, measurer: TextMeasurer,
    typography: TextStyle, photoBudget: Long): CollectionPosterPainter {
    val places = collection.places.chunked(format.perPage).getOrElse(page) { emptyList() }
    val photos = loadPosterPhotos(places, repository, decodePhoto, photoBudget)
    return CollectionPosterPainter(collection, format, page, labels, QrCodePainter(collection.url), measurer, typography, photos)
}

private suspend fun loadPosterPhotos(places: List<CollectionPlace>, repository: CollectionsRepository,
    decodePhoto: CollectionPhotoDecoder, budget: Long): Map<String, ImageBitmap> {
    val photos = mutableMapOf<String, ImageBitmap>()
    val permits = Semaphore(4)
    val mutex = kotlinx.coroutines.sync.Mutex()
    withTimeoutOrNull(budget) {
        coroutineScope {
            places.map { place -> async {
                permits.withPermit {
                    val bytes = place.photoUrl?.let { repository.photo(it) } ?: return@withPermit
                    decodePhoto(bytes)?.let { image -> mutex.withLock { photos[place.key] = image } }
                }
            } }.awaitAll()
        }
    }
    return photos
}

@Composable
private fun CollectionPosterPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    val collection = SharedCollection("sample", "Мой кофейный Минск", "https://coffeepeek.by", listOf(
        CollectionPlace(CollectionPlaceKind.CoffeeShop, "sample", "Любимая кофейня", city = "Минск", address = "Кофейная улица, 1"),
        CollectionPlace(CollectionPlaceKind.Roaster, "roaster", "Любимый обжарщик")))
    val painter = CollectionPosterPainter(collection, CollectionPosterFormat.Chat, 0, collectionPosterLabels(collection, CollectionPosterFormat.Chat),
        rememberQrCodePainter(collection.url), rememberTextMeasurer(), MaterialTheme.typography.bodyLarge)
    Image(painter, collection.title, Modifier.width(360.dp).aspectRatio(0.8f))
}
@Preview @Composable private fun CollectionPosterLightPreview() = CollectionPosterPreview(false)
@Preview @Composable private fun CollectionPosterDarkPreview() = CollectionPosterPreview(true)
