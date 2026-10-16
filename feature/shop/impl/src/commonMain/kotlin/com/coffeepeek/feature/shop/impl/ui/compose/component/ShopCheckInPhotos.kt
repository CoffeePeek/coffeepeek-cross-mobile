package com.coffeepeek.feature.shop.impl.ui.compose.component

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_checkins_photo_description
import com.coffeepeek.feature.shop.impl.resources.shop_checkins_more_photos
import com.coffeepeek.feature.shop.impl.resources.shop_photo_unavailable
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/** At most three tiles; the third tile still opens the complete ordered album. */
@Composable
internal fun ShopCheckInPhotos(urls: List<String>, thumbnailUrls: List<String>,
    onOpenPhoto: (List<String>, Int) -> Unit, modifier: Modifier = Modifier) {
    if (urls.isEmpty()) return
    if (urls.size == 1) {
        PhotoTile(urls, thumbnailUrls, 0, onOpenPhoto, modifier.fillMaxWidth().aspectRatio(4f / 3f))
    } else Row(modifier.fillMaxWidth().aspectRatio(4f / 3f),
        horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing1)) {
        PhotoTile(urls, thumbnailUrls, 0, onOpenPhoto, Modifier.weight(1f).fillMaxHeight())
        if (urls.size == 2) PhotoTile(urls, thumbnailUrls, 1, onOpenPhoto, Modifier.weight(1f).fillMaxHeight())
        else Column(Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing1)) {
            PhotoTile(urls, thumbnailUrls, 1, onOpenPhoto, Modifier.weight(1f).fillMaxWidth())
            PhotoTile(urls, thumbnailUrls, 2, onOpenPhoto, Modifier.weight(1f).fillMaxWidth())
        }
    }
}

@Composable
private fun PhotoTile(urls: List<String>, thumbnails: List<String>, index: Int,
    onOpenPhoto: (List<String>, Int) -> Unit, modifier: Modifier) {
    val fullUrl = urls[index].takeIf(String::isNotBlank)
    val preview = thumbnails.getOrNull(index)?.takeIf(String::isNotBlank) ?: fullUrl
    val description = stringResource(Res.string.shop_checkins_photo_description, index + 1, urls.size)
    Box(modifier.clip(RoundedCornerShape(CoffeePeekDimens.radiusMd))
        .background(MaterialTheme.colorScheme.surfaceVariant)
        .clickable(enabled = fullUrl != null, role = Role.Button, onClick = { onOpenPhoto(urls, index) })
        .semantics(mergeDescendants = true) { contentDescription = description },
        contentAlignment = Alignment.Center) {
        if (preview == null) PhotoUnavailable()
        else KamelImage(resource = { asyncPainterResource(preview) }, contentDescription = null,
            contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
            onLoading = { PhotoUnavailable() }, onFailure = { PhotoUnavailable() })
        if (index == 2 && urls.size > 3) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)),
            contentAlignment = Alignment.Center) {
            Text(stringResource(Res.string.shop_checkins_more_photos, urls.size - 3),
                style = MaterialTheme.typography.headlineSmall, color = Color.White)
        }
    }
}

@Composable
private fun PhotoUnavailable() {
    Text(stringResource(Res.string.shop_photo_unavailable), style = MaterialTheme.typography.labelSmall)
}

// Blank fixture URLs exercise layout/fallback without initiating image requests.
@Preview @Composable private fun ShopCheckInPhotosLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopCheckInPhotos(List(4) { "" }, emptyList(), { _, _ -> })
}
@Preview @Composable private fun ShopCheckInPhotosDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopCheckInPhotos(List(2) { "" }, emptyList(), { _, _ -> })
}
