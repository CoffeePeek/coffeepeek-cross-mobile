package com.coffeepeek.admin.ui.screen.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekCard
import com.coffeepeek.admin.ui.screen.checkins.CalendarWeek
import com.coffeepeek.admin.ui.screen.checkins.CalendarPhoto
import com.coffeepeek.admin.ui.screen.checkins.calendarPhotos
import com.coffeepeek.admin.utils.CpImage
import com.coffeepeek.domain.repository.SessionRepository
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.pluralStringResource
import androidx.compose.ui.tooling.preview.Preview
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.profile_week_loading
import coffeepeek.composeapp.generated.resources.profile_week_error
import coffeepeek.composeapp.generated.resources.profile_week_retry
import coffeepeek.composeapp.generated.resources.profile_week_open
import coffeepeek.composeapp.generated.resources.profile_week_day
import coffeepeek.composeapp.generated.resources.profile_week_count
import coffeepeek.composeapp.generated.resources.profile_week_monday
import coffeepeek.composeapp.generated.resources.profile_week_tuesday
import coffeepeek.composeapp.generated.resources.profile_week_wednesday
import coffeepeek.composeapp.generated.resources.profile_week_thursday
import coffeepeek.composeapp.generated.resources.profile_week_friday
import coffeepeek.composeapp.generated.resources.profile_week_saturday
import coffeepeek.composeapp.generated.resources.profile_week_sunday

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.coffeepeek.admin.di.platformViewModel
import com.coffeepeek.admin.feature.homecheckin.ui.HomeCheckInUiState
import com.coffeepeek.admin.feature.homecheckin.ui.HomeCheckInViewModel
import com.coffeepeek.admin.ui.screen.checkins.VisitedPlacesUiState
import com.coffeepeek.admin.ui.screen.checkins.VisitedPlacesViewModel
import com.coffeepeek.admin.ui.screen.checkins.homeCalendarPhotos
import com.coffeepeek.admin.utils.currentUtcIsoDateTime
import com.coffeepeek.admin.utils.utcIsoToLocalDate

/** Independent week models keep the preview stable while the full calendar browses another month. */
@Composable
internal fun PersonalProfileActivityPreview(onClick: () -> Unit) {
    val sessions = koinInject<SessionRepository>()
    val session by sessions.observeSession().collectAsState(sessions.peekSession())
    if (!sessions.isActiveSession(session)) return
    val today = utcIsoToLocalDate(currentUtcIsoDateTime())
    val week = remember(today) { CalendarWeek(today) }
    val account = session?.userId ?: session?.accessToken
    val shopVm: VisitedPlacesViewModel = platformViewModel(key = "profile-week-shops-$account-$today", parameters = { parametersOf(week) })
    val homeVm: HomeCheckInViewModel = platformViewModel(key = "profile-week-home-$account-$today", parameters = { parametersOf(week) })
    val shop by shopVm.state.collectAsState()
    val home by homeVm.state.collectAsState()
    var opened by rememberSaveable(account, today) { mutableStateOf(false) }
    LaunchedEffect(shopVm, homeVm) {
        if (opened) { shopVm.refreshCalendar(); homeVm.loadCalendar() }
        opened = true
    }
    PersonalProfileWeekContent(week, shop, home, onClick,
        onRetry = { shopVm.refreshCalendar(); homeVm.loadCalendar() })
}

@Composable
private fun PersonalProfileWeekContent(week: CalendarWeek, shop: VisitedPlacesUiState,
    home: HomeCheckInUiState, onClick: () -> Unit, onRetry: () -> Unit) {
    val loading = shop.isCalendarLoading || home.calendarLoading
    val failed = shop.calendarError != null || home.calendarError != null
    val homePhotos = homeCalendarPhotos(home.calendarItems)
    val homeCounts = home.calendar.associate { it.preparedOn to it.brewCount }
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
                val count = shop.calendarCheckIns[date].orEmpty().size + (homeCounts[date] ?: 0)
                PersonalProfileWeekDay(date, stringResource(labels[index]), count,
                    calendarPhotos(shop.calendarCheckIns[date].orEmpty(), homePhotos[date].orEmpty()),
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
private fun PersonalProfileWeekDay(date: String, label: String, count: Int, photos: List<CalendarPhoto>,
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
private fun PersonalProfileWeekPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    val week = CalendarWeek("2026-10-22")
    PersonalProfileWeekContent(week, VisitedPlacesUiState(), HomeCheckInUiState(
        loading = false, month = com.coffeepeek.admin.feature.homecheckin.ui.HomeMonth(2026, 10),
        draft = com.coffeepeek.feature.homecheckin.domain.HomeCheckInInput(preparedAt = "2026-10-22T09:00:00Z", preparedOn = week.today),
        calendar = listOf(com.coffeepeek.feature.homecheckin.domain.HomeCalendarDay("2026-10-20", 2))), {}, {})
}

@Preview @Composable private fun PersonalProfileWeekLightPreview() = PersonalProfileWeekPreview(false)
@Preview @Composable private fun PersonalProfileWeekDarkPreview() = PersonalProfileWeekPreview(true)
