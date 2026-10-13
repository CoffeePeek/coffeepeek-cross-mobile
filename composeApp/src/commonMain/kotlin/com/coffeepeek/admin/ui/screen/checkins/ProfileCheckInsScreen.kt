package com.coffeepeek.admin.ui.screen.checkins

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.di.platformViewModel
import com.coffeepeek.admin.feature.homecheckin.ui.HomeCheckInViewModel
import com.coffeepeek.admin.feature.homecheckin.ui.HomeDetail
import com.coffeepeek.admin.feature.homecheckin.ui.HomeEditor
import com.coffeepeek.admin.feature.homecheckin.ui.HomeTimelineCard
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.CapsuleSegmentedControl
import com.coffeepeek.admin.ui.component.CheckInDisplayCard
import com.coffeepeek.admin.ui.component.CoffeePeekLoader
import com.coffeepeek.admin.ui.component.CpTopBar
import com.coffeepeek.admin.ui.component.FullScreenImageDialog
import com.coffeepeek.admin.ui.component.PlatformAlert
import com.coffeepeek.admin.ui.component.PlatformScrollableColumn
import com.coffeepeek.admin.ui.component.SwipeDismissModalBottomSheet
import com.coffeepeek.admin.ui.icons.CpIcons
import com.coffeepeek.admin.utils.currentUtcIsoDateTime
import com.coffeepeek.admin.utils.utcIsoToLocalDate
import com.coffeepeek.admin.utils.utcIsoToLocalDateTime
import com.coffeepeek.domain.model.CheckIn
import com.coffeepeek.feature.homecheckin.domain.HomeCheckIn

private enum class ActivityKind { All, Shop, Home }

private sealed interface ActivityEntry {
    val date: String
    val timestamp: String
    val key: String

    data class Shop(val value: CheckIn) : ActivityEntry {
        override val date = utcIsoToLocalDate(value.visitedAt.ifBlank { value.createdAt })
        override val timestamp = value.visitedAt.ifBlank { value.createdAt }
        override val key = "shop:${value.id}"
    }

    data class Home(val value: HomeCheckIn) : ActivityEntry {
        override val date = value.preparedOn
        override val timestamp = value.preparedAtUtc
        override val key = "home:${value.id}"
    }
}

