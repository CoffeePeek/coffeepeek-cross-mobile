package com.coffeepeek.admin.ui.screen.checkins

import coffeepeek.composeapp.generated.resources.*
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

import com.coffeepeek.core.designsystem.component.coffeePeekScrollContentPadding

import com.coffeepeek.core.designsystem.component.CoffeePeekScaffold as Scaffold

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import com.coffeepeek.core.designsystem.component.CoffeePeekFilledButton as Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.coffeepeek.core.designsystem.component.CoffeePeekTextButton as TextButton
import com.coffeepeek.admin.ui.component.pluralCountLabel
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.di.platformViewModel
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.CapsuleSegmentedControl
import com.coffeepeek.admin.ui.component.CheckInDisplayCard
import com.coffeepeek.admin.ui.component.CoffeePeekLoader
import com.coffeepeek.admin.ui.component.CpTopBar
import com.coffeepeek.admin.ui.component.FullScreenImageDialog
import com.coffeepeek.admin.ui.component.PhotoViewerImages
import com.coffeepeek.admin.ui.component.PlatformScrollableColumn
import com.coffeepeek.admin.ui.component.SwipeDismissModalBottomSheet
import com.coffeepeek.admin.utils.currentUtcIsoDateTime
import com.coffeepeek.admin.utils.utcIsoToLocalDate
import com.coffeepeek.domain.model.CheckIn

@Composable
fun VisitedPlacesScreen(vm: VisitedPlacesViewModel = platformViewModel()) {
    val localizedCheckinSheetTitle = stringResource(Res.string.checkin_sheet_title)
    val localizedLegacyProfileCheckInsScreenContributionsRequestOpenShop = stringResource(Res.string.legacy_profile_check_ins_screen_contributions_request_open_shop)
    val localizedProfileStatCheckIns = stringResource(Res.string.profile_stat_check_ins)
    val localizedLegacyVisitedPlacesScreenCalendar = stringResource(Res.string.legacy_visited_places_screen_calendar)
    val localizedLegacyVisitedPlacesScreenSandboxFeedTitle = stringResource(Res.string.legacy_visited_places_screen_sandbox_feed_title)

    val state by vm.state.collectAsState()
    var viewMode by rememberSaveable { mutableStateOf(CheckInViewMode.Calendar) }
    var photoPreview by remember { mutableStateOf<Pair<PhotoViewerImages, Int>?>(null) }
    var selectedCheckIn by remember { mutableStateOf<CheckIn?>(null) }

    selectedCheckIn?.let { checkIn ->
        SwipeDismissModalBottomSheet(onDismissRequest = { selectedCheckIn = null }) {
            PlatformScrollableColumn(Modifier.fillMaxWidth().weight(1f, fill = false)) {
                Text(localizedCheckinSheetTitle, style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp))
                CheckInDisplayCard(
                    checkIn = checkIn, fullVersion = true,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    onPhotoClick = { urls, index -> photoPreview = urls to index },
                )
                if (checkIn.shopId.isNotBlank()) TextButton(
                    onClick = {
                        selectedCheckIn = null
                        Navigator.navigate(Navigator.Screen.ShopDetail(checkIn.shopId))
                    },
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                ) { Text(localizedLegacyProfileCheckInsScreenContributionsRequestOpenShop) }
            }
        }
    }

    photoPreview?.let { (urls, index) ->
        FullScreenImageDialog(
            images = urls,
            initialIndex = index,
            onDismiss = { photoPreview = null },
        )
    }

    Scaffold(
        topBar = { CpTopBar(localizedProfileStatCheckIns) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            CapsuleSegmentedControl(
                options = CheckInViewMode.entries,
                selected = viewMode,
                label = { if (it == CheckInViewMode.Calendar) localizedLegacyVisitedPlacesScreenCalendar else localizedLegacyVisitedPlacesScreenSandboxFeedTitle },
                onSelected = { selected ->
                    viewMode = selected
                    if (selected == CheckInViewMode.List && state.checkIns.isEmpty() && !state.isLoading) {
                        vm.load(reset = true)
                    }
                },
                modifier = Modifier.padding(horizontal = CpDimens.spacing4, vertical = CpDimens.spacing2),
            )

            when (viewMode) {
                CheckInViewMode.Calendar -> CheckInCalendarContent(
                    state = state,
                    onPreviousMonth = { vm.changeMonth(-1) },
                    onNextMonth = { vm.changeMonth(1) },
                    onSelectDate = vm::selectDate,
                    onToday = vm::showToday,
                    onRetry = vm::refreshCalendar,
                    onCheckInClick = { selectedCheckIn = it },
                )
                CheckInViewMode.List -> CheckInListContent(
                    state = state,
                    vm = vm,
                    onPhotoClick = { urls, index -> photoPreview = urls to index },
                )
            }
        }
    }
}

