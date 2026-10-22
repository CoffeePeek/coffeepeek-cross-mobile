package com.coffeepeek.admin.feature.profile.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.coffeepeek.admin.ui.screen.checkins.CalendarPhoto
import com.coffeepeek.admin.ui.screen.checkins.CalendarWeek
import com.coffeepeek.admin.ui.screen.profile.ProfileActivityWeek
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.profile.domain.PublicProfileActivity
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours

/** Only public activity and the viewed author's public feed enter this preview. */
@Composable
@OptIn(kotlin.time.ExperimentalTime::class)
internal fun PublicProfileActivityPreview(
    slug: String, activity: PublicProfileActivity?, checkIns: List<Pair<String, List<String>>>,
    loading: Boolean, failed: Boolean, onRetry: () -> Unit, onClick: () -> Unit,
) {
    val today = (Clock.System.now() + 3.hours).toString().take(10)
    val week = remember(today) { CalendarWeek(today) }
    var activities by remember(slug, today) { mutableStateOf(emptyMap<String, PublicProfileActivity>()) }
    LaunchedEffect(activity, week) {
        if (activity != null && week.dates.any { it.startsWith(activity.month) }) {
            activities = activities + (activity.month to activity)
        }
    }
    val grouped = publicProfileWeekCheckIns(checkIns)
    val counts = publicProfileWeekCounts(week, activities, grouped)
    // shortcut: photos come from loaded public feed pages; use a week-photo endpoint when the backend provides one.
    val photos = grouped.mapValues { (_, items) -> items.flatten().filter(String::isNotBlank).distinct().map(::CalendarPhoto) }
    ProfileActivityWeek(week, counts, photos, loading && activities.isEmpty(), failed, onClick, onRetry)
}

@OptIn(kotlin.time.ExperimentalTime::class)
internal fun publicProfileWeekCheckIns(checkIns: List<Pair<String, List<String>>>): Map<String, List<List<String>>> =
    checkIns.mapNotNull { (occurredAt, photos) ->
        runCatching { (kotlin.time.Instant.parse(occurredAt) + 3.hours).toString().take(10) }
            .getOrNull()?.let { it to photos }
    }.groupBy({ it.first }, { it.second })

internal fun publicProfileWeekCounts(
    week: CalendarWeek, activities: Map<String, PublicProfileActivity>, checkIns: Map<String, List<List<String>>>,
): Map<String, Int> = week.dates.associateWith { date ->
    val activity = activities[date.take(7)]
    val day = date.takeLast(2).toInt()
    val count = activity?.checkInCounts?.get(day) ?: if (day in activity?.activeDays.orEmpty()) 1 else 0
    maxOf(count, checkIns[date].orEmpty().size)
}

@Composable
private fun PublicWeekPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    ProfileActivityWeek(CalendarWeek("2026-10-22"), mapOf("2026-10-19" to 1, "2026-10-21" to 3), emptyMap(), false, false, {}, {})
}

@Preview @Composable private fun PublicWeekLightPreview() = PublicWeekPreview(false)
@Preview @Composable private fun PublicWeekDarkPreview() = PublicWeekPreview(true)
