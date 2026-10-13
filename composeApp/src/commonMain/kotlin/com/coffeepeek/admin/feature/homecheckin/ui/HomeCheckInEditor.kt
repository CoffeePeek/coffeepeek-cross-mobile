package com.coffeepeek.admin.feature.homecheckin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.checkin_rating_coffee
import com.coffeepeek.admin.feature.coffee.api.CoffeePickerItem
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.component.AppButton
import com.coffeepeek.admin.ui.component.CheckInVisibilityControl
import com.coffeepeek.admin.ui.component.CoffeePeekLoader
import com.coffeepeek.admin.ui.component.CompactOutlinedTextField
import com.coffeepeek.admin.ui.component.CpSearchField
import com.coffeepeek.admin.ui.component.EditableRatingCard
import com.coffeepeek.admin.ui.component.NativeActionSheet
import com.coffeepeek.admin.ui.component.NativeDatePicker
import com.coffeepeek.admin.ui.component.PhotoAttachmentsSection
import com.coffeepeek.admin.ui.component.PlatformAction
import com.coffeepeek.admin.ui.component.PlatformScrollableColumn
import com.coffeepeek.admin.ui.component.ReviewFormField
import com.coffeepeek.admin.ui.component.ReviewFormStep
import com.coffeepeek.admin.ui.component.ReviewTextInput
import com.coffeepeek.admin.ui.icons.CpIcons
import com.coffeepeek.admin.utils.PickedImage
import com.coffeepeek.admin.utils.currentEpochMillis
import com.coffeepeek.admin.utils.currentUtcIsoDateTime
import com.coffeepeek.admin.utils.datePickerMillisToUtcIsoInstant
import com.coffeepeek.admin.utils.formatVisitDate
import com.coffeepeek.admin.utils.utcIsoToLocalDate
import com.coffeepeek.feature.homecheckin.domain.HomeCheckInInput
import com.coffeepeek.feature.homecheckin.domain.HomeCheckInVisibility
import com.coffeepeek.feature.homecheckin.domain.HomePhotoUpload
import com.coffeepeek.feature.homecheckin.domain.homeBrewMethods
import kotlin.time.Instant

@Composable
internal fun HomeEditor(state: HomeCheckInUiState, vm: HomeCheckInViewModel, modifier: Modifier) {
    HomeEditorContent(state, { draft -> vm.updateDraft { draft } }, vm::setCatalogQuery,
        vm::searchCatalog, vm::selectCatalogProduct, vm::save, vm::closeEditor, modifier)
}

