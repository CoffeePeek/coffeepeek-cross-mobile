package com.coffeepeek.admin.feature.homecheckin.ui

import com.coffeepeek.admin.ui.component.preparationMetricLabels

import com.coffeepeek.admin.ui.component.PreparationMetricLabels

import com.coffeepeek.core.designsystem.resources.coffeePeekStringArrayResource as stringArrayResource

import coffeepeek.composeapp.generated.resources.*
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

import com.coffeepeek.core.designsystem.component.coffeePeekScrollContentPadding

import com.coffeepeek.core.designsystem.component.CoffeePeekScaffold as Scaffold

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import com.coffeepeek.admin.ui.component.PlatformAlert
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.coffeepeek.core.designsystem.component.CoffeePeekFilledButton as Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.coffeepeek.core.designsystem.component.CoffeePeekOutlinedButton as OutlinedButton
import androidx.compose.material3.Text
import com.coffeepeek.core.designsystem.component.CoffeePeekTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.di.platformViewModel
import com.coffeepeek.admin.feature.catalog.api.methodLabel
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.icons.CpIcons
import com.coffeepeek.admin.ui.component.CoffeePeekLoader
import com.coffeepeek.admin.ui.component.CpTopBar
import com.coffeepeek.admin.ui.component.FullScreenImageDialog
import com.coffeepeek.admin.ui.component.PhotoViewerImages
import com.coffeepeek.admin.utils.utcIsoToLocalDateTime
import com.coffeepeek.legacy.homecheckin.domain.HomeCheckIn
import com.coffeepeek.legacy.homecheckin.domain.HomeCheckInVisibility
import com.coffeepeek.legacy.homecheckin.domain.HomeCheckInModeration

private enum class HomeTab { History, Calendar, Statistics }

@Composable
internal fun HomeCheckInScreen(vm: HomeCheckInViewModel = platformViewModel()) {
    val localizedHistoryHomeDeleteTitle = stringResource(Res.string.history_home_delete_title)
    val localizedLegacyHomeCheckInScreenDeleteEntryMessage = stringResource(Res.string.legacy_home_check_in_screen_delete_entry_message)
    val localizedHistoryHomeDeleteConfirm = stringResource(Res.string.history_home_delete_confirm)
    val localizedHistoryHomeDeleteCancel = stringResource(Res.string.history_home_delete_cancel)
    val localizedLegacyHomeCheckInScreenNewHomeCup = stringResource(Res.string.legacy_home_check_in_screen_new_home_cup)
    val localizedLegacyHomeCheckInScreenEditing = stringResource(Res.string.legacy_home_check_in_screen_editing)
    val localizedLegacyHomeCheckInDetailHomeCheckInHome = stringResource(Res.string.legacy_home_check_in_detail_home_check_in_home)
    val localizedLegacyHomeCheckInScreenHistory = stringResource(Res.string.legacy_home_check_in_screen_history)
    val localizedLegacyVisitedPlacesScreenCalendar = stringResource(Res.string.legacy_visited_places_screen_calendar)
    val localizedLegacyHomeCheckInScreenStatistics = stringResource(Res.string.legacy_home_check_in_screen_statistics)
    val localizedLegacyHomeCheckInScreenRecordBrew = stringResource(Res.string.legacy_home_check_in_screen_record_brew)

    val state by vm.state.collectAsState()
    var tab by remember { mutableStateOf(HomeTab.History) }
    LaunchedEffect(tab) { if (tab == HomeTab.Statistics) vm.loadStats() }
    var photoPreview by remember { mutableStateOf<Pair<PhotoViewerImages, Int>?>(null) }
    photoPreview?.let { (images, index) -> FullScreenImageDialog(images, index) { photoPreview = null } }

    if (state.deleteConfirmation) PlatformAlert(title = localizedHistoryHomeDeleteTitle, message = localizedLegacyHomeCheckInScreenDeleteEntryMessage, confirmLabel = localizedHistoryHomeDeleteConfirm, onConfirm = vm::delete, onDismiss = vm::cancelDelete, cancelLabel = localizedHistoryHomeDeleteCancel, destructive = true)

    Scaffold(topBar = {
        CpTopBar(
            title = if (state.editing) if (state.selected == null) localizedLegacyHomeCheckInScreenNewHomeCup else localizedLegacyHomeCheckInScreenEditing
                else localizedLegacyHomeCheckInDetailHomeCheckInHome,
            onBack = if (state.editing) vm::closeEditor else if (state.selected != null) vm::closeDetail else Navigator::popBack,
            customBack = state.editing || state.selected != null,
        )
    }) { padding ->
        when {
            state.editing -> HomeEditor(state, vm, Modifier.padding(padding))
            state.selected != null -> HomeDetail(state.selected!!, vm, Modifier.padding(padding), notice = state.saveNotice) { urls, index ->
                photoPreview = urls to index
            }
            else -> Column(Modifier.fillMaxSize().padding(padding)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HomeTab.entries.forEach { option ->
                        FilterChip(
                            selected = tab == option,
                            onClick = { tab = option },
                            label = { Text(when (option) {
                                HomeTab.History -> localizedLegacyHomeCheckInScreenHistory
                                HomeTab.Calendar -> localizedLegacyVisitedPlacesScreenCalendar
                                HomeTab.Statistics -> localizedLegacyHomeCheckInScreenStatistics
                            }) },
                        )
                    }
                }
                when (tab) {
                    HomeTab.History -> HomeHistory(state, vm, Modifier.weight(1f))
                    HomeTab.Calendar -> HomeCalendar(state, vm, Modifier.weight(1f))
                    HomeTab.Statistics -> HomeStatistics(state, Modifier.weight(1f))
                }
                Button(onClick = vm::create, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(localizedLegacyHomeCheckInScreenRecordBrew)
                }
            }
        }
    }
}

