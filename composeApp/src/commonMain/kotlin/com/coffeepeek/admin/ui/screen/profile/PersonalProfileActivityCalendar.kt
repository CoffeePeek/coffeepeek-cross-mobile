package com.coffeepeek.admin.ui.screen.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.coffeepeek.admin.feature.homecheckin.ui.HomeCheckInUiState
import com.coffeepeek.admin.feature.homecheckin.ui.HomeCheckInViewModel
import com.coffeepeek.admin.ui.screen.checkins.CalendarCard
import com.coffeepeek.admin.ui.screen.checkins.VisitedPlacesUiState
import com.coffeepeek.admin.ui.screen.checkins.VisitedPlacesViewModel
import com.coffeepeek.admin.ui.screen.checkins.homeCalendarPhotos
import com.coffeepeek.admin.utils.currentUtcIsoDateTime
import com.coffeepeek.admin.utils.utcIsoToLocalDate
import org.koin.compose.koinInject

/** Application composition joins personal shop and home activity without exposing their UI to profiles. */
@Composable
internal fun PersonalProfileActivityCalendar(
    shopVm: VisitedPlacesViewModel = koinInject(),
    homeVm: HomeCheckInViewModel = koinInject(),
) {
    val shop by shopVm.state.collectAsState()
    val home by homeVm.state.collectAsState()
    val today = utcIsoToLocalDate(currentUtcIsoDateTime())
    val month = shop.calendarMonth

    DisposableEffect(shopVm, homeVm) {
        onDispose {
            shopVm.close()
            homeVm.close()
        }
    }
    LaunchedEffect(month) {
        val offset = (month.year - home.month.year) * 12 + month.month - home.month.month
        if (offset != 0) homeVm.changeMonth(offset)
    }

    PersonalProfileActivityCalendarContent(
        shop = shop,
        home = home,
        today = today,
        onPreviousMonth = { shopVm.changeMonth(-1) },
        onNextMonth = { shopVm.changeMonth(1) },
        onSelectDate = shopVm::selectDate,
        onToday = shopVm::showToday,
        onRetry = {
            shopVm.refreshCalendar()
            homeVm.loadCalendar()
        },
    )
}

@Composable
internal fun PersonalProfileActivityCalendarContent(
    shop: VisitedPlacesUiState,
    home: HomeCheckInUiState,
    today: String,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDate: (String) -> Unit,
    onToday: () -> Unit,
    onRetry: () -> Unit,
) {
    val month = shop.calendarMonth
    val homeMonthMatches = home.month.year == month.year && home.month.month == month.month
    val loading = shop.isCalendarLoading || !homeMonthMatches || home.calendarLoading
    val error = shop.calendarError ?: home.calendarError.takeIf { homeMonthMatches }
    Column(Modifier.fillMaxWidth()) {
        CalendarCard(
            state = shop.copy(isCalendarLoading = loading, calendarError = error),
            today = today,
            onPreviousMonth = onPreviousMonth,
            onNextMonth = onNextMonth,
            onSelectDate = onSelectDate,
            onToday = onToday,
            homeCounts = if (homeMonthMatches) home.calendar.associate { it.preparedOn to it.brewCount } else emptyMap(),
            homePhotos = if (homeMonthMatches) homeCalendarPhotos(home.calendarItems) else emptyMap(),
            framed = false,
        )
        if (error != null && !loading) {
            TextButton(onClick = onRetry) { Text("Повторить") }
        }
    }
}
