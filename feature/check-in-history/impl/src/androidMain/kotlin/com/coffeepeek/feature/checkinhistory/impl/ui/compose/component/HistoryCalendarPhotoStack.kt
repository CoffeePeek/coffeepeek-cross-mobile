package com.coffeepeek.feature.checkinhistory.impl.ui.compose.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryCalendarPhoto
import com.coffeepeek.feature.checkinhistory.impl.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun HistoryCalendarPhotoStack(photos: List<HistoryCalendarPhoto>, total: Long, imageContent: HistoryImageContent?) {
    Box(Modifier.fillMaxWidth().height(32.dp).clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
        if (photos.isEmpty()) Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(total.coerceIn(0, 3).toInt()) {
                Box(Modifier.size(4.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
            }
        } else {
            Box(Modifier.width(28.dp + 6.dp * (photos.size - 1)).height(32.dp)) {
                photos.forEachIndexed { index, photo ->
                    val modifier = Modifier.align(Alignment.CenterStart).offset(x = 6.dp * index)
                        .size(28.dp).rotate(if (photos.size > 1) (index - (photos.size - 1) / 2f) * 12f else 0f)
                    HistoryPhoto(photo.url, if (photo.isCutout) modifier else modifier.clip(RoundedCornerShape(6.dp)),
                        if (photo.isCutout) ContentScale.Fit else ContentScale.Crop, imageContent)
                }
            }
            if (total > photos.size) Text(stringResource(Res.string.history_calendar_more_photos, total - photos.size),
                Modifier.align(Alignment.BottomEnd).background(MaterialTheme.colorScheme.surface, CircleShape).padding(horizontal = 2.dp),
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@PreviewLightDark @Composable private fun HistoryCalendarPhotoStackPreview() = CoffeePeekTheme {
    Box(Modifier.width(64.dp)) {
        HistoryCalendarPhotoStack(List(3) { HistoryCalendarPhoto("fixture-$it", true) }, 5) { _, modifier, _ ->
            Icon(CoffeePeekIcons.Coffee, null, modifier, tint = MaterialTheme.colorScheme.primary)
        }
    }
}
