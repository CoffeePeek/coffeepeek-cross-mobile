package com.coffeepeek.admin.ui.screen.profile

import com.coffeepeek.admin.ui.screen.checkins.checkInCalendarLabels

import coffeepeek.composeapp.generated.resources.*
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.coffeepeek.core.designsystem.component.CoffeePeekTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.di.platformViewModel
import com.coffeepeek.admin.feature.homecheckin.ui.HomeCheckInUiState
import com.coffeepeek.admin.feature.homecheckin.ui.HomeCheckInViewModel
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.screen.checkins.CalendarCard
import com.coffeepeek.admin.ui.screen.checkins.CheckInDiaryRow
import com.coffeepeek.admin.ui.screen.checkins.calendarMonthFromIsoDate
import com.coffeepeek.admin.ui.screen.checkins.calendarWeekday
import com.coffeepeek.admin.ui.screen.checkins.selectedDateTitle
import com.coffeepeek.admin.ui.screen.checkins.toTimelineCheckIn
import com.coffeepeek.admin.ui.screen.checkins.visitCount
import com.coffeepeek.domain.model.CheckIn
import com.coffeepeek.legacy.homecheckin.domain.HomeCheckIn
import com.coffeepeek.admin.ui.screen.checkins.VisitedPlacesUiState
import com.coffeepeek.admin.ui.screen.checkins.VisitedPlacesViewModel
import com.coffeepeek.admin.ui.screen.checkins.homeCalendarPhotos
import com.coffeepeek.admin.utils.currentUtcIsoDateTime
import com.coffeepeek.admin.utils.utcIsoToLocalDate

/** Application composition joins personal shop and home activity without exposing their UI to profiles. */
@Composable
internal fun PersonalProfileActivityCalendar(
    shopVm: VisitedPlacesViewModel = platformViewModel(),
    homeVm: HomeCheckInViewModel = platformViewModel(),
    onOpenShop: (String) -> Unit = { Navigator.navigate(Navigator.Screen.ShopDetail(it)) },
    onOpenCheckIns: () -> Unit = { Navigator.navigate(Navigator.Screen.VisitedPlaces) },
) {
    val shop by shopVm.state.collectAsState()
    val home by homeVm.state.collectAsState()
    val today = utcIsoToLocalDate(currentUtcIsoDateTime())
    val month = shop.calendarMonth
    var opened by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(shopVm, homeVm) {
        if (opened) {
            shopVm.refreshCalendar()
            homeVm.loadCalendar()
        }
        opened = true
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
        onOpenShop = onOpenShop,
        onOpenCheckIns = onOpenCheckIns,
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
    onOpenShop: (String) -> Unit = {},
    onOpenCheckIns: () -> Unit = {},
) {
    val localizedHistoryHomeRetry = stringResource(Res.string.history_home_retry)

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
            TextButton(onClick = onRetry) { Text(localizedHistoryHomeRetry) }
        }
        if (!loading && error == null && shop.selectedDate != null) {
            SelectedDayCheckIns(
                date = shop.selectedDate,
                includeYear = month.year != calendarMonthFromIsoDate(today)?.year,
                shopCheckIns = shop.calendarCheckIns[shop.selectedDate].orEmpty(),
                homeCheckIns = if (homeMonthMatches) home.calendarItems.filter { it.preparedOn == shop.selectedDate } else emptyList(),
                onOpenShop = onOpenShop,
                onOpenCheckIns = onOpenCheckIns,
            )
        }
    }
}

/** What the tapped day holds: the coffee shop visits first, then the home brews, each opening its own place. */
@Composable
private fun SelectedDayCheckIns(
    date: String,
    includeYear: Boolean,
    shopCheckIns: List<CheckIn>,
    homeCheckIns: List<HomeCheckIn>,
    onOpenShop: (String) -> Unit,
    onOpenCheckIns: () -> Unit,
) {
    val labels = checkInCalendarLabels()

    val localizedLegacyProfileCheckInsScreenHistoryDayEmpty = stringResource(Res.string.legacy_profile_check_ins_screen_history_day_empty)

    val entries = remember(shopCheckIns, homeCheckIns) { selectedDayEntries(shopCheckIns, homeCheckIns) }
    Column(Modifier.fillMaxWidth().padding(top = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(selectedDateTitle(date, labels, includeYear), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("${calendarWeekday(date, labels.weekdays)} · ${visitCount(entries.size, labels)}", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (entries.isEmpty()) {
            Text(localizedLegacyProfileCheckInsScreenHistoryDayEmpty, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)) {
                Column {
                    entries.forEachIndexed { index, (checkIn, isHome) ->
                        CheckInDiaryRow(checkIn, onClick = { if (isHome || checkIn.shopId.isBlank()) onOpenCheckIns() else onOpenShop(checkIn.shopId) })
                        if (index < entries.lastIndex) HorizontalDivider(
                            modifier = Modifier.padding(start = 88.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        )
                    }
                }
            }
        }
    }
}

/** The coffee shop visits of the day by time, then the home brews by time; the flag marks a home brew (it opens the history, not a shop). */
internal fun selectedDayEntries(shopCheckIns: List<CheckIn>, homeCheckIns: List<HomeCheckIn>): List<Pair<CheckIn, Boolean>> =
    shopCheckIns.sortedBy { it.visitedAt.ifBlank { it.createdAt } }.map { it to false } +
        homeCheckIns.sortedBy { it.preparedAtUtc }.map { it.toTimelineCheckIn() to true }
