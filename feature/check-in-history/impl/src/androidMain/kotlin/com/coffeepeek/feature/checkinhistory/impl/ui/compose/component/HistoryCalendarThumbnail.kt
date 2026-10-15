package com.coffeepeek.feature.checkinhistory.impl.ui.compose.component

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.checkinhistory.domain.model.PersonalCheckIn

@Composable
internal fun HistoryCalendarThumbnail(checkIns: List<PersonalCheckIn>, modifier: Modifier = Modifier,
    imageContent: HistoryImageContent? = null) {
    val visible = checkIns.take(3)
    if (visible.isEmpty()) return
    Box(modifier.width(36.dp + 5.dp * (visible.size - 1)).height(36.dp)) {
        visible.forEachIndexed { index, checkIn ->
            HistoryPhoto(checkIn.photoUrls.firstOrNull(),
                Modifier.align(Alignment.CenterStart).offset(x = 5.dp * index).size(36.dp)
                    .clip(CircleShape).border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape),
                imageContent = imageContent)
        }
    }
}

@PreviewLightDark @Composable private fun HistoryCalendarThumbnailPreview() = CoffeePeekTheme {
    HistoryCalendarThumbnail(List(3) { previewHistoryCheckIn().copy(id = "$it") })
}
