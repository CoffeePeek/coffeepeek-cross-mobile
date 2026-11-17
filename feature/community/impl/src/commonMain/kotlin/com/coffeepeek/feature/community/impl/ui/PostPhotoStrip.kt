package com.coffeepeek.feature.community.impl.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekTimelinePhotoStrip
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import com.coffeepeek.feature.community.api.CommunityHost
import com.coffeepeek.feature.community.domain.model.PostPhotos
import com.coffeepeek.feature.community.impl.resources.Res
import com.coffeepeek.feature.community.impl.resources.community_photo

@Composable
internal fun PostPhotoStrip(photos: PostPhotos, onPhoto: (Int) -> Unit, host: CommunityHost, horizontalInset: Dp = 0.dp) {
    val descriptions = photos.originals.indices.map { stringResource(Res.string.community_photo, it + 1, photos.originals.size) }
    CoffeePeekTimelinePhotoStrip(photos.originals, { descriptions[it] }, onPhoto, horizontalInset) { index, modifier, scale, onSize ->
        val original = photos.originals[index]
        val preview = photos.thumbnails.getOrNull(index)?.takeIf(String::isNotBlank) ?: original
        host.image(preview, photos.inlineSource(index), modifier, scale, onSize)
    }
}
