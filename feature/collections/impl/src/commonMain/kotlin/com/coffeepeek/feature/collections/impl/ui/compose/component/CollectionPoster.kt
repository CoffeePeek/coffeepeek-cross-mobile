package com.coffeepeek.feature.collections.impl.ui.compose.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.collections.domain.model.*
import com.coffeepeek.feature.collections.domain.repository.CollectionsRepository
import com.coffeepeek.feature.collections.impl.ui.model.CollectionPosterFormat
import com.coffeepeek.feature.collections.impl.resources.*
import io.github.alexzhirkevich.qrose.ImageFormat
import io.github.alexzhirkevich.qrose.toByteArray
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import com.coffeepeek.feature.collections.api.CollectionPhotoDecoder
import com.coffeepeek.feature.collections.impl.ui.model.collectionPosterPage
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.sync.withLock
import kotlin.time.TimeSource
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

@Composable
internal fun CollectionPoster(collection: SharedCollection, format: CollectionPosterFormat, page: Int,
    repository: CollectionsRepository, decodePhoto: CollectionPhotoDecoder, modifier: Modifier = Modifier) {
    val resources = collectionPosterResources(collection, format)
    key(collection, format, page, resources) {
        val initial = remember { CollectionPosterPainter(collection, format, page, resources) }
        val painter by produceState<Painter>(initial) {
            value = preparePoster(collection, format, page, repository, decodePhoto, resources, 4_000L)
        }
        val names = collectionPosterPage(collection.places, format, page).joinToString { it.title }
        Image(painter, stringResource(Res.string.collections_poster_description, collection.title, names),
            modifier.fillMaxWidth().aspectRatio(format.width.toFloat() / format.height))
    }
}

internal suspend fun exportPosters(collection: SharedCollection, format: CollectionPosterFormat, repository: CollectionsRepository, decodePhoto: CollectionPhotoDecoder,
    resources: CollectionPosterResources): List<ByteArray> {
    val count = (collection.places.size + format.perPage - 1) / format.perPage
    val started = TimeSource.Monotonic.markNow()
    return (0 until count).map { page ->
        currentCoroutineContext().ensureActive()
        preparePoster(collection, format, page, repository, decodePhoto, resources,
            (12_000L - started.elapsedNow().inWholeMilliseconds).coerceAtLeast(0))
            .toByteArray(format.width, format.height, ImageFormat.PNG)
    }
}

private suspend fun preparePoster(collection: SharedCollection, format: CollectionPosterFormat, page: Int,
    repository: CollectionsRepository, decodePhoto: CollectionPhotoDecoder, resources: CollectionPosterResources,
    photoBudget: Long): CollectionPosterPainter {
    val places = collectionPosterPage(collection.places, format, page)
    val photos = loadPosterPhotos(places, repository, decodePhoto, photoBudget)
    return CollectionPosterPainter(collection, format, page, resources, photos)
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
private fun CollectionPosterPreview(dark: Boolean, format: CollectionPosterFormat = CollectionPosterFormat.Chat) = CoffeePeekTheme(darkTheme = dark) {
    val collection = SharedCollection("sample", "Мой кофейный Минск", "https://coffeepeek.by", listOf(
        CollectionPlace(CollectionPlaceKind.CoffeeShop, "sample", "Любимая кофейня", city = "Минск", address = "Кофейная улица, 1", rating = 4.6, checkInCount = 124),
        CollectionPlace(CollectionPlaceKind.Roaster, "roaster", "Любимый обжарщик"),
        CollectionPlace(CollectionPlaceKind.CoffeeShop, "second", "Кофейная лаборатория", address = "Кофейная улица, 6", rating = 4.5, checkInCount = 89),
        CollectionPlace(CollectionPlaceKind.Roaster, "second-roaster", "Обжарщики Минска", city = "Минск")))
    val painter = CollectionPosterPainter(collection, format, 0, collectionPosterResources(collection, format))
    Image(painter, collection.title, Modifier.width(360.dp).aspectRatio(format.width.toFloat() / format.height))
}
@Preview @Composable private fun CollectionPosterLightPreview() = CollectionPosterPreview(false)
@Preview @Composable private fun CollectionPosterDarkPreview() = CollectionPosterPreview(true)
@Preview @Composable private fun CollectionPosterStoryLightPreview() = CollectionPosterPreview(false, CollectionPosterFormat.Story)
@Preview @Composable private fun CollectionPosterStoryDarkPreview() = CollectionPosterPreview(true, CollectionPosterFormat.Story)
