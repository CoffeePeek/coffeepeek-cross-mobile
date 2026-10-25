package com.coffeepeek.feature.checkinhistory.impl.ui.compose.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.*
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.component.CoffeePeekCalendarDay
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
    CoffeePeekCalendarDay(day, isToday, isFuture, isSelected = isSelected,
        modifier = modifier.aspectRatio(1f).padding(1.dp)
            .clickable(enabled = !isFuture, role = Role.Button,
                onClickLabel = stringResource(Res.string.history_select_day), onClick = onSelect)
            .semantics(mergeDescendants = true) { contentDescription = description; selected = isSelected },
        photo = photo?.let { value -> { imageModifier ->
            HistoryPhoto(value.url,
                if (value.isCutout) imageModifier else imageModifier.clip(RoundedCornerShape(6.dp)),
                if (value.isCutout) ContentScale.Fit else ContentScale.Crop, imageContent)
        } },
        footer = if (total > 0) ({ HistoryCalendarVisitMarkers(total, isSelected && !isToday && photo == null) }) else null)
}

@PreviewLightDark @Composable private fun HistoryCalendarDayPreview() = CoffeePeekTheme {
    HistoryCalendarDay(14, "2026-10-14", 2, 1, isSelected = true, isToday = true, isFuture = false,
        onSelect = {}, modifier = Modifier.width(56.dp))
}

@PreviewLightDark @Composable private fun HistoryCalendarDayTodayPreview() = CoffeePeekTheme {
    HistoryCalendarDay(14, "2026-10-14", 0, 0, isSelected = false, isToday = true, isFuture = false,
        onSelect = {}, modifier = Modifier.width(56.dp))
}

@PreviewLightDark @Composable private fun HistoryCalendarDayPhotoPreview() = CoffeePeekTheme {
    HistoryCalendarDay(14, "2026-10-14", 2, 1, isSelected = true, isToday = true, isFuture = false,
        onSelect = {}, modifier = Modifier.width(56.dp),
        photos = listOf(HistoryCalendarPhoto("fixture", true), HistoryCalendarPhoto("fixture-2", false)))
}