@Composable
private fun HomeHistory(state: HomeCheckInUiState, vm: HomeCheckInViewModel, modifier: Modifier) {
    val localizedLegacyHomeCheckInScreenLoadingEntry = stringResource(Res.string.legacy_home_check_in_screen_loading_entry)
    val localizedLegacyHomeCheckInScreenNoHomeBrews = stringResource(Res.string.legacy_home_check_in_screen_no_home_brews)
    val localizedLegacyHomeCheckInScreenBeansUnspecified = stringResource(Res.string.legacy_home_check_in_screen_beans_unspecified)
    val localizedShopDetailLoading = stringResource(Res.string.shop_detail_loading)
    val localizedNotificationsLoadMore = stringResource(Res.string.notifications_load_more)
    val localizedLegacyHomeCheckInScreenOwnershipRefresh = stringResource(Res.string.legacy_home_check_in_screen_ownership_refresh)

    LazyColumn(modifier, contentPadding = coffeePeekScrollContentPadding(PaddingValues(16.dp)), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (state.selectedDate != null) item {
            TextButton(onClick = { vm.selectDate(null) }) { Text(stringResource(Res.string.legacy_home_selected_date_reset, state.selectedDate.orEmpty())) }
        }
        if (state.loading) item { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CoffeePeekLoader() } }
        if (state.error != null) item { Text(state.error, color = MaterialTheme.colorScheme.error) }
        if (state.detailLoading) item { Text(localizedLegacyHomeCheckInScreenLoadingEntry) }
        if (!state.loading && state.items.isEmpty()) item { Text(localizedLegacyHomeCheckInScreenNoHomeBrews) }
        items(state.items, key = { it.id }) { entry ->
            Card(Modifier.fillMaxWidth().clickable { vm.open(entry.id) }) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(entry.preparedOn, style = MaterialTheme.typography.labelMedium)
                    Text(entry.beanName ?: localizedLegacyHomeCheckInScreenBeansUnspecified, style = MaterialTheme.typography.titleMedium)
                    Text(listOfNotNull(entry.brewMethodCode?.methodLabel(stringResource(Res.string.select_drink_other)), entry.doseGrams?.let { stringResource(Res.string.legacy_grams, it.toString()) },
                        entry.tasteScore?.let { "$it/5" }).joinToString(" · "))
                    val note = entry.text
                    if (!note.isNullOrBlank()) Text(note, maxLines = 2)
                }
            }
        }
        if (state.hasMore) item {
            OutlinedButton(onClick = vm::loadMore, enabled = !state.loadingMore, modifier = Modifier.fillMaxWidth()) {
                Text(if (state.loadingMore) localizedShopDetailLoading else localizedNotificationsLoadMore)
            }
        }
        item { TextButton(onClick = vm::refresh) { Text(localizedLegacyHomeCheckInScreenOwnershipRefresh) } }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HomeCalendar(state: HomeCheckInUiState, vm: HomeCheckInViewModel, modifier: Modifier) {
    val localizedLegacyProfileCheckInsScreenHistoryClearDate = stringResource(Res.string.legacy_profile_check_ins_screen_history_clear_date)

    val month = state.month
    val counts = state.calendar.associate { it.preparedOn to it.brewCount }
    LazyColumn(modifier, contentPadding = coffeePeekScrollContentPadding(PaddingValues(16.dp)), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { vm.changeMonth(-1) }) { Text("‹") }
                Text("${month.month.toString().padStart(2, '0')}.${month.year}",
                    style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = { vm.changeMonth(1) }) { Text("›") }
            }
        }
        item { Row(Modifier.fillMaxWidth()) {
            stringArrayResource(Res.array.legacy_weekdays_letters).forEach { label ->
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { Text(label) }
            }
        } }
        val cells = List<Int?>(month.mondayOffset) { null } + (1..daysInMonth(month.year, month.month)).map { it }
        items(cells.chunked(7)) { week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                (0..6).forEach { index ->
                    val day = week.getOrNull(index)
                    val date = day?.let(month::date)
                    val count = date?.let { counts[it] } ?: 0
                    Box(
                        Modifier.weight(1f).height(48.dp)
                            .background(if (date == state.selectedDate) MaterialTheme.colorScheme.primaryContainer
                                else if (count > 0) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
                            .then(if (date != null) Modifier.clickable { vm.selectDate(date) } else Modifier),
                        contentAlignment = Alignment.Center,
                    ) { if (day != null) Text(if (count > 0) "$day•" else "$day") }
                }
            }
        }
        item { Text(stringResource(Res.string.legacy_home_month_brews, state.calendar.sumOf { it.brewCount })) }
        if (state.selectedDate != null) item {
            Text(stringResource(Res.string.legacy_home_day_brews, state.selectedDate.orEmpty(), counts[state.selectedDate] ?: 0))
            TextButton(onClick = { vm.selectDate(null) }) { Text(localizedLegacyProfileCheckInsScreenHistoryClearDate) }
        }
    }
}

