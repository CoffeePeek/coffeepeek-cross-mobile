package com.coffeepeek.feature.checkinhistory.impl.ui.compose.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
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
    Column(modifier.padding(1.dp).clip(RoundedCornerShape(12.dp))
        .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f) else Color.Transparent)
        .clickable(enabled = !isFuture, role = Role.Button,
            onClickLabel = stringResource(Res.string.history_select_day), onClick = onSelect)
        .semantics(mergeDescendants = true) { contentDescription = description; selected = isSelected }
        .heightIn(min = 76.dp).padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.defaultMinSize(minWidth = 32.dp, minHeight = 32.dp)
            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape)
            .then(if (isToday && !isSelected) Modifier.border(1.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier)
            .clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
            Text(day.toString(), style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected || isToday) FontWeight.SemiBold else FontWeight.Normal,
                color = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimary
                    isFuture -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                    else -> MaterialTheme.colorScheme.onSurface
                }, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
        }
        HistoryCalendarPhotoStack(photos.take(total.coerceAtMost(3).toInt()), total, imageContent)
    }
}

@PreviewLightDark @Composable private fun HistoryCalendarDayPreview() = CoffeePeekTheme {
    HistoryCalendarDay(14, "2026-10-14", 2, 1, true, true, false, {}, Modifier.width(56.dp))
}
