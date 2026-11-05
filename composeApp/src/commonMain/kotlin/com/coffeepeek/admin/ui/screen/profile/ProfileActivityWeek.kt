package com.coffeepeek.admin.ui.screen.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.coffeepeek.core.designsystem.component.CoffeePeekTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import coffeepeek.composeapp.generated.resources.*
import com.coffeepeek.admin.ui.screen.checkins.CalendarPhoto
import com.coffeepeek.admin.ui.screen.checkins.CalendarWeek
import com.coffeepeek.core.designsystem.component.CoffeePeekCard
import com.coffeepeek.core.designsystem.component.CoffeePeekImage
import com.coffeepeek.core.designsystem.component.CoffeePeekShimmer
import com.coffeepeek.core.designsystem.component.CoffeePeekActivityDay
import com.coffeepeek.core.designsystem.component.CoffeePeekActivityWeek
import com.coffeepeek.core.designsystem.component.CoffeePeekActivityWeekLabels
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

/** Compatibility adapter for legacy/public callers; shared week geometry lives in design-system. */
@Composable
internal fun ProfileActivityWeek(
    week: CalendarWeek, counts: Map<String, Int>, photos: Map<String, List<CalendarPhoto>>,
    loading: Boolean, failed: Boolean, onClick: () -> Unit, onRetry: () -> Unit,
) {
    if (loading) {
        ProfileActivityWeekShimmer()
        return
    }
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
            CoffeePeekImage(data = photo.url, modifier = modifier, contentScale = ContentScale.Crop)
        }
    }
}

@Composable
private fun ProfileActivityWeekShimmer() {
    val loading = stringResource(Res.string.profile_week_loading)
    CoffeePeekCard(modifier = Modifier.semantics { contentDescription = loading }, elevation = 0.dp,
        shape = RoundedCornerShape(20.dp), border = BorderStroke(0.7.dp, MaterialTheme.colorScheme.outlineVariant),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(7) {
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    CoffeePeekShimmer(Modifier.width(18.dp).height(10.dp))
                    CoffeePeekShimmer(Modifier.fillMaxWidth().aspectRatio(1f), RoundedCornerShape(10.dp))
                }
            }
        }
    }
}

@Composable
private fun ProfileWeekPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    ProfileActivityWeek(CalendarWeek("2026-10-22"), mapOf("2026-10-20" to 2), emptyMap(), false, false, {}, {})
}

@Preview @Composable private fun ProfileWeekLightPreview() = ProfileWeekPreview(false)
@Preview @Composable private fun ProfileWeekDarkPreview() = ProfileWeekPreview(true)
@Composable private fun ProfileWeekLoadingPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    ProfileActivityWeek(CalendarWeek("2026-10-22"), emptyMap(), emptyMap(), true, false, {}, {})
}
@Preview @Composable private fun ProfileWeekLoadingLightPreview() = ProfileWeekLoadingPreview(false)
@Preview @Composable private fun ProfileWeekLoadingDarkPreview() = ProfileWeekLoadingPreview(true)
