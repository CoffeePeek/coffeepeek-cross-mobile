package com.coffeepeek.admin.feature.homecheckin.ui

import com.coffeepeek.admin.feature.catalog.api.catalogBrewMethodSlug
import com.coffeepeek.admin.feature.catalog.api.methodLabel
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.semantics.error
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.checkin_rating_coffee
import coffeepeek.composeapp.generated.resources.maskot_with_magnifying_glass
import com.coffeepeek.admin.feature.catalog.api.BrewMethodOption
import com.coffeepeek.admin.feature.coffee.api.CoffeePickerItem
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.component.AppButton
import com.coffeepeek.admin.ui.component.CheckInVisibilityControl
import com.coffeepeek.admin.ui.component.CoffeePeekLoader
import com.coffeepeek.admin.ui.component.CompactOutlinedTextField
import com.coffeepeek.admin.ui.component.CpSearchField
import com.coffeepeek.admin.ui.component.EditableRatingCard
import com.coffeepeek.admin.ui.component.KeyboardAwareForm
import com.coffeepeek.admin.ui.component.brewMethodIcon
import com.coffeepeek.admin.ui.component.NativeDatePicker
import com.coffeepeek.admin.ui.component.PhotoAttachmentsSection
import com.coffeepeek.admin.ui.component.PlatformScrollableColumn
import com.coffeepeek.admin.ui.component.ReviewFormField
import com.coffeepeek.admin.ui.component.ReviewFieldError
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
import com.coffeepeek.feature.homecheckin.domain.HomeCheckInField
import com.coffeepeek.feature.homecheckin.domain.HomeCheckInVisibility
import com.coffeepeek.feature.homecheckin.domain.HomePhotoUpload
import org.jetbrains.compose.resources.painterResource
import kotlin.time.Instant

