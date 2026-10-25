package com.coffeepeek.admin.ui.screen.profile

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coffeepeek.composeapp.generated.resources.*
import com.coffeepeek.admin.ui.screen.checkins.CalendarPhoto
import com.coffeepeek.admin.ui.screen.checkins.CalendarWeek
import com.coffeepeek.admin.utils.CpImage
import com.coffeepeek.core.designsystem.component.CoffeePeekActivityDay
import com.coffeepeek.core.designsystem.component.CoffeePeekActivityWeek
import com.coffeepeek.core.designsystem.component.CoffeePeekActivityWeekLabels
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Compatibility adapter for legacy/public callers; shared week geometry lives in design-system. */
@Composable
internal fun ProfileActivityWeek(
    week: CalendarWeek, counts: Map<String, Int>, photos: Map<String, List<CalendarPhoto>>,
    loading: Boolean, failed: Boolean, onClick: () -> Unit, onRetry: () -> Unit,
) {
    val labels = listOf(Res.string.profile_week_monday, Res.string.profile_week_tuesday, Res.string.profile_week_wednesday,
        Res.string.profile_week_thursday, Res.string.profile_week_friday, Res.string.profile_week_saturday, Res.string.profile_week_sunday)
    val days = week.dates.mapIndexed { index, date ->
        val count = counts[date] ?: 0
        CoffeePeekActivityDay(stringResource(labels[index]),
            stringResource(Res.string.profile_week_day, date, pluralStringResource(Res.plurals.profile_week_count, count, count)),
            date == week.today, count > 0, photos[date].orEmpty().size)
    }
    CoffeePeekActivityWeek(days, CoffeePeekActivityWeekLabels(stringResource(Res.string.profile_week_open),
        stringResource(Res.string.profile_week_loading), stringResource(Res.string.profile_week_error),
        stringResource(Res.string.profile_week_retry)), loading, failed, onClick, onRetry) { index, modifier ->
        photos[week.dates[index]].orEmpty().firstOrNull()?.let { photo ->
            CpImage(data = photo.url, modifier = modifier, contentScale = ContentScale.Crop)
        }
    }
}

@Composable
private fun ProfileWeekPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    ProfileActivityWeek(CalendarWeek("2026-10-22"), mapOf("2026-10-20" to 2), emptyMap(), false, false, {}, {})
}

@Preview @Composable private fun ProfileWeekLightPreview() = ProfileWeekPreview(false)
@Preview @Composable private fun ProfileWeekDarkPreview() = ProfileWeekPreview(true)
