package com.coffeepeek.admin.ui.screen.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import coffeepeek.composeapp.generated.resources.*
import com.coffeepeek.admin.ui.screen.checkins.CalendarPhoto
import com.coffeepeek.admin.ui.screen.checkins.CalendarWeek
import com.coffeepeek.admin.utils.CpImage
import com.coffeepeek.core.designsystem.component.CoffeePeekCard
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** One week layout for personal and public activity; callers supply only the data they may expose. */
@Composable
internal fun ProfileActivityWeek(
    week: CalendarWeek, counts: Map<String, Int>, photos: Map<String, List<CalendarPhoto>>,
    loading: Boolean, failed: Boolean, onClick: () -> Unit, onRetry: () -> Unit,
) {
    val labels = listOf(Res.string.profile_week_monday, Res.string.profile_week_tuesday, Res.string.profile_week_wednesday,
        Res.string.profile_week_thursday, Res.string.profile_week_friday, Res.string.profile_week_saturday, Res.string.profile_week_sunday)
    val openLabel = stringResource(Res.string.profile_week_open)
    CoffeePeekCard(
        modifier = Modifier.semantics { contentDescription = openLabel }, onClick = onClick,
        elevation = 0.dp, shape = RoundedCornerShape(20.dp),
        border = BorderStroke(0.7.dp, MaterialTheme.colorScheme.outlineVariant),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            week.dates.forEachIndexed { index, date ->
                ProfileWeekDay(date, stringResource(labels[index]), counts[date] ?: 0, photos[date].orEmpty(),
                    isToday = date == week.today, modifier = Modifier.weight(1f))
            }
        }
        if (loading) Text(stringResource(Res.string.profile_week_loading), Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        else if (failed) Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(Res.string.profile_week_error), Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
            TextButton(onClick = onRetry) { Text(stringResource(Res.string.profile_week_retry)) }
        }
    }
}

@Composable
private fun ProfileWeekDay(date: String, label: String, count: Int, photos: List<CalendarPhoto>,
    isToday: Boolean, modifier: Modifier) {
    val visits = pluralStringResource(Res.plurals.profile_week_count, count, count)
    val description = stringResource(Res.string.profile_week_day, date, visits)
    Column(modifier.semantics(mergeDescendants = true) { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
            if (photos.isNotEmpty()) {
                CpImage(data = photos.first().url, modifier = Modifier.fillMaxSize().clearAndSetSemantics {}, contentScale = ContentScale.Crop)
                if (photos.size > 1) Text("+${photos.size - 1}",
                    Modifier.align(Alignment.BottomEnd).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)).padding(horizontal = 3.dp),
                    style = MaterialTheme.typography.labelSmall)
            } else Box(Modifier.size(8.dp).background(if (count > 0) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f), CircleShape))
        }
    }
}

@Composable
private fun ProfileWeekPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    ProfileActivityWeek(CalendarWeek("2026-10-22"), mapOf("2026-10-20" to 2), emptyMap(), false, false, {}, {})
}

@Preview @Composable private fun ProfileWeekLightPreview() = ProfileWeekPreview(false)
@Preview @Composable private fun ProfileWeekDarkPreview() = ProfileWeekPreview(true)