@Composable
internal fun HomeEditor(state: HomeCheckInUiState, vm: HomeCheckInViewModel, modifier: Modifier) {
    HomeEditorContent(state, { draft -> vm.updateDraft { draft } }, vm::setCatalogQuery,
        vm::searchCatalog, vm::selectCatalogProduct, vm::save, vm::closeEditor, modifier,
        onRetryMethods = vm::loadBrewMethods)
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
    onRetryMethods: () -> Unit = {},
) {
    val draft = state.draft
    val errors = state.fieldErrors
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

    KeyboardAwareForm(modifier) {
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
                onBack = { focus.clearFocus(); keyboard?.hide(); showBeanPicker = false }, modifier = Modifier)
            return@KeyboardAwareForm
        }

        Column(Modifier.fillMaxSize()) {
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
            val summary = if (errors.isNotEmpty()) homeValidationSummary(errors.size) else state.formError
            summary?.let {
                HomeCheckInErrorSummary(it, Modifier.padding(horizontal = CpDimens.spacing4).padding(bottom = CpDimens.spacing4))
            }
            PlatformScrollableColumn(
                Modifier.weight(1f).fillMaxWidth().padding(horizontal = CpDimens.spacing4),
                verticalArrangement = Arrangement.spacedBy(CpDimens.spacing6),
            ) {
                CheckInVisibilityControl(draft.visibility == HomeCheckInVisibility.Public, enabled = !state.saving) {
                    onDraftChange(draft.copy(visibility = if (it) HomeCheckInVisibility.Public else HomeCheckInVisibility.Private))
                }
                errors[HomeCheckInField.Visibility]?.let { ReviewFieldError(it) }
                Column(verticalArrangement = Arrangement.spacedBy(CpDimens.spacing2)) {
                    EditableRatingCard(Res.drawable.checkin_rating_coffee, "Как получился кофе?", draft.tasteScore ?: 0,
                        onRatingChange = { score ->
                            if (!state.saving) onDraftChange(draft.copy(tasteScore = score.takeUnless { it == draft.tasteScore }))
                        })
                    errors[HomeCheckInField.Rating]?.let { ReviewFieldError(it) }
                }
                Column {
                    ReviewFormStep(isError = HomeCheckInField.Date in errors) {
                        ReviewFormField("Дата приготовления", errors[HomeCheckInField.Date]) {
                            HomeSelectField(formatVisitDate(dateMillis), CpIcons.Calendar, "Выбрать дату приготовления",
                                enabled = !state.saving, error = errors[HomeCheckInField.Date], onClick = { showDatePicker = true })
                        }
                    }
                    ReviewFormStep(optional = true, isError = HomeCheckInField.Method in errors) {
                        ReviewFormField("Способ заваривания", errors[HomeCheckInField.Method]) {
                            HomeMethodField(draft.brewMethodCode, state.brewMethods, !state.saving && !state.brewMethodsLoading, errors[HomeCheckInField.Method]) {
                                onDraftChange(draft.copy(brewMethodCode = it))
                            }
                            if (state.brewMethodsLoading) Text("Загружаем способы…", style = MaterialTheme.typography.bodySmall)
                            if (state.brewMethodsError) TextButton(onClick = onRetryMethods) { Text("Не удалось загрузить. Повторить") }
                        }
                    }
                    ReviewFormStep(optional = true, isError = HomeCheckInField.Bean in errors) {
                        ReviewFormField("Зерно", errors[HomeCheckInField.Bean]) {
                            HomeSelectField(draft.customBeanName ?: state.catalogSelectionName ?: "Выбрать зерно",
                                CpIcons.CoffeeBean, "Выбрать зерно", enabled = !state.saving, error = errors[HomeCheckInField.Bean],
                                onClick = { focus.clearFocus(); keyboard?.hide(); showBeanPicker = true })
                        }
                    }
                    ReviewFormStep(optional = true, isError = HomeCheckInField.Dose in errors) {
                        ReviewFormField("Кофе, г", errors[HomeCheckInField.Dose]) {
                            HomeNumberField(draft.doseGrams.orEmpty(), "Например, 18", KeyboardType.Decimal, !state.saving, errors[HomeCheckInField.Dose], "Количество кофе, г") {
                                onDraftChange(draft.copy(doseGrams = it.replace(',', '.').ifBlank { null }))
                            }
                        }
                    }
                    ReviewFormStep(optional = true, isError = HomeCheckInField.Duration in errors) {
                        HomeDurationField(draft.brewDurationSeconds, !state.saving, errors[HomeCheckInField.Duration]) {
                            onDraftChange(draft.copy(brewDurationSeconds = it))
                        }
                    }
                    ReviewFormStep(optional = true, isError = HomeCheckInField.Note in errors) {
                        ReviewFormField("Заметка", errors[HomeCheckInField.Note]) {
                            ReviewTextInput(draft.text.orEmpty(), { if (!state.saving) onDraftChange(draft.copy(text = it.ifBlank { null })) },
                                "Какой вкус? Что хочется повторить?", isError = HomeCheckInField.Note in errors, maxLength = 1000, modifier = Modifier.heightIn(min = 80.dp))
                        }
                    }
                    ReviewFormStep(optional = true, isError = HomeCheckInField.Drink in errors) {
                        ReviewFormField("Напиток", errors[HomeCheckInField.Drink]) {
                            ReviewTextInput(draft.customDrinkName ?: state.selected?.drinkName.takeIf { draft.drinkSlug != null }.orEmpty(),
                                { name -> if (!state.saving) onDraftChange(draft.copy(
                                    drinkSlug = "other".takeIf { name.isNotBlank() }, customDrinkName = name.ifBlank { null })) },
                                "Например, капучино", isError = HomeCheckInField.Drink in errors, singleLine = true, maxLength = 100)
                        }
                    }
                    ReviewFormStep(optional = true, last = true, isError = HomeCheckInField.Photos in errors) {
                        errors[HomeCheckInField.Photos]?.let { ReviewFieldError(it) }
                        if (state.selected == null) {
                            PhotoAttachmentsSection(
                                photos = draft.photos.map { PickedImage(it.bytes, it.fileName, it.contentType) }, maxPhotos = 5,
                                onPhotosAdded = { added -> if (!state.saving) onDraftChange(draft.copy(
                                    photos = (draft.photos + added.map { HomePhotoUpload(it.fileName, it.contentType, it.bytes) }).take(5))) },
                                onRemovePhoto = { index -> if (!state.saving) {
                                    onDraftChange(draft.copy(photos = draft.photos.filterIndexed { i, _ -> i != index }))
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
                AppButton(if (state.saving) "Сохраняем…" else "Сохранить чекин", onClick = {
                    keyboard?.hide(); focus.clearFocus(force = true); onSave()
                }, enabled = !state.saving)
            }
        }
    }
}

@Composable
private fun HomeSelectField(value: String, icon: ImageVector, action: String, enabled: Boolean,
    methodName: String? = null, error: String? = null, onClick: () -> Unit) {
    val shape = RoundedCornerShape(CpDimens.radiusMd)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(if (error != null) MaterialTheme.colorScheme.error.copy(alpha = 0.05f)
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
            .border(1.dp, if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline, shape).clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = action; error?.let { error(it) } }
            .heightIn(min = CpDimens.controlHeight).padding(horizontal = 14.dp, vertical = CpDimens.spacing3),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing2),
    ) {
        if (methodName != null) Icon(painterResource(brewMethodIcon(methodName)), null, Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
        else Icon(icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Icon(CpIcons.ChevronUpDown, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun HomeNumberField(value: String, placeholder: String, type: KeyboardType, enabled: Boolean,
    error: String? = null, label: String = placeholder, maximum: Int? = null, onChange: (String) -> Unit) {
    CompactOutlinedTextField(value, onChange, enabled = enabled, isError = error != null,
        modifier = Modifier.fillMaxWidth().heightIn(min = CpDimens.controlHeight)
            .semantics { contentDescription = label; error?.let { error(it) } },
        shape = RoundedCornerShape(CpDimens.inputRadius), textStyle = MaterialTheme.typography.bodyMedium,
        placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        keyboardOptions = KeyboardOptions(keyboardType = type), maxLength = 8,
        inputFilter = { it.isDigit() || type == KeyboardType.Decimal && (it == '.' || it == ',') },
        inputValidator = { maximum == null || it.isEmpty() || it.toIntOrNull() in 0..maximum },
        colors = OutlinedTextFieldDefaults.colors(
            errorContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.05f),
            errorBorderColor = MaterialTheme.colorScheme.error,
            focusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
            unfocusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
            focusedBorderColor = MaterialTheme.colorScheme.outline, unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        ))
}

@Composable
private fun HomeMethodField(selected: String?, methods: List<BrewMethodOption>, enabled: Boolean, error: String?, onSelect: (String?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selectedMethod = methods.firstOrNull { it.slug == selected?.catalogBrewMethodSlug() }
    val selectedName = if (error != null && selectedMethod == null) null else selectedMethod?.name ?: selected?.methodLabel()
    Box {
        HomeSelectField(selectedName ?: "Выбрать способ", CpIcons.Coffee, "Выбрать способ заваривания", enabled,
            methodName = selectedName, error = error) { expanded = true }
        DropdownMenu(expanded, onDismissRequest = { expanded = false },
            modifier = Modifier.widthIn(min = 240.dp, max = 320.dp).heightIn(max = 360.dp),
            shape = RoundedCornerShape(CpDimens.radiusMd), containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
            DropdownMenuItem(text = { Text("Не указывать") }, onClick = { onSelect(null); expanded = false })
            methods.forEach { method ->
                DropdownMenuItem(text = { Text(method.name, style = MaterialTheme.typography.bodyMedium) },
                    onClick = { onSelect(method.slug); expanded = false },
                    leadingIcon = { Icon(painterResource(brewMethodIcon(method.name)), null, Modifier.size(24.dp)) },
                    trailingIcon = { if (method == selectedMethod) Icon(CpIcons.Check, null, Modifier.size(18.dp)) })
            }
        }
    }
}

@Composable
private fun HomeBeanPicker(state: HomeCheckInUiState, onQueryChange: (String) -> Unit, onSearch: () -> Unit,
    onProductSelected: (CoffeePickerItem) -> Unit, onCustomName: (String?) -> Unit, onBack: () -> Unit, modifier: Modifier) {
    var customName by remember { mutableStateOf(state.draft.customBeanName.orEmpty()) }
    LaunchedEffect(Unit) { onSearch() }
    Column(modifier.fillMaxSize().padding(horizontal = CpDimens.spacing4),
        verticalArrangement = Arrangement.spacedBy(CpDimens.spacing4)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(CpDimens.controlHeight)) { Icon(CpIcons.Back, "Назад к приготовлению") }
            Text("Зерно", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
        }
        CpSearchField(state.catalogQuery, onQueryChange, "Название или обжарщик", Modifier.fillMaxWidth(), onSearch = onSearch)
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            when {
                state.catalogLoading -> CoffeePeekLoader()
                state.catalogError != null -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Не удалось загрузить зерно", color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = onSearch) { Text("Повторить") }
                }
                state.catalogItems.isEmpty() -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(CpDimens.spacing3),
                ) {
                    Image(painterResource(Res.drawable.maskot_with_magnifying_glass), null, Modifier.size(120.dp))
                    Text("Зерно не найдено", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                    Text("Попробуйте изменить поиск или добавьте своё зерно ниже.",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center)
                }
                else -> LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(CpDimens.spacing2)) {
                    items(state.catalogItems, key = { it.slug }) { product ->
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(CpDimens.radiusMd))
                            .clickable(role = Role.Button) { onProductSelected(product) }
                            .padding(vertical = CpDimens.spacing3, horizontal = CpDimens.spacing2)) {
                            Text(product.name, style = MaterialTheme.typography.bodyLarge)
                            Text(product.roasterName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    }
                }
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

@Composable
private fun HomeDurationField(duration: Int?, enabled: Boolean, error: String?, onChange: (Int?) -> Unit) {
    var minutes by remember { mutableStateOf(duration?.div(60)?.takeIf { it > 0 }?.toString().orEmpty()) }
    var seconds by remember { mutableStateOf(duration?.rem(60)?.takeIf { it > 0 }?.toString().orEmpty()) }
    ReviewFormField("Время заваривания", error) {
        Row(horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing3)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(CpDimens.spacing1)) {
                Text("Минуты", style = MaterialTheme.typography.bodySmall)
                HomeNumberField(minutes, "0", KeyboardType.Number, enabled, error, "Минуты заваривания", maximum = 10080) { value ->
                    if (value.isEmpty() || value.toIntOrNull() in 0..10080) {
                        minutes = value
                        onChange(homeBrewDurationSeconds(minutes, seconds))
                    }
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(CpDimens.spacing1)) {
                Text("Секунды", style = MaterialTheme.typography.bodySmall)
                HomeNumberField(seconds, "00", KeyboardType.Number, enabled, error, "Секунды заваривания", maximum = 59) { value ->
                    if (value.isEmpty() || value.toIntOrNull() in 0..59) {
                        seconds = value
                        onChange(homeBrewDurationSeconds(minutes, seconds))
                    }
                }
            }
        }
    }
}

internal fun homeBrewDurationSeconds(minutes: String, seconds: String): Int? =
    if (minutes.isBlank() && seconds.isBlank()) null
    else (minutes.toIntOrNull() ?: 0) * 60 + (seconds.toIntOrNull() ?: 0)

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
