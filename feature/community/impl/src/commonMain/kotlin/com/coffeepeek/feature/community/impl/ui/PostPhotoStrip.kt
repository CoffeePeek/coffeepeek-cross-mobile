package com.coffeepeek.feature.community.impl.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekLoader
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import com.coffeepeek.core.designsystem.theme.CoffeePeekColors
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.community.api.CommunityHost
import com.coffeepeek.feature.community.domain.model.PostPhotos
import com.coffeepeek.feature.community.impl.resources.Res
import com.coffeepeek.feature.community.impl.resources.community_loading
import com.coffeepeek.feature.community.impl.resources.community_more_photos
import com.coffeepeek.feature.community.impl.resources.community_photo
import org.jetbrains.compose.ui.tooling.preview.Preview

private val SinglePhotoMaxHeight = 480.dp

@Composable
internal fun PostPhotoStrip(photos: PostPhotos, onPhoto: (Int) -> Unit, host: CommunityHost) {
    when (photos.originals.size) {
        0 -> Unit
        1 -> PostPhotoTile(photos, 0, onPhoto, host,
            Modifier.fillMaxWidth().heightIn(max = SinglePhotoMaxHeight), placeholderRatio = 2f)
        else -> PostPhotoCollage(photos, onPhoto, host)
    }
}

@Composable
private fun PostPhotoCollage(photos: PostPhotos, onPhoto: (Int) -> Unit, host: CommunityHost) {
    Row(Modifier.fillMaxWidth().aspectRatio(4f / 3f), horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing1)) {
        PostPhotoTile(photos, 0, onPhoto, host, Modifier.weight(1f).fillMaxHeight())
        if (photos.originals.size == 2) {
            PostPhotoTile(photos, 1, onPhoto, host, Modifier.weight(1f).fillMaxHeight())
        } else Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing1)) {
            PostPhotoTile(photos, 1, onPhoto, host, Modifier.weight(1f).fillMaxWidth())
            PostPhotoTile(photos, 2, onPhoto, host, Modifier.weight(1f).fillMaxWidth())
        }
    }
}

@Composable
private fun PostPhotoTile(photos: PostPhotos, index: Int, onPhoto: (Int) -> Unit, host: CommunityHost,
    modifier: Modifier, placeholderRatio: Float? = null) {
    val original = photos.originals[index]
    val preview = photos.thumbnails.getOrNull(index)?.takeIf(String::isNotBlank) ?: original
    val detail = photos.inlineSource(index)
    var ratio by remember(original, preview, detail) { mutableStateOf<Float?>(null) }
    val frameRatio = ratio ?: placeholderRatio
    val frame = if (frameRatio != null) Modifier.aspectRatio(frameRatio) else Modifier.fillMaxSize()
    val description = stringResource(Res.string.community_photo, index + 1, photos.originals.size)
    Box(modifier, contentAlignment = Alignment.Center) {
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
            if (index == 2 && photos.originals.size > 3) PostPhotoOverflow(photos.originals.size - 3)
        }
    }
}

/** For extreme ratios, keep enough frame to measure a visible image. Fit still preserves the pixels' exact ratio. */
private fun postPhotoFrameRatio(size: Size): Float? {
    if (!size.isSpecified || size.width <= 0f || size.height <= 0f) return null
    return (size.width / size.height).takeIf { it.isFinite() && it > 0f }?.coerceIn(1f / 16f, 16f)
}

@Composable
private fun PostPhotoOverflow(count: Int) {
    Box(Modifier.fillMaxSize().background(CoffeePeekColors.PhotoBadgeOverlay), contentAlignment = Alignment.Center) {
        Text(stringResource(Res.string.community_more_photos, count), style = MaterialTheme.typography.headlineSmall, color = Color.White)
    }
}

private val photoPreviewHost = CommunityHost(image = { url, _, modifier, _, onDecodedSize ->
    LaunchedEffect(url) {
        val size = if (url == "portrait") Size(300f, 450f) else Size(450f, 300f)
        onDecodedSize?.invoke(size)
    }
    Box(modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh))
})

@Composable
private fun PostPhotoStripPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Column(Modifier.widthIn(max = 300.dp), verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
        PostPhotoStrip(PostPhotos(listOf("portrait"), emptyList(), emptyList()), {}, photoPreviewHost)
        PostPhotoStrip(PostPhotos(listOf("portrait", "landscape", "landscape", "portrait"), emptyList(), emptyList()), {}, photoPreviewHost)
    }
}

@Preview @Composable private fun PostPhotoStripLightPreview() = PostPhotoStripPreview(false)
@Preview @Composable private fun PostPhotoStripDarkPreview() = PostPhotoStripPreview(true)
