package com.coffeepeek.feature.checkinhistory.impl.ui.compose.component

import androidx.compose.foundation.background
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryCalendarPhoto
import com.coffeepeek.feature.checkinhistory.impl.resources.*
import com.coffeepeek.feature.checkinhistory.impl.ui.data.historyDateTitle
import com.coffeepeek.feature.checkinhistory.impl.ui.data.historyVisitCountLabel
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun HistoryCalendarDay(day: Int, date: String, total: Long, homeCount: Int,
    isSelected: Boolean, isToday: Boolean, isFuture: Boolean, onSelect: () -> Unit,
    modifier: Modifier = Modifier, photos: List<HistoryCalendarPhoto> = emptyList(), imageContent: HistoryImageContent? = null) {
    val dateLabel = if (isToday) stringResource(Res.string.history_day_today, historyDateTitle(date)) else historyDateTitle(date)
    val description = stringResource(Res.string.history_day_description, dateLabel, historyVisitCountLabel(total)) +
        if (homeCount > 0) ", " + stringResource(Res.string.history_home_count, homeCount) else ""
    val photo = photos.firstOrNull()
    val shape = RoundedCornerShape(12.dp)
    Box(modifier.aspectRatio(1f).padding(1.dp).clip(shape)
        .background(when {
            isSelected && photo == null -> MaterialTheme.colorScheme.primary
            isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
            else -> Color.Transparent
        })
        .then(if ((isToday && !isSelected) || (isSelected && photo != null)) {
            Modifier.border(1.dp, MaterialTheme.colorScheme.primary, shape)
        } else Modifier)
        .clickable(enabled = !isFuture, role = Role.Button,
            onClickLabel = stringResource(Res.string.history_select_day), onClick = onSelect)
        .semantics(mergeDescendants = true) { contentDescription = description; selected = isSelected },
        contentAlignment = Alignment.Center) {
        if (photo == null) {
            Text(day.toString(), style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected || isToday) FontWeight.SemiBold else FontWeight.Normal,
                color = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimary
                    isFuture -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                    else -> MaterialTheme.colorScheme.onSurface
                }, modifier = Modifier.padding(bottom = if (total > 0) 6.dp else 0.dp)
                    .padding(4.dp).clearAndSetSemantics {})
        } else {
            Box(Modifier.fillMaxSize().padding(start = 3.dp, top = 3.dp, end = 3.dp,
                bottom = if (total > 3) 17.dp else if (total > 0) 11.dp else 3.dp)
                .clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
                val imageModifier = Modifier.aspectRatio(1f, matchHeightConstraintsFirst = true)
                HistoryPhoto(photo.url,
                    if (photo.isCutout) imageModifier else imageModifier.clip(RoundedCornerShape(6.dp)),
                    if (photo.isCutout) ContentScale.Fit else ContentScale.Crop, imageContent)
            }
        }
        HistoryCalendarVisitMarkers(total, isSelected && photo == null,
            Modifier.align(Alignment.BottomCenter).padding(bottom = 3.dp))
    }
}

@Preview @Composable private fun HistoryCalendarDayPreview() = CoffeePeekTheme {
    HistoryCalendarDay(14, "2026-10-14", 2, 1, isSelected = true, isToday = true, isFuture = false,
        onSelect = {}, modifier = Modifier.width(56.dp))
}

@Preview @Composable private fun HistoryCalendarDayTodayPreview() = CoffeePeekTheme {
    HistoryCalendarDay(14, "2026-10-14", 0, 0, isSelected = false, isToday = true, isFuture = false,
        onSelect = {}, modifier = Modifier.width(56.dp))
}

@Preview @Composable private fun HistoryCalendarDayPhotoPreview() = CoffeePeekTheme {
    HistoryCalendarDay(14, "2026-10-14", 2, 1, isSelected = true, isToday = true, isFuture = false,
        onSelect = {}, modifier = Modifier.width(56.dp),
        photos = listOf(HistoryCalendarPhoto("fixture", true), HistoryCalendarPhoto("fixture-2", false)))
}
