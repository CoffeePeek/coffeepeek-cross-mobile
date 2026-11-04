package com.coffeepeek.feature.shelf.impl.ui.compose.component

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shelf.domain.model.PublicShelfItem
import com.coffeepeek.feature.shelf.impl.resources.Res
import com.coffeepeek.feature.shelf.impl.resources.shelf_bag_untitled
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

/** All requests go through the anonymous public-photo repository, never the protected owner image port. */
@Composable
internal fun ShelfPublicPhoto(item: PublicShelfItem, load: suspend (String) -> Result<ByteArray>, modifier: Modifier) {
    val bitmap by produceState<ImageBitmap?>(null, item.photoUrl, load) {
        value = item.photoUrl?.let { publicShelfBitmap(it, load) }
    }
    val image = bitmap
    if (image != null) Image(image, null, modifier, contentScale = ContentScale.Fit)
    else ShelfBagPlaceholder(item.name.takeIf(String::isNotBlank) ?: stringResource(Res.string.shelf_bag_untitled),
        modifier.clearAndSetSemantics { })
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
private fun ShelfPublicPhotoSample(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    ShelfPublicPhoto(PublicShelfItem("sample", "Эфиопия", null, null, null), { Result.failure(IllegalStateException()) },
        Modifier.size(112.dp, 136.dp))
}

@Preview @Composable private fun ShelfPublicPhotoLightPreview() = ShelfPublicPhotoSample(false)
@Preview @Composable private fun ShelfPublicPhotoDarkPreview() = ShelfPublicPhotoSample(true)
