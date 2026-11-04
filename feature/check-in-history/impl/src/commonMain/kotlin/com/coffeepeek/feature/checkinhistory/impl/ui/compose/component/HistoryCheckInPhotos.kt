package com.coffeepeek.feature.checkinhistory.impl.ui.compose.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.checkinhistory.impl.resources.*
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

@Composable
internal fun HistoryCheckInPhotos(urls: List<String>, onOpenPhoto: (Int) -> Unit, modifier: Modifier = Modifier,
    imageContent: HistoryImageContent? = null, originalUrls: List<String> = urls,
    detailUrls: List<String>? = null) {
    require(originalUrls.size == urls.size)
    if (urls.isEmpty()) return
    if (urls.size == 1) HistoryPhotoTile(urls, originalUrls, detailUrls, 0, onOpenPhoto, modifier.fillMaxWidth().aspectRatio(4f / 3f), imageContent)
    else Row(modifier.fillMaxWidth().aspectRatio(4f / 3f), horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing1)) {
        HistoryPhotoTile(urls, originalUrls, detailUrls, 0, onOpenPhoto, Modifier.weight(1f).fillMaxHeight(), imageContent)
        if (urls.size == 2) HistoryPhotoTile(urls, originalUrls, detailUrls, 1, onOpenPhoto, Modifier.weight(1f).fillMaxHeight(), imageContent)
        else Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing1)) {
            HistoryPhotoTile(urls, originalUrls, detailUrls, 1, onOpenPhoto, Modifier.weight(1f).fillMaxWidth(), imageContent)
            HistoryPhotoTile(urls, originalUrls, detailUrls, 2, onOpenPhoto, Modifier.weight(1f).fillMaxWidth(), imageContent)
        }
    }
}

@Composable
private fun HistoryPhotoTile(urls: List<String>, originals: List<String>, details: List<String>?, index: Int, onOpenPhoto: (Int) -> Unit, modifier: Modifier,
    imageContent: HistoryImageContent?) {
    val description = stringResource(Res.string.history_photo_description, index + 1, urls.size)
    Box(modifier.clip(RoundedCornerShape(CoffeePeekDimens.radiusMd))
        .clickable(enabled = originals[index].isNotBlank(), role = Role.Button) { onOpenPhoto(index) }
        .semantics(mergeDescendants = true) { contentDescription = description }, contentAlignment = Alignment.Center) {
        HistoryPhoto(urls[index], Modifier.fillMaxSize(), imageContent = imageContent,
            highQualityUrl = details?.getOrNull(index)?.takeIf { it.isNotBlank() && it != urls[index] })
        if (index == 2 && urls.size > 3) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)),
            contentAlignment = Alignment.Center) {
            Text(stringResource(Res.string.history_more_photos, urls.size - 3), color = Color.White,
                style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable private fun HistoryCheckInPhotosPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    HistoryCheckInPhotos(List(4) { "" }, {}, originalUrls = List(4) { "fixture-original-$it" })
}
@Preview @Composable private fun HistoryCheckInPhotosLightPreview() = HistoryCheckInPhotosPreview(false)
@Preview @Composable private fun HistoryCheckInPhotosDarkPreview() = HistoryCheckInPhotosPreview(true)
