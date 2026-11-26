package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

typealias CoffeePeekTimelinePhotoImage = @Composable (Int, Modifier, ContentScale, (Size) -> Unit) -> Unit

private val PostPhotoMaxHeight = 260.dp
private const val MinimumPostPhotoPreviewRatio = 3f / 4f

@Composable
fun CoffeePeekTimelinePhotoStrip(
    photoKeys: List<String>, description: (Int) -> String, onPhoto: (Int) -> Unit,
    horizontalInset: Dp = 0.dp, photoEnabled: (Int) -> Boolean = { true }, image: CoffeePeekTimelinePhotoImage,
) {
    val count = photoKeys.size
    if (count == 0) return
    val ratios = remember(photoKeys) { List(count) { mutableStateOf<Float?>(null) } }
    BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopStart) {
        val contentWidth = (maxWidth - horizontalInset * 2).coerceAtLeast(0.dp)
        val previewHeight = (contentWidth * 0.72f).coerceAtMost(PostPhotoMaxHeight)
        if (count == 1) {
            TimelinePhotoTile(0, description(0), photoEnabled(0), onPhoto, image, ratios.first(),
                Modifier.padding(horizontal = horizontalInset).heightIn(max = previewHeight),
                placeholderRatio = 2f)
        } else TimelinePhotoRow(photoKeys, description, photoEnabled, onPhoto, image, ratios, previewHeight, contentWidth * 0.88f, horizontalInset)
    }
}

@Composable
private fun TimelinePhotoRow(photoKeys: List<String>, description: (Int) -> String, photoEnabled: (Int) -> Boolean,
    onPhoto: (Int) -> Unit, image: CoffeePeekTimelinePhotoImage, ratios: List<MutableState<Float?>>, rowHeight: Dp, tileWidth: Dp, horizontalInset: Dp) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().height(rowHeight),
        contentPadding = PaddingValues(horizontal = horizontalInset),
        horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2),
        verticalAlignment = Alignment.Top,
    ) {
        items(photoKeys.size, key = { index -> "$index:${photoKeys[index]}" }) { index ->
            TimelinePhotoTile(index, description(index), photoEnabled(index), onPhoto, image, ratios[index], Modifier.widthIn(max = tileWidth),
                placeholderRatio = MinimumPostPhotoPreviewRatio, matchHeightFirst = true)
        }
    }
}

@Composable
private fun TimelinePhotoTile(index: Int, description: String, enabled: Boolean, onPhoto: (Int) -> Unit, image: CoffeePeekTimelinePhotoImage,
    measuredRatio: MutableState<Float?>, modifier: Modifier, placeholderRatio: Float, matchHeightFirst: Boolean = false) {
    var ratio by measuredRatio
    val cropTallPhoto = ratio?.let { it < MinimumPostPhotoPreviewRatio } == true
    val frameRatio = ratio?.coerceAtLeast(MinimumPostPhotoPreviewRatio) ?: placeholderRatio
    val scale = if (cropTallPhoto) ContentScale.Crop else ContentScale.Fit
    val frame = modifier.aspectRatio(frameRatio, matchHeightConstraintsFirst = matchHeightFirst)
    Box(frame.clip(RoundedCornerShape(CoffeePeekDimens.radiusMd))
        .clickable(enabled = enabled, role = Role.Button) { onPhoto(index) }
        .semantics(mergeDescendants = true) { contentDescription = description },
        contentAlignment = Alignment.Center) {
        image(index, Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant), scale) { size ->
            // A sharper rendition or a renewed permission must not move the already visible tile.
            if (ratio == null) ratio = decodedPostPhotoRatio(size)
        }
    }
}

/** Use the decoded dimensions so rotations and older posts without image metadata follow the same rule. */
private fun decodedPostPhotoRatio(size: Size): Float? {
    if (!size.isSpecified || size.width <= 0f || size.height <= 0f) return null
    return (size.width / size.height).takeIf { it.isFinite() && it > 0f }?.coerceAtMost(16f)
}

@Composable
private fun PreviewTimelinePhoto(url: String, modifier: Modifier, onDecodedSize: (Size) -> Unit) {
    val portrait = url.startsWith("portrait")
    LaunchedEffect(url) {
        val size = when {
            url == "screenshot" -> Size(360f, 800f)
            portrait -> Size(300f, 400f)
            else -> Size(480f, 300f)
        }
        onDecodedSize(size)
    }
    Box(modifier.background(if (portrait) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.tertiaryContainer))
}

@Composable
private fun PostPhotoStripPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Column(Modifier.widthIn(max = 300.dp), verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
        listOf(listOf("screenshot"), listOf("portrait"), listOf("portrait", "screenshot", "landscape")).forEach { photos ->
            CoffeePeekTimelinePhotoStrip(photos, { photos[it] }, {}, horizontalInset = CoffeePeekDimens.spacing4) { index, modifier, _, onSize ->
                PreviewTimelinePhoto(photos[index], modifier, onSize)
            }
        }
    }
}

@Preview @Composable private fun PostPhotoStripLightPreview() = PostPhotoStripPreview(false)
@Preview @Composable private fun PostPhotoStripDarkPreview() = PostPhotoStripPreview(true)
