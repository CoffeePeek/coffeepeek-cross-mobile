package com.coffeepeek.feature.checkinhistory.impl.ui.compose.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.checkinhistory.impl.resources.Res
import com.coffeepeek.feature.checkinhistory.impl.resources.history_maskot_with_photo
import org.jetbrains.compose.resources.painterResource

internal typealias HistoryImageContent = @Composable (String, Modifier, ContentScale) -> Unit

/** Loading/authentication belong to the caller; local previews never request images. */
@Composable
internal fun HistoryPhoto(
    url: String?,
    modifier: Modifier = Modifier,
    scale: ContentScale = ContentScale.Crop,
    imageContent: HistoryImageContent? = null,
) {
    Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
        if (url.isNullOrBlank() || LocalInspectionMode.current || imageContent == null) {
            Image(painterResource(Res.drawable.history_maskot_with_photo), null,
                modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        } else imageContent(url, Modifier.fillMaxSize(), scale)
    }
}

@PreviewLightDark @Composable private fun HistoryPhotoPreview() = CoffeePeekTheme {
    HistoryPhoto(null, Modifier.size(64.dp))
}
