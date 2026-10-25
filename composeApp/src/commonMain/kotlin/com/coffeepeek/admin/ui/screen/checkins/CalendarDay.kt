package com.coffeepeek.admin.ui.screen.checkins

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coffeepeek.composeapp.generated.resources.*
import com.coffeepeek.admin.utils.CpImage
import com.coffeepeek.core.designsystem.component.CoffeePeekCalendarDay
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.domain.model.CheckIn
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.tooling.preview.Preview

/** Legacy profile-month adapter shares the same visual day as profile weeks and migrated history. */
@Composable
internal fun CalendarDay(day: Int, date: String, checkIns: List<CheckIn>, homeCount: Int,
    photos: List<CalendarPhoto>, isSelected: Boolean, isToday: Boolean, isFuture: Boolean,
    onClick: () -> Unit, modifier: Modifier) {
    val dateLabel = if (isToday) stringResource(Res.string.calendar_today_suffix, selectedDateTitle(date)) else selectedDateTitle(date)
    val visits = stringResource(Res.string.calendar_date_description, dateLabel, visitCount(checkIns.size + homeCount))
    val description = if (homeCount > 0) stringResource(Res.string.calendar_home_description, visits, homeCount) else visits
    CoffeePeekCalendarDay(day, isToday, isFuture, isSelected = isSelected,
        modifier = modifier.aspectRatio(1f).padding(1.dp)
            .clickable(enabled = !isFuture, role = Role.Button,
                onClickLabel = stringResource(Res.string.calendar_select_date), onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = description; selected = isSelected },
        photo = photos.firstOrNull()?.let { photo -> { imageModifier ->
            CpImage(data = photo.url, contentScale = ContentScale.Crop, modifier = imageModifier.clip(RoundedCornerShape(6.dp)))
        } }, footer = if (photos.size > 1) ({
            Text(stringResource(Res.string.calendar_more_photos, photos.size - 1),
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }) else null)
}

@Preview @Composable private fun CalendarDayLightPreview() = CoffeePeekTheme(darkTheme = false) {
    CalendarDay(25, "2026-10-25", emptyList(), 0, emptyList(), false, true, false, {}, Modifier.size(48.dp))
}
@Preview @Composable private fun CalendarDayDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    CalendarDay(25, "2026-10-25", emptyList(), 0, emptyList(), false, true, false, {}, Modifier.size(48.dp))
}
