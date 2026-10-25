@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.coffeepeek.feature.profile.impl.ui.compose.component

import androidx.compose.runtime.*
import com.coffeepeek.core.designsystem.component.CoffeePeekActivityDay
import com.coffeepeek.core.designsystem.component.CoffeePeekActivityWeek
import com.coffeepeek.core.designsystem.component.CoffeePeekActivityWeekLabels
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.profile.api.ProfileImage
import com.coffeepeek.feature.profile.domain.PublicProfileActivity
import com.coffeepeek.feature.profile.domain.PublicProfileCheckIn
import com.coffeepeek.feature.profile.impl.resources.*
import com.coffeepeek.feature.profile.impl.ui.data.PublicProfileWeekDay
import com.coffeepeek.feature.profile.impl.ui.data.publicProfileDate
import com.coffeepeek.feature.profile.impl.ui.data.publicProfileWeek
import com.coffeepeek.feature.profile.impl.ui.data.publicProfileWeekDates
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.time.Clock

/** The public profile owns its presentation; only image transport comes from composition. */
@Composable
internal fun PublicProfileActivityPreview(
    slug: String, activity: PublicProfileActivity?, checkIns: List<PublicProfileCheckIn>,
    loading: Boolean, failed: Boolean, image: ProfileImage, onRetry: () -> Unit, onOpen: () -> Unit,
) {
    val today = requireNotNull(publicProfileDate(Clock.System.now().toString()))
    val dates = remember(today) { publicProfileWeekDates(today) }
    var activities by remember(slug, today) { mutableStateOf(emptyMap<String, PublicProfileActivity>()) }
    LaunchedEffect(activity, dates) {
        if (activity != null && dates.any { it.startsWith(activity.month) }) {
            activities = activities + (activity.month to activity)
        }
    }
    PublicProfileWeekContent(today, publicProfileWeek(dates, activities, checkIns),
        loading && activities.isEmpty(), failed, image, onRetry, onOpen)
}

@Composable
private fun PublicProfileWeekContent(
    today: String, days: List<PublicProfileWeekDay>, loading: Boolean, failed: Boolean,
    image: ProfileImage, onRetry: () -> Unit, onOpen: () -> Unit,
) {
    val weekdays = stringArrayResource(Res.array.profile_weekdays)
    val cells = days.mapIndexed { index, day ->
        CoffeePeekActivityDay(weekdays[index], stringResource(Res.string.profile_week_day, day.date,
            pluralStringResource(Res.plurals.profile_week_count, day.count, day.count)),
            day.date == today, day.count > 0, day.photos.size)
    }
    val labels = CoffeePeekActivityWeekLabels(stringResource(Res.string.profile_week_open),
        stringResource(Res.string.profile_week_loading), stringResource(Res.string.profile_week_error),
        stringResource(Res.string.profile_retry))
    CoffeePeekActivityWeek(cells, labels, loading, failed, onOpen, onRetry) { index, modifier ->
        days[index].photos.firstOrNull()?.let { image(it, null, modifier) }
    }
}

@Composable
private fun PublicProfileWeekPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    val days = publicProfileWeekDates("2026-10-25").mapIndexed { index, date ->
        PublicProfileWeekDay(date, if (index == 1) 3 else 0, emptyList())
    }
    PublicProfileWeekContent("2026-10-25", days, false, false, { _, _, _ -> }, {}, {})
}

@Preview @Composable private fun PublicProfileWeekLightPreview() = PublicProfileWeekPreview(false)
@Preview @Composable private fun PublicProfileWeekDarkPreview() = PublicProfileWeekPreview(true)