@Composable
private fun HomeStatistics(state: HomeCheckInUiState, modifier: Modifier) {
    val localizedLegacyHomeCheckInScreenByBeans = stringResource(Res.string.legacy_home_check_in_screen_by_beans)
    val localizedLegacyHomeCheckInScreenNoData = stringResource(Res.string.legacy_home_check_in_screen_no_data)
    val localizedLegacyHomeCheckInScreenBeansUnspecified = stringResource(Res.string.legacy_home_check_in_screen_beans_unspecified)
    val localizedLegacyHomeCheckInScreenByBrewMethod = stringResource(Res.string.legacy_home_check_in_screen_by_brew_method)
    val localizedLegacyHomeCheckInScreenMethodUnspecified = stringResource(Res.string.legacy_home_check_in_screen_method_unspecified)

    LazyColumn(modifier, contentPadding = coffeePeekScrollContentPadding(PaddingValues(16.dp)), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text(localizedLegacyHomeCheckInScreenByBeans, style = MaterialTheme.typography.titleMedium) }
        if (state.beanStats.isEmpty()) item { Text(localizedLegacyHomeCheckInScreenNoData) }
        items(state.beanStats) { stats ->
            StatCard(stats.beanName ?: localizedLegacyHomeCheckInScreenBeansUnspecified, stats.brewCount, stats.dosedBrewCount,
                stats.totalKnownDoseGrams, stats.ratedBrewCount, stats.averageTasteScore)
        }
        item { Spacer(Modifier.height(12.dp)); Text(localizedLegacyHomeCheckInScreenByBrewMethod, style = MaterialTheme.typography.titleMedium) }
        items(state.methodStats) { stats ->
            StatCard(stats.brewMethodCode?.methodLabel(stringResource(Res.string.select_drink_other)) ?: localizedLegacyHomeCheckInScreenMethodUnspecified, stats.brewCount,
                stats.dosedBrewCount, stats.totalKnownDoseGrams, stats.ratedBrewCount, stats.averageTasteScore)
        }
    }
}