/** Profile-owned activity: both kinds share one timeline and one date selection. */
@Composable
internal fun ProfileCheckInsScreen(
    initialHomeId: String? = null,
    shopVm: VisitedPlacesViewModel = platformViewModel(),
    homeVm: HomeCheckInViewModel = platformViewModel(),
) {
    val shop by shopVm.state.collectAsState()
    val home by homeVm.state.collectAsState()
    val today = remember { utcIsoToLocalDate(currentUtcIsoDateTime()) }
    var selectedDate by remember { mutableStateOf<String?>(null) }
    var kind by remember { mutableStateOf(ActivityKind.All) }
    var showCreate by remember { mutableStateOf(false) }
    var selectedShop by remember { mutableStateOf<CheckIn?>(null) }
    var photoPreview by remember { mutableStateOf<Pair<List<String>, Int>?>(null) }

    LaunchedEffect(Unit) { shopVm.load(reset = true) }
    LaunchedEffect(initialHomeId) { initialHomeId?.let { homeVm.open(it, editOnLoad = true) } }

    fun syncMonth(date: String) {
        val month = calendarMonthFromIsoDate(date) ?: return
        val shopOffset = (month.year - shop.calendarMonth.year) * 12 + month.month - shop.calendarMonth.month
        if (shopOffset != 0) shopVm.changeMonth(shopOffset)
        val homeOffset = (month.year - home.month.year) * 12 + month.month - home.month.month
        if (homeOffset != 0) homeVm.changeMonth(homeOffset)
    }

    fun chooseDate(date: String?) {
        val next = date.takeUnless { it == selectedDate }
        selectedDate = next
        if (next != null) syncMonth(next)
        if (home.selectedDate != next) homeVm.selectDate(next)
    }

    fun moveMonth(offset: Int) {
        val next = shop.calendarMonth.plusMonths(offset)
        if (next.isoDate(1) > today) return
        if (selectedDate != null) chooseDate(null)
        syncMonth(next.isoDate(1))
    }

    val shopEntries = when {
        kind == ActivityKind.Home -> emptyList()
        selectedDate != null && shop.isCalendarLoading -> emptyList()
        selectedDate != null -> shop.calendarCheckIns[selectedDate].orEmpty()
        else -> shop.checkIns
    }
    val homeEntries = when {
        kind == ActivityKind.Shop -> emptyList()
        home.loading -> emptyList()
        home.selectedDate != selectedDate -> emptyList()
        else -> home.items
    }
    val entries = remember(shopEntries, homeEntries) {
        (shopEntries.map(ActivityEntry::Shop) + homeEntries.map(ActivityEntry::Home))
            .sortedWith(compareByDescending<ActivityEntry> { it.date }.thenByDescending { it.timestamp })
    }
    val isLoading = (kind != ActivityKind.Home && (if (selectedDate != null) shop.isCalendarLoading else shop.isLoading)) ||
        (kind != ActivityKind.Shop && home.loading)
    val hasMore = (selectedDate == null && kind != ActivityKind.Home && shop.hasMore) ||
        (kind != ActivityKind.Shop && home.hasMore)

    if (home.deleteConfirmation) PlatformAlert(
        title = "Удалить домашнюю запись?",
        message = "Она исчезнет из истории чекинов.",
        confirmLabel = "Удалить", onConfirm = homeVm::delete,
        onDismiss = homeVm::cancelDelete, cancelLabel = "Отмена", destructive = true,
    )
    photoPreview?.let { (urls, index) ->
        FullScreenImageDialog(urls, index) { photoPreview = null }
    }
    selectedShop?.let { checkIn ->
        SwipeDismissModalBottomSheet(onDismissRequest = { selectedShop = null }) {
            PlatformScrollableColumn(Modifier.fillMaxWidth().weight(1f)) {
                CheckInDisplayCard(checkIn, fullVersion = true,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    onPhotoClick = { urls, index -> photoPreview = urls to index })
                if (checkIn.shopId.isNotBlank()) TextButton(onClick = {
                    selectedShop = null
                    Navigator.navigate(Navigator.Screen.ShopDetail(checkIn.shopId))
                }, modifier = Modifier.fillMaxWidth().padding(16.dp)) { Text("Открыть кофейню") }
            }
        }
    }
    if (!showCreate && (home.editing || home.selected != null)) {
        SwipeDismissModalBottomSheet(
            onDismissRequest = { if (home.editing) homeVm.closeEditor() else homeVm.closeDetail() },
            dismissEnabled = !home.saving,
        ) {
            if (home.editing) HomeEditor(home, homeVm, Modifier.fillMaxWidth().heightIn(max = 680.dp))
            else home.selected?.let { entry ->
                HomeDetail(entry, homeVm, Modifier.fillMaxWidth().heightIn(max = 680.dp), notice = home.saveNotice) { urls, index ->
                    photoPreview = urls to index
                }
            }
        }
    }
    if (showCreate) CheckInCreateFlow(
        onDismiss = { showCreate = false },
        onShop = { showCreate = false; Navigator.navigate(Navigator.Screen.CreateCheckIn) },
        homeVm = homeVm,
    )

    Scaffold(
        topBar = { CpTopBar("Чекины") },
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            CheckInCreateButton(onClick = { showCreate = true })
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            CapsuleSegmentedControl(ActivityKind.entries, kind,
                label = { when (it) { ActivityKind.All -> "Все"; ActivityKind.Shop -> "В кофейне"; ActivityKind.Home -> "Дома" } },
                onSelected = { kind = it }, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item(key = "calendar") {
                    CalendarCard(
                        state = shop.copy(
                            selectedDate = selectedDate,
                            calendarCheckIns = if (kind == ActivityKind.Home) emptyMap() else shop.calendarCheckIns,
                            isCalendarLoading = (kind != ActivityKind.Home && shop.isCalendarLoading) ||
                                (kind != ActivityKind.Shop && home.calendarLoading),
                            calendarError = (if (kind != ActivityKind.Home) shop.calendarError else null) ?:
                                (if (kind != ActivityKind.Shop) home.calendarError else null),
                        ),
                        today = today,
                        onPreviousMonth = { moveMonth(-1) }, onNextMonth = { moveMonth(1) },
                        onSelectDate = { chooseDate(it) },
                        onToday = { syncMonth(today); if (selectedDate != today) chooseDate(today) },
                        homeCounts = if (kind == ActivityKind.Shop) emptyMap()
                            else home.calendar.associate { it.preparedOn to it.brewCount },
                    )
                }
                if (selectedDate != null) item(key = "clear-date") {
                    TextButton(onClick = { chooseDate(null) }) { Text("Показать все даты") }
                }
                if (isLoading && entries.isEmpty()) item { Box(Modifier.fillMaxWidth().height(90.dp),
                    contentAlignment = Alignment.Center) { CoffeePeekLoader() } }
                if (!isLoading && entries.isEmpty()) item {
                    Text(if (selectedDate != null) "В этот день чекинов не было" else "Пока нет чекинов",
                        modifier = Modifier.padding(vertical = 32.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                itemsIndexed(entries, key = { _, entry -> entry.key }) { index, entry ->
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (index == 0 || entries[index - 1].date != entry.date) Text(
                            selectedDateTitle(entry.date, includeYear = entry.date.take(4) != today.take(4)),
                            style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
                        TimelineRail(entry is ActivityEntry.Home) {
                            when (entry) {
                                is ActivityEntry.Home -> HomeTimelineCard(entry.value) { homeVm.open(entry.value.id) }
                                is ActivityEntry.Shop -> ShopTimelineCard(entry.value) { selectedShop = entry.value }
                            }
                        }
                    }
                }
                if (home.detailLoading) item { CoffeePeekLoader() }
                if (kind != ActivityKind.Home && shop.error != null) item {
                    RetryMessage(shop.error.orEmpty(), shopVm::refresh)
                }
                if (kind != ActivityKind.Home && shop.calendarError != null) item {
                    RetryMessage(shop.calendarError.orEmpty(), shopVm::refreshCalendar)
                }
                if (kind != ActivityKind.Shop && home.calendarError != null) item {
                    RetryMessage(home.calendarError.orEmpty(), homeVm::loadCalendar)
                }
                if (kind != ActivityKind.Shop && home.error != null) item {
                    RetryMessage(home.error.orEmpty(), homeVm::refresh)
                }
                if (hasMore) item {
                    TextButton(onClick = {
                        if (selectedDate == null && kind != ActivityKind.Home && shop.hasMore) shopVm.loadMore()
                        if (kind != ActivityKind.Shop && home.hasMore) homeVm.loadMore()
                    }, modifier = Modifier.fillMaxWidth(), enabled = !shop.isLoadingMore && !home.loadingMore) {
                        Text(if (shop.isLoadingMore || home.loadingMore) "Загрузка…" else "Показать ещё")
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineRail(isHome: Boolean, content: @Composable () -> Unit) {
    val color = if (isHome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    Box(Modifier.fillMaxWidth()) {
        Canvas(Modifier.matchParentSize()) {
            val x = 12.dp.toPx()
            drawLine(color.copy(alpha = 0.6f), start = androidx.compose.ui.geometry.Offset(x, 0f),
                end = androidx.compose.ui.geometry.Offset(x, size.height), strokeWidth = 2.dp.toPx())
            drawCircle(color, radius = 6.dp.toPx(), center = androidx.compose.ui.geometry.Offset(x, 28.dp.toPx()))
        }
        Box(Modifier.fillMaxWidth().padding(start = 32.dp)) { content() }
    }
}

@Composable
private fun ShopTimelineCard(entry: CheckIn, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Icon(CpIcons.Location, contentDescription = null, modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("В кофейне · ${utcIsoToLocalDateTime(entry.visitedAt.ifBlank { entry.createdAt }).substringAfterLast(' ', "")}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(entry.shopName.ifBlank { "Кофейня" }, style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold)
            val drink = entry.drinkNameRu ?: entry.customDrinkName ?: entry.drinkNameEn
            if (!drink.isNullOrBlank()) Text(drink, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary)
            if (entry.note.isNotBlank()) Text(entry.note, style = MaterialTheme.typography.bodyMedium,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun RetryMessage(message: String, onRetry: () -> Unit) {
    Column {
        Text(message, color = MaterialTheme.colorScheme.error)
        TextButton(onClick = onRetry) { Text("Повторить") }
    }
}
