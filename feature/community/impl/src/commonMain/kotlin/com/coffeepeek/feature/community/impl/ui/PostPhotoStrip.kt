package com.coffeepeek.feature.community.impl.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekLoader
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.community.api.CommunityHost
import com.coffeepeek.feature.community.domain.model.PostPhotos
import com.coffeepeek.feature.community.impl.resources.Res
import com.coffeepeek.feature.community.impl.resources.community_loading
import com.coffeepeek.feature.community.impl.resources.community_photo
import org.jetbrains.compose.ui.tooling.preview.Preview

private val SinglePhotoMaxHeight = 480.dp
private val PhotoRowMaxHeight = 260.dp

@Composable
internal fun PostPhotoStrip(photos: PostPhotos, onPhoto: (Int) -> Unit, host: CommunityHost) {
    val count = photos.originals.size
    if (count == 0) return
    val ratios = remember(photos) { List(count) { mutableStateOf<Float?>(null) } }
    if (count == 1) Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopStart) {
        PostPhotoTile(photos, 0, onPhoto, host, ratios.first(), Modifier.heightIn(max = SinglePhotoMaxHeight),
            placeholderRatio = 2f)
    } else PostPhotoRow(photos, onPhoto, host, ratios)
}

@Composable
private fun PostPhotoRow(photos: PostPhotos, onPhoto: (Int) -> Unit, host: CommunityHost, ratios: List<MutableState<Float?>>) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val rowHeight = (maxWidth * 0.72f).coerceAtMost(PhotoRowMaxHeight)
        val tileWidth = maxWidth * 0.88f
        LazyRow(
            modifier = Modifier.fillMaxWidth().height(rowHeight),
            horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2),
            verticalAlignment = Alignment.Top,
        ) {
            items(photos.originals.size, key = { index -> "$index:${photos.originals[index]}" }) { index ->
                PostPhotoTile(photos, index, onPhoto, host, ratios[index], Modifier.widthIn(max = tileWidth),
                    placeholderRatio = 3f / 4f, matchHeightFirst = true)
            }
        }
    }
}

@Composable
private fun PostPhotoTile(photos: PostPhotos, index: Int, onPhoto: (Int) -> Unit, host: CommunityHost,
    measuredRatio: MutableState<Float?>, modifier: Modifier, placeholderRatio: Float, matchHeightFirst: Boolean = false) {
    val original = photos.originals[index]
    val preview = photos.thumbnails.getOrNull(index)?.takeIf(String::isNotBlank) ?: original
    val detail = photos.inlineSource(index)
    var ratio by measuredRatio
    val frame = modifier.aspectRatio(ratio ?: placeholderRatio, matchHeightConstraintsFirst = matchHeightFirst)
    val description = stringResource(Res.string.community_photo, index + 1, photos.originals.size)
    Box(frame.clip(RoundedCornerShape(CoffeePeekDimens.radiusMd))
        .clickable(role = Role.Button) { onPhoto(index) }
        .semantics(mergeDescendants = true) { contentDescription = description },
        contentAlignment = Alignment.Center) {
        Box(Modifier.matchParentSize().background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
            if (ratio == null) CoffeePeekLoader(stringResource(Res.string.community_loading), size = 32.dp)
        }
        host.image(preview, detail, Modifier.fillMaxSize(), ContentScale.Fit) { size ->
            // A sharper rendition or a renewed permission must not move the already visible tile.
            if (ratio == null) ratio = postPhotoFrameRatio(size)
        }
    }
}

/** For extreme ratios, keep enough frame to measure a visible image. Fit still preserves the pixels' exact ratio. */
private fun postPhotoFrameRatio(size: Size): Float? {
    if (!size.isSpecified || size.width <= 0f || size.height <= 0f) return null
    return (size.width / size.height).takeIf { it.isFinite() && it > 0f }?.coerceIn(1f / 16f, 16f)
}

private val photoPreviewHost = CommunityHost(image = { url, _, modifier, _, onDecodedSize ->
    val portrait = url.startsWith("portrait")
    LaunchedEffect(url) {
        val size = if (portrait) Size(300f, 400f) else Size(480f, 300f)
        onDecodedSize?.invoke(size)
    }
    Box(modifier.background(if (portrait) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.tertiaryContainer))
})

@Composable
private fun PostPhotoStripPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Column(Modifier.widthIn(max = 300.dp), verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
        PostPhotoStrip(PostPhotos(listOf("portrait"), emptyList(), emptyList()), {}, photoPreviewHost)
        PostPhotoStrip(PostPhotos(listOf("portrait", "portrait-2"), emptyList(), emptyList()), {}, photoPreviewHost)
        PostPhotoStrip(PostPhotos(listOf("landscape", "portrait", "landscape-2", "portrait-2"), emptyList(), emptyList()), {}, photoPreviewHost)
    }
}

@Preview @Composable private fun PostPhotoStripLightPreview() = PostPhotoStripPreview(false)
@Preview @Composable private fun PostPhotoStripDarkPreview() = PostPhotoStripPreview(true)
