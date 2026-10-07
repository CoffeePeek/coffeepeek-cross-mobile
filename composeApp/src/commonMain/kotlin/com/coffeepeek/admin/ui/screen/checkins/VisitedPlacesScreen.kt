package com.coffeepeek.admin.ui.screen.checkins

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.di.platformViewModel
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.CapsuleSegmentedControl
import com.coffeepeek.admin.ui.component.CheckInDisplayCard
import com.coffeepeek.admin.ui.component.CoffeePeekLoader
import com.coffeepeek.admin.ui.component.CpTopBar
import com.coffeepeek.admin.ui.component.FullScreenImageDialog
import com.coffeepeek.admin.ui.icons.CpIcons
import com.coffeepeek.admin.utils.CpImage
import com.coffeepeek.admin.utils.currentUtcIsoDateTime
import com.coffeepeek.admin.utils.utcIsoToLocalDate
import com.coffeepeek.domain.model.CheckIn
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.maskot_with_photo
import org.jetbrains.compose.resources.painterResource

@Composable
fun VisitedPlacesScreen(vm: VisitedPlacesViewModel = platformViewModel()) {
    val state by vm.state.collectAsState()
    var viewMode by rememberSaveable { mutableStateOf(CheckInViewMode.Calendar) }
    var photoPreview by remember { mutableStateOf<Pair<List<String>, Int>?>(null) }

    photoPreview?.let { (urls, index) ->
        FullScreenImageDialog(
            imageUrls = urls,
            initialIndex = index,
            onDismiss = { photoPreview = null },
        )
    }

    Scaffold(
        topBar = { CpTopBar("Чекины") },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            CapsuleSegmentedControl(
                options = CheckInViewMode.entries,
                selected = viewMode,
                label = { if (it == CheckInViewMode.Calendar) "Календарь" else "Лента" },
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
                    vm = vm,
                    onPhotoClick = { urls, index -> photoPreview = urls to index },
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
private fun CheckInCalendarContent(
    state: VisitedPlacesUiState,
    vm: VisitedPlacesViewModel,
    onPhotoClick: (List<String>, Int) -> Unit,
) {
    val selectedCheckIns = state.selectedDate?.let(state.calendarCheckIns::get).orEmpty()
    val today = remember { utcIsoToLocalDate(currentUtcIsoDateTime()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(CpDimens.spacing4),
        verticalArrangement = Arrangement.spacedBy(CpDimens.spacing3),
    ) {
        item {
            CalendarCard(
                state = state,
                today = today,
                onPreviousMonth = { vm.changeMonth(-1) },
                onNextMonth = { vm.changeMonth(1) },
                onSelectDate = vm::selectDate,
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
            item { ErrorMessage(state.calendarError, vm::refreshCalendar) }
        } else if (state.selectedDate != null) {
            item {
                Text(
                    text = selectedDateTitle(state.selectedDate),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            if (selectedCheckIns.isEmpty()) {
                item {
                    Text(
                        text = "В этот день чекинов не было",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(selectedCheckIns, key = { it.id }) { checkIn ->
                    CheckInDisplayCard(
                        checkIn = checkIn,
                        onClick = { Navigator.navigate(Navigator.Screen.ShopDetail(checkIn.shopId)) },
                        onPhotoClick = onPhotoClick,
                    )
                }
            }
        } else {
            item {
                Text(
                    text = "В этом месяце пока нет чекинов",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CalendarCard(
    state: VisitedPlacesUiState,
    today: String,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDate: (String) -> Unit,
) {
    val month = state.calendarMonth
    val total = state.calendarCheckIns.values.sumOf { it.size }
    val shops = state.calendarCheckIns.values.flatten().map(CheckIn::shopId).filter(String::isNotBlank).distinct().size
    val checkInDates = state.calendarCheckIns.filterValues { it.isNotEmpty() }.keys

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CpDimens.radius2xl),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = CpDimens.spacing2, vertical = CpDimens.spacing3),
            verticalArrangement = Arrangement.spacedBy(CpDimens.spacing1),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = onPreviousMonth) {
                    Icon(CpIcons.ChevronLeft, contentDescription = "Предыдущий месяц")
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = monthTitle(month),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "${visitCount(total)} · ${shopCount(shops)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onNextMonth, enabled = state.canGoNextMonth) {
                    Icon(CpIcons.ChevronRight, contentDescription = "Следующий месяц")
                }
            }

            Row(Modifier.fillMaxWidth()) {
                listOf("П", "В", "С", "Ч", "П", "С", "В").forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            calendarCells(month).chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth()) {
                    week.forEachIndexed { dayOfWeek, day ->
                        if (day == null) {
                            Spacer(Modifier.weight(1f).height(52.dp))
                        } else {
                            val date = month.isoDate(day)
                            val streak = checkInStreak(month, day, checkInDates)
                            CalendarDay(
                                day = day,
                                date = date,
                                checkIns = state.calendarCheckIns[date].orEmpty(),
                                selected = state.selectedDate == date,
                                today = today == date,
                                inStreak = streak.isPartOfStreak,
                                connectsPrevious = streak.hasPreviousDay && dayOfWeek > 0,
                                connectsNext = streak.hasNextDay && dayOfWeek < 6,
                                onClick = { onSelectDate(date) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDay(
    day: Int,
    date: String,
    checkIns: List<CheckIn>,
    selected: Boolean,
    today: Boolean,
    inStreak: Boolean,
    connectsPrevious: Boolean,
    connectsNext: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = if (inStreak) {
        RoundedCornerShape(
            topStart = if (connectsPrevious) 0.dp else CpDimens.radiusLg,
            bottomStart = if (connectsPrevious) 0.dp else CpDimens.radiusLg,
            topEnd = if (connectsNext) 0.dp else CpDimens.radiusLg,
            bottomEnd = if (connectsNext) 0.dp else CpDimens.radiusLg,
        )
    } else {
        RoundedCornerShape(CpDimens.radiusLg)
    }
    val description = "${selectedDateTitle(date)}, ${if (checkIns.isEmpty()) "нет чекинов" else visitCount(checkIns.size)}"
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(shape)
            .background(
                when {
                    selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                    inStreak -> MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                    else -> Color.Transparent
                },
            )
            .then(
                when {
                    inStreak -> Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, shape)
                    today -> Modifier.border(1.dp, MaterialTheme.colorScheme.primary, shape)
                    else -> Modifier
                },
            )
            .semantics { contentDescription = description }
            .clickable(role = Role.Button, onClickLabel = "Выбрать дату", onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (checkIns.isEmpty()) {
            Text(
                text = day.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected || today) FontWeight.Bold else FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        } else {
            CheckInThumbnail(
                checkIns = checkIns,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

@Composable
private fun CheckInThumbnail(checkIns: List<CheckIn>, modifier: Modifier = Modifier) {
    val visibleCheckIns = checkIns.take(3)
    val thumbnailSize = 36.dp
    val stackStep = 5.dp
    Box(
        modifier = modifier
            .width(thumbnailSize + stackStep * (visibleCheckIns.size - 1))
            .height(thumbnailSize),
    ) {
        visibleCheckIns.forEachIndexed { index, checkIn ->
            val itemModifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = stackStep * index)
                .size(thumbnailSize)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
            val photo = checkIn.photoThumbnailUrls.firstOrNull()
            if (photo != null) {
                CpImage(
                    data = photo,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = itemModifier,
                )
            } else {
                Image(
                    painter = painterResource(Res.drawable.maskot_with_photo),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = itemModifier.padding(1.dp),
                )
            }
        }
    }
}

@Composable
private fun CheckInListContent(
    state: VisitedPlacesUiState,
    vm: VisitedPlacesViewModel,
    onPhotoClick: (List<String>, Int) -> Unit,
) {
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
            Text("Пока нет чекинов", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(CpDimens.spacing4),
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
    Column(
        modifier = Modifier.fillMaxWidth().padding(CpDimens.spacing4),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(message ?: "Ошибка", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(
            onClick = onRetry,
            modifier = Modifier.padding(top = CpDimens.spacing3).height(CpDimens.buttonHeight),
            shape = RoundedCornerShape(percent = 50),
        ) {
            Text("Попробовать снова")
        }
    }
}

private val monthNames = listOf(
    "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
    "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь",
)

private val monthNamesGenitive = listOf(
    "января", "февраля", "марта", "апреля", "мая", "июня",
    "июля", "августа", "сентября", "октября", "ноября", "декабря",
)

private fun monthTitle(month: CalendarMonth) = "${monthNames[month.month - 1]} ${month.year}"

private fun selectedDateTitle(date: String): String {
    val parts = date.split('-')
    val year = parts.getOrNull(0)?.toIntOrNull() ?: return date
    val month = parts.getOrNull(1)?.toIntOrNull()?.takeIf { it in 1..12 } ?: return date
    val day = parts.getOrNull(2)?.toIntOrNull() ?: return date
    return "$day ${monthNamesGenitive[month - 1]} $year"
}

private fun visitCount(count: Int) = "$count ${plural(count, "чекин", "чекина", "чекинов")}"
private fun shopCount(count: Int) = "$count ${plural(count, "кофейня", "кофейни", "кофеен")}"

private fun plural(value: Int, one: String, few: String, many: String): String {
    val mod100 = value % 100
    if (mod100 in 11..14) return many
    return when (value % 10) {
        1 -> one
        2, 3, 4 -> few
        else -> many
    }
}