/** Uses the same fields, rating card and step rhythm as the coffee-shop check-in form. */
@OptIn(kotlin.time.ExperimentalTime::class)
@Composable
internal fun HomeEditorContent(
    state: HomeCheckInUiState,
    onDraftChange: (HomeCheckInInput) -> Unit,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onProductSelected: (CoffeePickerItem) -> Unit,
    onSave: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val draft = state.draft
    var showDatePicker by remember { mutableStateOf(false) }
    var showBeanPicker by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val date = draft.preparedOn ?: utcIsoToLocalDate(draft.preparedAt)
    val dateMillis = remember(date) {
        runCatching { Instant.parse("${date}T00:00:00Z").toEpochMilliseconds() }.getOrDefault(currentEpochMillis())
    }

    if (showDatePicker) HomeDatePicker(dateMillis, onDismiss = { showDatePicker = false }) { millis ->
        // Keeping the same date must not silently replace the recorded time with midnight.
        if (millis != dateMillis) {
            val instant = datePickerMillisToUtcIsoInstant(millis)
            onDraftChange(draft.copy(preparedAt = instant, preparedOn = utcIsoToLocalDate(instant)))
        }
        showDatePicker = false
    }

    if (showBeanPicker) {
        HomeBeanPicker(state, onQueryChange, onSearch,
            onProductSelected = {
                focus.clearFocus(); keyboard?.hide()
                onProductSelected(it); showBeanPicker = false
            },
            onCustomName = { name ->
                focus.clearFocus(); keyboard?.hide()
                onDraftChange(draft.copy(coffeeProductSlug = null, coffeeProductId = null, customBeanName = name))
                showBeanPicker = false
            },
            onBack = { focus.clearFocus(); keyboard?.hide(); showBeanPicker = false }, modifier = modifier)
        return
    }

    Column(modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = CpDimens.spacing4).padding(bottom = CpDimens.spacing4),
            verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(CpDimens.spacing1)) {
                Text(if (state.selected == null) "Кофе дома" else "Редактировать чекин",
                    style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = onClose, enabled = !state.saving, modifier = Modifier.size(CpDimens.controlHeight)) {
                Icon(CpIcons.Close, "Закрыть форму", Modifier.size(20.dp))
            }
        }
        PlatformScrollableColumn(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = CpDimens.spacing4),
            verticalArrangement = Arrangement.spacedBy(CpDimens.spacing6),
        ) {
            CheckInVisibilityControl(draft.visibility == HomeCheckInVisibility.Public, enabled = !state.saving) {
                onDraftChange(draft.copy(visibility = if (it) HomeCheckInVisibility.Public else HomeCheckInVisibility.Private))
            }
            EditableRatingCard(Res.drawable.checkin_rating_coffee, "Как получился кофе?", draft.tasteScore ?: 0,
                onRatingChange = { score ->
                    if (!state.saving) onDraftChange(draft.copy(tasteScore = score.takeUnless { it == draft.tasteScore }))
                })
            Column {
                ReviewFormStep {
                    ReviewFormField("Дата приготовления") {
                        HomeSelectField(formatVisitDate(dateMillis), CpIcons.Calendar, "Выбрать дату приготовления",
                            enabled = !state.saving, onClick = { showDatePicker = true })
                    }
                }
                ReviewFormStep(optional = true) {
                    ReviewFormField("Способ приготовления") {
                        HomeMethodField(draft.brewMethodCode, !state.saving) {
                            onDraftChange(draft.copy(brewMethodCode = it))
                        }
                    }
                }
                ReviewFormStep(optional = true) {
                    ReviewFormField("Зерно") {
                        HomeSelectField(draft.customBeanName ?: state.catalogSelectionName ?: "Выбрать зерно",
                            CpIcons.CoffeeBean, "Выбрать зерно", enabled = !state.saving,
                            onClick = { focus.clearFocus(); keyboard?.hide(); showBeanPicker = true })
                    }
                }
                ReviewFormStep(optional = true) {
                    Row(horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing3)) {
                        Column(Modifier.weight(1f)) {
                            ReviewFormField("Кофе, г") {
                                HomeNumberField(draft.doseGrams.orEmpty(), "Например, 18", KeyboardType.Decimal, !state.saving) {
                                    onDraftChange(draft.copy(doseGrams = it.replace(',', '.').ifBlank { null }))
                                }
                            }
                        }
                        Column(Modifier.weight(1f)) {
                            ReviewFormField("Время, с") {
                                HomeNumberField(draft.brewDurationSeconds?.toString().orEmpty(), "Например, 180", KeyboardType.Number, !state.saving) {
                                    if (it.isEmpty() || it.toIntOrNull() != null) {
                                        onDraftChange(draft.copy(brewDurationSeconds = it.toIntOrNull()))
                                    }
                                }
                            }
                        }
                    }
                }
                ReviewFormStep(optional = true) {
                    ReviewFormField("Заметка") {
                        ReviewTextInput(draft.text.orEmpty(), { if (!state.saving) onDraftChange(draft.copy(text = it.ifBlank { null })) },
                            "Какой вкус? Что хочется повторить?", maxLength = 1000, modifier = Modifier.heightIn(min = 80.dp))
                    }
                }
                ReviewFormStep(optional = true) {
                    ReviewFormField("Напиток") {
                        ReviewTextInput(draft.customDrinkName ?: state.selected?.drinkName.takeIf { draft.drinkSlug != null }.orEmpty(),
                            { if (!state.saving) onDraftChange(draft.copy(drinkSlug = null, customDrinkName = it.ifBlank { null })) },
                            "Например, капучино", singleLine = true, maxLength = 100)
                    }
                }
                ReviewFormStep(optional = true, last = true) {
                    if (state.selected == null) {
                        PhotoAttachmentsSection(
                            photos = draft.photos.map { PickedImage(it.bytes, it.fileName, it.contentType) }, maxPhotos = 5,
                            onPhotosAdded = { added -> if (!state.saving) onDraftChange(draft.copy(
                                photos = (draft.photos + added.map { HomePhotoUpload(it.fileName, it.contentType, it.bytes) }).take(5))) },
                            onRemovePhoto = { index -> if (!state.saving) {
                                val remaining = draft.photos.filterIndexed { i, _ -> i != index }
                                onDraftChange(draft.copy(photos = remaining, drinkPhoto = draft.drinkPhoto.takeIf { remaining.isNotEmpty() }))
                            } },
                            hint = "Чашка, зерно или процесс приготовления — до 5 фото.",
                        )
                    } else {
                        Text("Фотографии этой записи сохранятся без изменений", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        Column(Modifier.fillMaxWidth().padding(CpDimens.spacing4),
            verticalArrangement = Arrangement.spacedBy(CpDimens.spacing2)) {
            state.formError?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
            AppButton(if (state.saving) "Сохраняем…" else "Сохранить чекин", onClick = {
                keyboard?.hide(); focus.clearFocus(force = true); onSave()
            }, enabled = !state.saving)
        }
    }
}

@Composable
private fun HomeSelectField(value: String, icon: ImageVector, action: String, enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(CpDimens.radiusMd)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
            .border(1.dp, MaterialTheme.colorScheme.outline, shape).clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = action }
            .heightIn(min = CpDimens.controlHeight).padding(horizontal = 14.dp, vertical = CpDimens.spacing3),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing2),
    ) {
        Icon(icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Icon(CpIcons.ChevronUpDown, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun HomeNumberField(value: String, placeholder: String, type: KeyboardType, enabled: Boolean, onChange: (String) -> Unit) {
    CompactOutlinedTextField(value, onChange, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = CpDimens.controlHeight),
        shape = RoundedCornerShape(CpDimens.inputRadius), textStyle = MaterialTheme.typography.bodyMedium,
        placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        keyboardOptions = KeyboardOptions(keyboardType = type), maxLength = 8,
        inputFilter = { it.isDigit() || type == KeyboardType.Decimal && (it == '.' || it == ',') },
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
            unfocusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
            focusedBorderColor = MaterialTheme.colorScheme.outline, unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        ))
}

@Composable
private fun HomeMethodField(selected: String?, enabled: Boolean, onSelect: (String?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf<String?>(null) + homeBrewMethods
    val native = expanded && NativeActionSheet("Способ приготовления", options.map { code ->
        PlatformAction(code?.methodLabel() ?: "Не указывать", onClick = { onSelect(code); expanded = false })
    }) { expanded = false }
    Box {
        HomeSelectField(selected?.methodLabel() ?: "Выбрать способ", CpIcons.Coffee, "Выбрать способ приготовления", enabled) { expanded = true }
        DropdownMenu(expanded && !native, onDismissRequest = { expanded = false },
            modifier = Modifier.widthIn(min = 240.dp, max = 320.dp).heightIn(max = 360.dp),
            shape = RoundedCornerShape(CpDimens.radiusMd), containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
            options.forEach { code ->
                DropdownMenuItem(text = { Text(code?.methodLabel() ?: "Не указывать", style = MaterialTheme.typography.bodyMedium) },
                    onClick = { onSelect(code); expanded = false },
                    leadingIcon = { if (code == selected) Icon(CpIcons.Check, null, Modifier.size(18.dp)) else Box(Modifier.size(18.dp)) })
            }
        }
    }
}

@Composable
private fun HomeBeanPicker(state: HomeCheckInUiState, onQueryChange: (String) -> Unit, onSearch: () -> Unit,
    onProductSelected: (CoffeePickerItem) -> Unit, onCustomName: (String?) -> Unit, onBack: () -> Unit, modifier: Modifier) {
    var customName by remember { mutableStateOf(state.draft.customBeanName.orEmpty()) }
    Column(modifier.fillMaxSize().padding(horizontal = CpDimens.spacing4),
        verticalArrangement = Arrangement.spacedBy(CpDimens.spacing4)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(CpDimens.controlHeight)) { Icon(CpIcons.Back, "Назад к приготовлению") }
            Text("Зерно", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            TextButton(onClick = { onCustomName(null) }) { Text("Не указывать") }
        }
        CpSearchField(state.catalogQuery, onQueryChange, "Название или обжарщик", Modifier.fillMaxWidth(), onSearch = onSearch)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Из каталога", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            TextButton(onClick = onSearch, enabled = state.catalogQuery.isNotBlank() && !state.catalogLoading) { Text("Найти") }
        }
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(CpDimens.spacing2)) {
            if (state.catalogLoading) item { CoffeePeekLoader() }
            if (state.catalogError != null) item {
                Text("Не удалось найти зерно. Попробуйте ещё раз.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            items(state.catalogItems, key = { it.slug }) { product ->
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(CpDimens.radiusMd))
                    .clickable(role = Role.Button) { onProductSelected(product) }.padding(vertical = CpDimens.spacing3, horizontal = CpDimens.spacing2)) {
                    Text(product.name, style = MaterialTheme.typography.bodyLarge)
                    Text(product.roasterName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            }
            if (!state.catalogLoading && state.catalogItems.isEmpty() && state.catalogError == null) item {
                Text("Найдите зерно в каталоге или добавьте своё название ниже.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Column(Modifier.padding(bottom = CpDimens.spacing4), verticalArrangement = Arrangement.spacedBy(CpDimens.spacing3)) {
            ReviewFormField("Своё зерно") {
                ReviewTextInput(customName, { customName = it }, "Например, Эфиопия Иргачеффе", singleLine = true, maxLength = 250,
                    modifier = Modifier.semantics { contentDescription = "Название своего зерна" })
            }
            AppButton("Использовать своё зерно", onClick = { onCustomName(customName.trim()) }, enabled = customName.isNotBlank())
        }
    }
}

@OptIn(kotlin.time.ExperimentalTime::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun HomeDatePicker(initialMillis: Long, onDismiss: () -> Unit, onSelected: (Long) -> Unit) {
    val maxMillis = runCatching {
        Instant.parse("${utcIsoToLocalDate(currentUtcIsoDateTime())}T23:59:59Z").toEpochMilliseconds()
    }.getOrDefault(currentEpochMillis())
    if (NativeDatePicker("Дата приготовления", initialMillis, maxMillis, false, onSelected, onDismiss)) return
    val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= maxMillis
        })
    DatePickerDialog(onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { state.selectedDateMillis?.let(onSelected) }) { Text("Готово") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    ) { DatePicker(state = state, showModeToggle = false) }
}