@Composable
private fun StatCard(name: String, count: Int, dosedCount: Int, dose: String, ratedCount: Int, average: Double?) {
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(name, fontWeight = FontWeight.SemiBold)
        Text(stringResource(Res.string.legacy_home_stats_counts, count, dosedCount))
        Text(stringResource(Res.string.legacy_home_stats_dose, dose.toString(), ratedCount))
        Text(stringResource(Res.string.legacy_home_stats_rating, average?.toString() ?: "—"))
    } }
}

@Composable
internal fun HomeTimelineCard(entry: HomeCheckIn, onClick: () -> Unit) {
    val metricLabels = preparationMetricLabels()

    val localizedLegacyHomeCheckInDetailHomeCheckInTitleFallback = stringResource(Res.string.legacy_home_check_in_detail_home_check_in_title_fallback)

    val accent = MaterialTheme.colorScheme.primary
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp),
                itemVerticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(CpIcons.Coffee, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                    Text(stringResource(Res.string.legacy_home_prepared_time, utcIsoToLocalDateTime(entry.preparedAtUtc).substringAfterLast(' ', "")),
                        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                entry.visibilityLabel(homeVisibilityLabels())?.let { label ->
                    Text(label, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(entry.beanName?.takeIf(String::isNotBlank) ?: localizedLegacyHomeCheckInDetailHomeCheckInTitleFallback,
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            val details = listOfNotNull(
                entry.brewMethodCode?.methodLabel(stringResource(Res.string.select_drink_other)),
                entry.doseGrams?.let { homeDoseLabel(it, metricLabels) },
            ).joinToString(" · ")
            if (details.isNotBlank()) Text(details, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            entry.tasteScore?.let { score ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(CpIcons.StarFilled, null, tint = accent, modifier = Modifier.size(15.dp))
                    Text("$score/5", style = MaterialTheme.typography.labelMedium)
                }
            }
            entry.text?.takeIf(String::isNotBlank)?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

internal fun HomeCheckIn.visibilityLabel(labels: List<String>): String? = when (visibility) {
    HomeCheckInVisibility.Private -> labels[0]
    HomeCheckInVisibility.Public -> when (moderation) {
        HomeCheckInModeration.Approved -> labels[1]
        HomeCheckInModeration.Pending -> labels[2]
        HomeCheckInModeration.Rejected -> labels[3]
        HomeCheckInModeration.NotSubmitted -> labels[4]
        HomeCheckInModeration.Unknown -> labels[5]
    }
    null -> null
}

@Composable
internal fun homeVisibilityLabels(): List<String> = listOf(
    stringResource(Res.string.legacy_home_visibility_home_check_in_private),
    stringResource(Res.string.legacy_home_visibility_home_check_in_public),
    stringResource(Res.string.legacy_my_contributions_screen_contributions_tab_pending),
    stringResource(Res.string.legacy_home_visibility_home_check_in_rejected),
    stringResource(Res.string.legacy_home_visibility_home_check_in_not_submitted),
    stringResource(Res.string.legacy_home_visibility_community_label_public),
)
