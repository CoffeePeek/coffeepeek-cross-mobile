package com.coffeepeek.feature.shelf.impl.ui.compose.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekShimmer
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shelf.domain.model.PublicShelfItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.jetbrains.compose.ui.tooling.preview.Preview

private sealed interface PublicPhotoState {
    data object Loading : PublicPhotoState
    data class Ready(val bitmap: ImageBitmap) : PublicPhotoState
    data object Unavailable : PublicPhotoState
}

/** All requests go through the anonymous public-photo repository, never the protected owner image port. */
@Composable
internal fun ShelfPublicPhoto(item: PublicShelfItem, load: suspend (String) -> Result<ByteArray>, modifier: Modifier) {
    val url = item.photoUrl?.takeIf(String::isNotBlank)
    val photo by produceState<PublicPhotoState>(if (url == null) PublicPhotoState.Unavailable else PublicPhotoState.Loading,
        url, load) {
        value = if (url == null) PublicPhotoState.Unavailable else PublicPhotoState.Loading
        if (url != null) value = publicShelfBitmap(url, load)?.let { PublicPhotoState.Ready(it) } ?: PublicPhotoState.Unavailable
    }
    when (val current = photo) {
        PublicPhotoState.Loading -> CoffeePeekShimmer(modifier, shape = RoundedCornerShape(4.dp))
        is PublicPhotoState.Ready -> Image(current.bitmap, null, modifier, contentScale = ContentScale.Fit)
        PublicPhotoState.Unavailable -> ShelfPublicPhotoFallback(modifier)
    }
}

@Composable
internal fun ShelfPublicPhotoFallback(modifier: Modifier = Modifier) {
    Box(modifier.clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
        .clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
        Icon(CoffeePeekIcons.Photo, contentDescription = null, modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private suspend fun publicShelfBitmap(url: String, load: suspend (String) -> Result<ByteArray>): ImageBitmap? {
    var loadedBytes: ByteArray? = null
    return try {
        val result = load(url)
        result.exceptionOrNull()?.let { if (it is CancellationException) throw it }
        val bytes = result.getOrNull() ?: return null
        loadedBytes = bytes
        withContext(Dispatchers.Default) { bytes.takeIf(::publicShelfPhotoFits)?.decodeToImageBitmap() }
    }
    catch (cancelled: CancellationException) { throw cancelled }
    catch (_: Exception) { null }
    finally { loadedBytes?.fill(0) }
}

@Composable
private fun ShelfPublicPhotoSample(dark: Boolean, loading: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    val modifier = Modifier.size(112.dp, 136.dp)
    if (loading) CoffeePeekShimmer(modifier, shape = RoundedCornerShape(4.dp))
    else ShelfPublicPhotoFallback(modifier)
}

@Preview @Composable private fun ShelfPublicPhotoLoadingLightPreview() = ShelfPublicPhotoSample(false, true)
@Preview @Composable private fun ShelfPublicPhotoLoadingDarkPreview() = ShelfPublicPhotoSample(true, true)
@Preview @Composable private fun ShelfPublicPhotoMissingLightPreview() = ShelfPublicPhotoSample(false, false)
@Preview @Composable private fun ShelfPublicPhotoMissingDarkPreview() = ShelfPublicPhotoSample(true, false)
