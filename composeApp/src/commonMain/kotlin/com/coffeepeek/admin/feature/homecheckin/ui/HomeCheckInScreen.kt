package com.coffeepeek.admin.feature.homecheckin.ui

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import com.coffeepeek.admin.ui.component.PlatformAlert
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.di.platformViewModel
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.CoffeePeekLoader
import com.coffeepeek.admin.ui.component.CpTopBar
import com.coffeepeek.admin.ui.component.FullScreenImageDialog
import com.coffeepeek.admin.ui.component.PhotoAttachmentsSection
import com.coffeepeek.admin.utils.CpImage
import com.coffeepeek.admin.utils.PickedImage
import com.coffeepeek.feature.homecheckin.domain.HomeCheckIn
import com.coffeepeek.feature.homecheckin.domain.HomePhotoUpload
import com.coffeepeek.feature.homecheckin.domain.homeBrewMethods

private enum class HomeTab { History, Calendar, Statistics }

@Composable
internal fun HomeCheckInScreen(vm: HomeCheckInViewModel = platformViewModel()) {
    val state by vm.state.collectAsState()
    var tab by remember { mutableStateOf(HomeTab.History) }
    var photoPreview by remember { mutableStateOf<Pair<List<String>, Int>?>(null) }
    photoPreview?.let { (urls, index) -> FullScreenImageDialog(urls, index) { photoPreview = null } }

    if (state.deleteConfirmation) PlatformAlert(title = "Удалить домашнюю запись?", message = "Она исчезнет из истории, календаря и статистики.", confirmLabel = "Удалить", onConfirm = vm::delete, onDismiss = vm::cancelDelete, cancelLabel = "Отмена", destructive = true)

    Scaffold(topBar = {
        CpTopBar(
            title = if (state.editing) if (state.selected == null) "Новая чашка дома" else "Редактирование"
                else "Кофе дома",
            onBack = if (state.editing) vm::closeEditor else if (state.selected != null) vm::closeDetail else Navigator::popBack,
            customBack = state.editing || state.selected != null,
        )
    }) { padding ->
        when {
            state.editing -> HomeEditor(state, vm, Modifier.padding(padding))
            state.selected != null -> HomeDetail(state.selected!!, vm, Modifier.padding(padding)) { urls, index ->
                photoPreview = urls to index
            }
            else -> Column(Modifier.fillMaxSize().padding(padding)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HomeTab.entries.forEach { option ->
                        FilterChip(
                            selected = tab == option,
                            onClick = { tab = option },
                            label = { Text(when (option) {
                                HomeTab.History -> "История"
                                HomeTab.Calendar -> "Календарь"
                                HomeTab.Statistics -> "Статистика"
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
                    Text("Записать приготовление")
                }
            }
        }
    }
}

@Composable
private fun HomeHistory(state: HomeCheckInUiState, vm: HomeCheckInViewModel, modifier: Modifier) {
    LazyColumn(modifier, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (state.selectedDate != null) item {
            TextButton(onClick = { vm.selectDate(null) }) { Text("${state.selectedDate} · Сбросить фильтр") }
        }
        if (state.loading) item { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CoffeePeekLoader() } }
        if (state.error != null) item { Text(state.error, color = MaterialTheme.colorScheme.error) }
        if (state.detailLoading) item { Text("Загружаем запись…") }
        if (!state.loading && state.items.isEmpty()) item { Text("Пока нет домашних приготовлений") }
        items(state.items, key = { it.id }) { entry ->
            Card(Modifier.fillMaxWidth().clickable { vm.open(entry.id) }) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(entry.preparedOn, style = MaterialTheme.typography.labelMedium)
                    Text(entry.beanName ?: "Зерно не указано", style = MaterialTheme.typography.titleMedium)
                    Text(listOfNotNull(entry.brewMethodCode?.methodLabel(), entry.doseGrams?.let { "$it г" },
                        entry.tasteScore?.let { "$it/5" }).joinToString(" · "))
                    val note = entry.text
                    if (!note.isNullOrBlank()) Text(note, maxLines = 2)
                }
            }
        }
        if (state.hasMore) item {
            OutlinedButton(onClick = vm::loadMore, enabled = !state.loadingMore, modifier = Modifier.fillMaxWidth()) {
                Text(if (state.loadingMore) "Загрузка…" else "Показать ещё")
            }
        }
        item { TextButton(onClick = vm::refresh) { Text("Обновить") } }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HomeCalendar(state: HomeCheckInUiState, vm: HomeCheckInViewModel, modifier: Modifier) {
    val month = state.month
    val counts = state.calendar.associate { it.preparedOn to it.brewCount }
    LazyColumn(modifier, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
            listOf("П", "В", "С", "Ч", "П", "С", "В").forEach { label ->
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
        item { Text("${state.calendar.sumOf { it.brewCount }} приготовлений за месяц") }
        if (state.selectedDate != null) item {
            Text("${state.selectedDate}: ${counts[state.selectedDate] ?: 0} приготовлений")
            TextButton(onClick = { vm.selectDate(null) }) { Text("Показать все даты") }
        }
    }
}

@Composable
private fun HomeStatistics(state: HomeCheckInUiState, modifier: Modifier) {
    LazyColumn(modifier, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text("По зерну", style = MaterialTheme.typography.titleMedium) }
        if (state.beanStats.isEmpty()) item { Text("Пока нет данных") }
        items(state.beanStats) { stats ->
            StatCard(stats.beanName ?: "Зерно не указано", stats.brewCount, stats.dosedBrewCount,
                stats.totalKnownDoseGrams, stats.ratedBrewCount, stats.averageTasteScore)
        }
        item { Spacer(Modifier.height(12.dp)); Text("По способу", style = MaterialTheme.typography.titleMedium) }
        items(state.methodStats) { stats ->
            StatCard(stats.brewMethodCode?.methodLabel() ?: "Способ не указан", stats.brewCount,
                stats.dosedBrewCount, stats.totalKnownDoseGrams, stats.ratedBrewCount, stats.averageTasteScore)
        }
    }
}

@Composable
private fun StatCard(name: String, count: Int, dosedCount: Int, dose: String, ratedCount: Int, average: Double?) {
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(name, fontWeight = FontWeight.SemiBold)
        Text("Приготовлений: $count · С известной дозой: $dosedCount")
        Text("Всего кофе: $dose г · С оценкой: $ratedCount")
        Text("Средняя оценка: ${average?.toString() ?: "—"}")
    } }
}

@Composable
private fun HomeDetail(entry: HomeCheckIn, vm: HomeCheckInViewModel, modifier: Modifier,
    onPhotoClick: (List<String>, Int) -> Unit) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text(entry.preparedOn, style = MaterialTheme.typography.titleLarge) }
        item { Text(entry.beanName ?: "Зерно не указано", style = MaterialTheme.typography.headlineSmall) }
        item { Text("Время: ${entry.preparedAtUtc}") }
        item { Text("Способ: ${entry.brewMethodCode?.methodLabel() ?: "не указан"}") }
        item { Text("Доза на всё приготовление: ${entry.doseGrams?.let { "$it г" } ?: "не указана"}") }
        item { Text("Длительность: ${entry.brewDurationSeconds?.let { "$it с" } ?: "не указана"}") }
        item { Text("Оценка: ${entry.tasteScore?.let { "$it/5" } ?: "не указана"}") }
        if (entry.drinkName != null) item { Text("Напиток: ${entry.drinkName}") }
        val note = entry.text
        if (!note.isNullOrBlank()) item { Text(note) }
        val drinkPhotoUrl = entry.drinkPhotoUrl
        if (drinkPhotoUrl != null) item {
            Text("Изображение напитка")
            CpImage(drinkPhotoUrl, modifier = Modifier.size(120.dp), contentScale = ContentScale.Fit)
        }
        if (entry.photos.isNotEmpty()) item {
            val urls = entry.photos.map { it.url }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                urls.take(5).forEachIndexed { index, url ->
                    CpImage(url, modifier = Modifier.size(60.dp).clickable { onPhotoClick(urls, index) },
                        contentScale = ContentScale.Crop)
                }
            }
        }
        item { OutlinedButton(onClick = vm::edit, modifier = Modifier.fillMaxWidth()) { Text("Редактировать") } }
        item { TextButton(onClick = vm::confirmDelete, modifier = Modifier.fillMaxWidth()) { Text("Удалить запись") } }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HomeEditor(state: HomeCheckInUiState, vm: HomeCheckInViewModel, modifier: Modifier) {
    val draft = state.draft
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Одна запись = одно приготовление, включая целый batch.") }
        item { HomeField("Дата и время (ISO с Z или +03:00)", draft.preparedAt) { value ->
            vm.updateDraft { it.copy(preparedAt = value) }
        } }
        item { HomeField("Дата в календаре", draft.preparedOn.orEmpty()) { value ->
            vm.updateDraft { it.copy(preparedOn = value.ifBlank { null }) }
        } }
        item { Text("Способ приготовления", style = MaterialTheme.typography.titleSmall) }
        item { FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (listOf<String?>(null) + homeBrewMethods).forEach { code ->
                FilterChip(selected = draft.brewMethodCode == code,
                    onClick = { vm.updateDraft { it.copy(brewMethodCode = code) } },
                    label = { Text(code?.methodLabel() ?: "Не указан") })
            }
        } }
        item { HomeField("Найти зерно в каталоге", state.catalogQuery, onChange = vm::setCatalogQuery) }
        item { OutlinedButton(onClick = vm::searchCatalog, enabled = !state.catalogLoading && state.catalogQuery.isNotBlank()) {
            Text(if (state.catalogLoading) "Поиск…" else "Искать в каталоге")
        } }
        if (state.catalogError != null) item { Text(state.catalogError, color = MaterialTheme.colorScheme.error) }
        items(state.catalogItems, key = { it.slug }) { product ->
            Card(Modifier.fillMaxWidth().clickable { vm.selectCatalogProduct(product) }) {
                Column(Modifier.padding(12.dp)) {
                    Text(product.name, fontWeight = FontWeight.SemiBold)
                    Text(product.roasterName, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item { HomeField("Slug зерна из каталога", draft.coffeeProductSlug.orEmpty()) { value ->
            vm.updateDraft { it.copy(coffeeProductSlug = value.ifBlank { null }, coffeeProductId = null,
                customBeanName = if (value.isBlank()) it.customBeanName else null) }
        } }
        item { HomeField("Или своё название зерна", draft.customBeanName.orEmpty()) { value ->
            vm.updateDraft { it.copy(customBeanName = value.ifBlank { null }, coffeeProductSlug = null,
                coffeeProductId = if (value.isBlank()) it.coffeeProductId else null) }
        } }
        if (draft.coffeeProductId != null) item {
            Text("Сохранено историческое название зерна из каталога")
            TextButton(onClick = { vm.updateDraft { it.copy(coffeeProductId = null) } }) { Text("Очистить зерно") }
        }
        item { HomeField("Доза на всё приготовление, г", draft.doseGrams.orEmpty()) { value ->
            vm.updateDraft { it.copy(doseGrams = value.replace(',', '.').ifBlank { null }) }
        } }
        item { HomeField("Общая длительность, с", draft.brewDurationSeconds?.toString().orEmpty()) { value ->
            vm.updateDraft { it.copy(brewDurationSeconds = value.toIntOrNull()) }
        } }
        item { Text("Оценка результата", style = MaterialTheme.typography.titleSmall) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (0..5).forEach { score ->
                FilterChip(selected = draft.tasteScore == score.takeIf { it != 0 },
                    onClick = { vm.updateDraft { it.copy(tasteScore = score.takeIf { it != 0 }) } },
                    label = { Text(if (score == 0) "—" else "$score") })
            }
        } }
        item { HomeField("Slug напитка", draft.drinkSlug.orEmpty()) { value ->
            vm.updateDraft { it.copy(drinkSlug = value.ifBlank { null }, customDrinkName = null) }
        } }
        item { HomeField("Или свой напиток", draft.customDrinkName.orEmpty()) { value ->
            vm.updateDraft { it.copy(customDrinkName = value.ifBlank { null }, drinkSlug = null) }
        } }
        item { HomeField("Заметка", draft.text.orEmpty(), singleLine = false) { value ->
            vm.updateDraft { it.copy(text = value.ifBlank { null }) }
        } }
        if (state.selected == null) item {
            PhotoAttachmentsSection(
                photos = draft.photos.map { PickedImage(it.bytes, it.fileName, it.contentType) },
                maxPhotos = 5,
                onPhotosAdded = { added -> vm.updateDraft { old -> old.copy(
                    photos = (old.photos + added.map { HomePhotoUpload(it.fileName, it.contentType, it.bytes) }).take(5),
                ) } },
                onRemovePhoto = { index -> vm.updateDraft { old ->
                    val remaining = old.photos.filterIndexed { i, _ -> i != index }
                    old.copy(photos = remaining, drinkPhoto = old.drinkPhoto.takeIf { remaining.isNotEmpty() })
                } },
            )
        }
        if (state.selected == null && draft.photos.isNotEmpty()) item {
            PhotoAttachmentsSection(
                photos = listOfNotNull(draft.drinkPhoto?.let { PickedImage(it.bytes, it.fileName, it.contentType) }),
                maxPhotos = 1,
                title = "Вырезка напитка (PNG)",
                hint = "Необязательное отдельное изображение PNG",
                onPhotosAdded = { added -> vm.updateDraft { old -> old.copy(
                    drinkPhoto = added.firstOrNull()?.let { HomePhotoUpload(it.fileName, it.contentType, it.bytes) },
                ) } },
                onRemovePhoto = { vm.updateDraft { it.copy(drinkPhoto = null) } },
            )
        }
        if (state.selected != null && state.selected.photos.isNotEmpty()) item {
            Text("При редактировании ${state.selected.photos.size} фото сохраняются без изменений")
        }
        if (state.formError != null) item { Text(state.formError, color = MaterialTheme.colorScheme.error) }
        item { Button(onClick = vm::save, enabled = !state.saving, modifier = Modifier.fillMaxWidth()) {
            Text(if (state.saving) "Сохраняем…" else "Сохранить")
        } }
    }
}

@Composable
private fun HomeField(label: String, value: String, singleLine: Boolean = true, onChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label) },
        singleLine = singleLine, modifier = Modifier.fillMaxWidth())
}

private fun String.methodLabel(): String = when (this) {
    "espresso" -> "Эспрессо"
    "pour_over" -> "Пуровер"
    "batch_drip" -> "Фильтр batch"
    "moka_pot" -> "Гейзерная кофеварка"
    "cezve" -> "Турка"
    "aeropress" -> "Аэропресс"
    "french_press" -> "Френч-пресс"
    "other" -> "Другое"
    else -> this
}
