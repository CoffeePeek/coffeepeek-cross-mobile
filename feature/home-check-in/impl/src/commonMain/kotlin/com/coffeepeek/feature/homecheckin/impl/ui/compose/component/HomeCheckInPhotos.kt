package com.coffeepeek.feature.homecheckin.impl.ui.compose.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.feature.homecheckin.impl.resources.*

/** Feed-only tiles retain album positions even when a preview is unavailable. */
@Composable
internal fun HomeCheckInPhotos(
    urls: List<String>,
    enabled: Boolean,
    imageContent: @Composable (String, String, Modifier, Boolean) -> Unit,
    originalUrls: List<String> = urls,
    onOpen: (Int) -> Unit,
) {
    require(urls.size == originalUrls.size)
    if (urls.isEmpty()) return
    @Composable fun Photo(index: Int, modifier: Modifier) {
        val description = stringResource(Res.string.home_check_in_photo_description, index + 1, urls.size)
        Box(modifier.clip(RoundedCornerShape(CoffeePeekDimens.radiusMd))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(enabled = enabled && originalUrls[index].isNotBlank(), role = Role.Button) { onOpen(index) }
            .semantics(mergeDescendants = true) { contentDescription = description }) {
            if (urls[index].isNotBlank()) imageContent(urls[index], description, Modifier.fillMaxSize(), false)
            if (index == 2 && urls.size > 3) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center) {
                Text(stringResource(Res.string.home_check_in_more_photos, urls.size - 3),
                    style = MaterialTheme.typography.headlineSmall, color = Color.White)
            }
        }
    }
    if (urls.size == 1) Photo(0, Modifier.fillMaxWidth().aspectRatio(16f / 10f))
    else Row(Modifier.fillMaxWidth().aspectRatio(4f / 3f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Photo(0, Modifier.weight(1f).fillMaxHeight())
        if (urls.size == 2) Photo(1, Modifier.weight(1f).fillMaxHeight())
        else Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Photo(1, Modifier.weight(1f).fillMaxWidth())
            Photo(2, Modifier.weight(1f).fillMaxWidth())
        }
    }
}

@Composable private fun HomeCheckInPhotosPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    HomeCheckInPhotos(listOf("fixture-1", "", "fixture-3", "fixture-4"), true,
        originalUrls = List(4) { "fixture-original-$it" },
        imageContent = { _, _, modifier, _ -> Surface(modifier, color = MaterialTheme.colorScheme.surfaceVariant) {} }, onOpen = {})
}
@Preview @Composable private fun HomeCheckInPhotosLightPreview() = HomeCheckInPhotosPreview(false)
@Preview @Composable private fun HomeCheckInPhotosDarkPreview() = HomeCheckInPhotosPreview(true)