@Composable
internal fun CheckInCalendarContent(
    state: VisitedPlacesUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDate: (String) -> Unit,
    onToday: () -> Unit,
    onRetry: () -> Unit,
    onCheckInClick: (CheckIn) -> Unit,
    today: String = utcIsoToLocalDate(currentUtcIsoDateTime()),
) {
    val labels = checkInCalendarLabels()

    val localizedLegacyProfileCheckInsScreenHistoryDayEmpty = stringResource(Res.string.legacy_profile_check_ins_screen_history_day_empty)
    val localizedLegacyVisitedPlacesScreenHistoryMonthEmpty = stringResource(Res.string.legacy_visited_places_screen_history_month_empty)

    val selectedCheckIns = remember(state.calendarCheckIns, state.selectedDate) {
        state.selectedDate?.let(state.calendarCheckIns::get).orEmpty()
            .sortedBy { it.visitedAt.ifBlank { it.createdAt } }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = coffeePeekScrollContentPadding(PaddingValues(horizontal = 16.dp, vertical = 12.dp)),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            CalendarCard(
                state = state,
                today = today,
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth,
                onSelectDate = onSelectDate,
                onToday = onToday,
            )
        }

        if (state.isCalendarLoading) {
            item {
                Box(
                    Modifier.fillMaxWidth().height(72.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CoffeePeekLoader(size = CpDimens.loaderButton, strokeWidth = 2.dp)
                }
            }
        } else if (state.calendarError != null) {
            item { ErrorMessage(state.calendarError, onRetry) }
        } else if (state.selectedDate != null) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = selectedDateTitle(state.selectedDate, labels, includeYear = state.calendarMonth.year != calendarMonthFromIsoDate(today)?.year),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text("${calendarWeekday(state.selectedDate, labels.weekdays)} · ${visitCount(selectedCheckIns.size, labels)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (selectedCheckIns.isEmpty()) {
                item {
                    Text(
                        text = localizedLegacyProfileCheckInsScreenHistoryDayEmpty,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                item {
                    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
                        Column {
                            selectedCheckIns.forEachIndexed { index, checkIn ->
                                CheckInDiaryRow(checkIn, onClick = { onCheckInClick(checkIn) })
                                if (index < selectedCheckIns.lastIndex) HorizontalDivider(
                                    modifier = Modifier.padding(start = 88.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                )
                            }
                        }
                    }
                }
            }
        } else {
            item {
                Text(
                    text = localizedLegacyVisitedPlacesScreenHistoryMonthEmpty,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CheckInListContent(
    state: VisitedPlacesUiState,
    vm: VisitedPlacesViewModel,
    onPhotoClick: (PhotoViewerImages, Int) -> Unit,
) {
    val localizedProfileCheckinsEmpty = stringResource(Res.string.profile_checkins_empty)

    val listState = rememberLazyListState()
    val shouldLoadMore by remember(state.checkIns.size, state.hasMore, state.isLoadingMore) {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= state.checkIns.size - 3 && state.hasMore && !state.isLoadingMore
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) vm.loadMore()
    }

    when {
        state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CoffeePeekLoader()
        }
        state.error != null && state.checkIns.isEmpty() -> ErrorMessage(state.error, vm::refresh)
        state.checkIns.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(localizedProfileCheckinsEmpty, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = coffeePeekScrollContentPadding(PaddingValues(CpDimens.spacing4)),
            verticalArrangement = Arrangement.spacedBy(CpDimens.spacing3),
        ) {
            items(state.checkIns, key = { it.id }) { checkIn ->
                CheckInDisplayCard(
                    checkIn = checkIn,
                    onClick = { Navigator.navigate(Navigator.Screen.ShopDetail(checkIn.shopId)) },
                    onPhotoClick = onPhotoClick,
                )
            }
        }
    }
}

@Composable
private fun ErrorMessage(message: String?, onRetry: () -> Unit) {
    val localizedLegacyAddShopScreenSandboxCitiesFailure = stringResource(Res.string.legacy_add_shop_screen_sandbox_cities_failure)
    val localizedRetry = stringResource(Res.string.retry)

    Column(
        modifier = Modifier.fillMaxWidth().padding(CpDimens.spacing4),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(message ?: localizedLegacyAddShopScreenSandboxCitiesFailure, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(
            onClick = onRetry,
            modifier = Modifier.padding(top = CpDimens.spacing3).height(CpDimens.buttonHeight),
            shape = RoundedCornerShape(percent = 50),
        ) {
            Text(localizedRetry)
        }
    }
}

internal fun monthTitle(month: CalendarMonth, labels: CheckInCalendarLabels) = "${labels.months[month.month - 1]} ${month.year}"

internal fun selectedDateTitle(date: String, labels: CheckInCalendarLabels, includeYear: Boolean = true): String {
    val parts = date.split('-')
    val year = parts.getOrNull(0)?.toIntOrNull() ?: return date
    val month = parts.getOrNull(1)?.toIntOrNull()?.takeIf { it in 1..12 } ?: return date
    val day = parts.getOrNull(2)?.toIntOrNull() ?: return date
    return "$day ${labels.genitiveMonths[month - 1]}" + if (includeYear) " $year" else ""
}

internal fun visitCount(count: Int, labels: CheckInCalendarLabels) = pluralCountLabel(count, labels.checkIns)
internal fun shopCount(count: Int, labels: CheckInCalendarLabels) = pluralCountLabel(count, labels.shops)
