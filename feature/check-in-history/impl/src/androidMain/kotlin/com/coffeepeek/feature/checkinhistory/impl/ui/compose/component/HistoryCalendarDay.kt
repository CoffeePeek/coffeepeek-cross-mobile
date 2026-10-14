package com.coffeepeek.feature.checkinhistory.impl.ui.compose.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.checkinhistory.domain.model.PersonalCheckIn
import com.coffeepeek.feature.checkinhistory.impl.resources.*
import com.coffeepeek.feature.checkinhistory.impl.ui.data.historyDateTitle
import com.coffeepeek.feature.checkinhistory.impl.ui.data.historyVisitCountLabel
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun HistoryCalendarDay(day: Int, date: String, checkIns: List<PersonalCheckIn>, isSelected: Boolean,
    isToday: Boolean, connectsPrevious: Boolean, connectsNext: Boolean, inStreak: Boolean,
    onSelect: () -> Unit, modifier: Modifier = Modifier, imageContent: HistoryImageContent? = null) {
    val shape = RoundedCornerShape(
        topStart = if (connectsPrevious) 0.dp else CoffeePeekDimens.radiusLg,
        bottomStart = if (connectsPrevious) 0.dp else CoffeePeekDimens.radiusLg,
        topEnd = if (connectsNext) 0.dp else CoffeePeekDimens.radiusLg,
        bottomEnd = if (connectsNext) 0.dp else CoffeePeekDimens.radiusLg)
    val visits = if (checkIns.isEmpty()) stringResource(Res.string.history_no_visits)
        else historyVisitCountLabel(checkIns.size)
    val description = stringResource(Res.string.history_day_description, historyDateTitle(date), visits)
    val selectLabel = stringResource(Res.string.history_select_day)
    Box(modifier.heightIn(min = 52.dp).clip(shape)
        .background(when {
            isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
            inStreak -> MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            else -> Color.Transparent
        }).then(when {
            inStreak -> Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, shape)
            isToday -> Modifier.border(1.dp, MaterialTheme.colorScheme.primary, shape)
            else -> Modifier
        }).clickable(role = Role.Button, onClickLabel = selectLabel, onClick = onSelect)
        .clearAndSetSemantics {
            contentDescription = description
            selected = isSelected
            role = Role.Button
            onClick(selectLabel) { onSelect(); true }
        }, contentAlignment = Alignment.Center) {
        if (checkIns.isEmpty()) Text(day.toString(), style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium)
        else HistoryCalendarThumbnail(checkIns, imageContent = imageContent)
    }
}

@PreviewLightDark @Composable private fun HistoryCalendarDayPreview() = CoffeePeekTheme {
    HistoryCalendarDay(14, "2026-10-14", listOf(previewHistoryCheckIn()), true, true,
        false, true, true, {}, Modifier.size(56.dp))
}
