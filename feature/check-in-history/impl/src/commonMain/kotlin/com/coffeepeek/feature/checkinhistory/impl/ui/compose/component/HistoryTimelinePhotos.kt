package com.coffeepeek.feature.checkinhistory.impl.ui.compose.component

import androidx.compose.runtime.Composable
import com.coffeepeek.core.designsystem.component.CoffeePeekPhotoStrip
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.checkinhistory.impl.resources.*
import com.coffeepeek.core.designsystem.component.CoffeePeekPhotoStripSources
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun HistoryTimelinePhotosContent(photos: CoffeePeekPhotoStripSources, onPhoto: (Int) -> Unit, image: HistoryImageContent?) {
    val descriptions = photos.originals.indices.map { stringResource(Res.string.history_photo_description, it + 1, photos.originals.size) }
    CoffeePeekPhotoStrip(photos.originals.size, { descriptions[it] }, onPhoto, photoEnabled = photos::canOpen) { index, modifier ->
        HistoryPhoto(photos.display(index), modifier, imageContent = image, highQualityUrl = photos.upgrade(index))
    }
}

@Composable private fun TimelinePhotosPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    HistoryTimelinePhotosContent(CoffeePeekPhotoStripSources(List(4) { "original" }, List(4) { "" }, List(4) { "" }), {}, null)
}
@Preview @Composable private fun TimelinePhotosLightPreview() = TimelinePhotosPreview(false)
@Preview @Composable private fun TimelinePhotosDarkPreview() = TimelinePhotosPreview(true)
