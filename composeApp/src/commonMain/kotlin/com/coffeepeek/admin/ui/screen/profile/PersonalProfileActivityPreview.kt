package com.coffeepeek.admin.ui.screen.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.coffeepeek.admin.di.platformViewModel
import com.coffeepeek.admin.feature.homecheckin.ui.HomeCheckInUiState
import com.coffeepeek.admin.feature.homecheckin.ui.HomeCheckInViewModel
import com.coffeepeek.admin.ui.screen.checkins.CalendarWeek
import com.coffeepeek.admin.ui.screen.checkins.calendarPhotos
import com.coffeepeek.admin.ui.screen.checkins.homeCalendarPhotos
import com.coffeepeek.admin.ui.screen.checkins.VisitedPlacesUiState
import com.coffeepeek.admin.ui.screen.checkins.VisitedPlacesViewModel
import com.coffeepeek.admin.utils.currentUtcIsoDateTime
import com.coffeepeek.admin.utils.utcIsoToLocalDate
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.domain.repository.SessionRepository
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

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
    val homePhotos = homeCalendarPhotos(home.calendarItems)
    val homeCounts = home.calendar.associate { it.preparedOn to it.brewCount }
    val counts = week.dates.associateWith { shop.calendarCheckIns[it].orEmpty().size + (homeCounts[it] ?: 0) }
    val photos = week.dates.associateWith { calendarPhotos(shop.calendarCheckIns[it].orEmpty(), homePhotos[it].orEmpty()) }
    ProfileActivityWeek(week, counts, photos, shop.isCalendarLoading || home.calendarLoading,
        shop.calendarError != null || home.calendarError != null, onClick, onRetry)
}

@Composable
private fun PersonalProfileWeekPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    val week = CalendarWeek("2026-10-22")
    PersonalProfileWeekContent(week, VisitedPlacesUiState(), HomeCheckInUiState(
        loading = false, month = com.coffeepeek.admin.feature.homecheckin.ui.HomeMonth(2026, 10),
        draft = com.coffeepeek.legacy.homecheckin.domain.HomeCheckInInput(preparedAt = "2026-10-22T09:00:00Z", preparedOn = week.today),
        calendar = listOf(com.coffeepeek.legacy.homecheckin.domain.HomeCalendarDay("2026-10-20", 2))), {}, {})
}

@Preview @Composable private fun PersonalProfileWeekLightPreview() = PersonalProfileWeekPreview(false)
@Preview @Composable private fun PersonalProfileWeekDarkPreview() = PersonalProfileWeekPreview(true)
