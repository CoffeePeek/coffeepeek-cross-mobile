package com.coffeepeek.admin.ui.screen.addshop

import com.coffeepeek.admin.ui.icons.CpIcons
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.component.CompactOutlinedTextField
import com.coffeepeek.admin.theme.CpColor
import com.coffeepeek.admin.ui.component.ActionRow
import com.coffeepeek.admin.ui.component.CheckmarkRow
import com.coffeepeek.admin.ui.component.CheckmarkSection
import com.coffeepeek.admin.ui.component.GroupSection
import com.coffeepeek.admin.ui.component.RowSeparator
import com.coffeepeek.admin.ui.component.StepperRow
import com.coffeepeek.admin.ui.component.SwitchRow
import com.coffeepeek.admin.ui.component.PriceBeanSlider
import com.coffeepeek.admin.ui.component.priceLevelValue
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.AppButton
import com.coffeepeek.admin.ui.component.CoffeePeekLoader
import com.coffeepeek.admin.ui.component.CpCircularBackButton
import com.coffeepeek.admin.ui.component.brewMethodIcon
import com.coffeepeek.admin.ui.component.PhotoAttachmentsSection
import com.coffeepeek.admin.ui.component.RoasterAvatar
import com.coffeepeek.admin.utils.MAX_MENU_PHOTOS
import com.coffeepeek.admin.utils.MAX_SHOP_PHOTOS
import com.coffeepeek.admin.utils.PickedImage
import com.coffeepeek.admin.location.LocationPermissionEffect
import org.jetbrains.compose.resources.painterResource
import com.coffeepeek.domain.model.City
import com.coffeepeek.admin.di.platformViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddShopScreen(vm: AddShopViewModel = platformViewModel()) {
    val state by vm.state.collectAsState()

    if (state.isSuccess) {
        AddShopSuccessScreen(
            name = state.name.trim().ifBlank { "Кофейня" },
            cityName = state.selectedCity?.name,
            address = state.address.trim(),
            coverPhoto = state.photos.firstOrNull()?.bytes,
            onGoHome = vm::onSuccessDismiss,
        )
        return
    }

    LocationPermissionEffect(
        requestKey = 1,
        onGranted = { vm.fillAddressFromCurrentLocation(force = false) },
        onDenied = vm::onLocationPermissionDenied,
    )

    if (state.showLocationPicker) {
        PickShopLocationDialog(
            initialPoint = state.selectedGeoPoint,
            onConfirm = vm::onLocationPicked,
            onDismiss = vm::dismissLocationPicker,
        )
    }

    // Диалог ошибки
    state.submitError?.let { err ->
        AlertDialog(
            onDismissRequest = vm::clearSubmitError,
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Ошибка", style = MaterialTheme.typography.headlineSmall) },
            text = { Text(err, style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(onClick = vm::clearSubmitError) { Text("Понятно") }
            },
            shape = RoundedCornerShape(CpDimens.radius2xl),
        )
    }

    Scaffold(
        topBar = {
            AddShopHeader(
                title = when (state.currentStep) {
                    AddShopStep.BASIC    -> "Основная информация"
                    AddShopStep.CONTACTS -> "Контакты"
                    AddShopStep.PHOTOS   -> "Фотографии"
                    AddShopStep.FEATURES -> "Оборудование"
                    AddShopStep.SCHEDULE -> "Расписание"
                },
                currentIndex = state.currentStep.ordinal,
                total = AddShopStep.entries.size,
                onBack = {
                    if (state.currentStep == AddShopStep.BASIC) Navigator.popBack()
                    else vm.prevStep()
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (state.isLoadingCatalogs) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CoffeePeekLoader()
            }
            return@Scaffold
        }

        if (state.catalogsError != null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.catalogsError ?: "Ошибка", color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(CpDimens.spacing3))
                    Button(
                        onClick = { vm.loadCatalogs() },
                        modifier = Modifier.height(CpDimens.buttonHeight),
                        shape = RoundedCornerShape(percent = 50),
                    ) { Text("Повторить") }
                }
            }
            return@Scaffold
        }

        AnimatedContent(
            targetState = state.currentStep,
            transitionSpec = {
                val forward = targetState.ordinal > initialState.ordinal
                (slideInHorizontally { if (forward) it else -it }) togetherWith
                    (slideOutHorizontally { if (forward) -it else it })
            },
            label = "step",
        ) { step ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(CpDimens.spacing4),
            ) {
                when (step) {
                    AddShopStep.BASIC    -> StepBasic(state = state, vm = vm)
                    AddShopStep.CONTACTS -> StepContacts(state, vm)
                    AddShopStep.PHOTOS   -> StepPhotos(state = state, vm = vm)
                    AddShopStep.FEATURES -> StepFeatures(state, vm)
                    AddShopStep.SCHEDULE -> StepSchedule(state, vm)
                }

                Spacer(Modifier.height(CpDimens.spacing6))

                AppButton(
                    text = when (step) {
                        AddShopStep.SCHEDULE -> "Отправить на модерацию"
                        else -> "Далее"
                    },
                    onClick = vm::nextStep,
                    enabled = !state.isSubmitting && when (step) {
                        AddShopStep.BASIC -> state.step1Valid
                        AddShopStep.CONTACTS -> state.contactsStepValid
                        else -> true
                    },
                )

                if (state.isSubmitting) {
                    Spacer(Modifier.height(CpDimens.spacing3))
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CoffeePeekLoader(
                            size = CpDimens.loaderButton,
                            strokeWidth = 2.dp,
                        )
                    }
                }

                Spacer(Modifier.height(CpDimens.spacing8))
            }
        }
    }
}

@Composable
private fun AddShopHeader(
    title: String,
    currentIndex: Int,
    total: Int,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = CpDimens.spacing5, vertical = CpDimens.spacing4),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(CpDimens.buttonHeight),
            contentAlignment = Alignment.Center,
        ) {
            CpCircularBackButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 44.dp),
            )
        }
        Spacer(Modifier.height(CpDimens.spacing4))
        AddShopStepDots(
            currentIndex = currentIndex,
            total = total,
        )
    }
}

@Composable
private fun AddShopStepDots(
    currentIndex: Int,
    total: Int,
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(total) { index ->
            val active = index == currentIndex
            Box(
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .height(10.dp)
                    .width(if (active) 28.dp else 10.dp)
                    .clip(CircleShape)
                    .background(
                        if (active) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f),
                    ),
            )
        }
    }
}

// ── Шаг 1: Основное ───────────────────────────────────────────────────────────

@Composable
private fun StepBasic(
    state: AddShopUiState,
    vm: AddShopViewModel,
) {
    StepLegend(requiredHint = "Поля со * обязательны для заполнения")
    Spacer(Modifier.height(CpDimens.spacing3))

    FormField(label = "Название кофейни", required = true) {
        AppOutlinedField(
            value = state.name,
            onValueChange = vm::onNameChange,
            maxLength = 55,
            placeholder = "Например, Surf Coffee",
            errorText = if (state.name.isNotEmpty()) state.nameError else null,
            counter = "${state.name.length}/55",
        )
    }

    Spacer(Modifier.height(CpDimens.spacing4))

    FormField(label = "Описание", optional = true) {
        AppOutlinedField(
            value = state.description,
            onValueChange = vm::onDescriptionChange,
            placeholder = "Расскажите о концепции, атмосфере и фишках кофейни…",
            minLines = 3,
            maxLines = 6,
        )
    }

    Spacer(Modifier.height(CpDimens.spacing4))

    FormField(label = "Город", required = true) {
        when {
            state.cities.size == 1 -> {
                SingleValueField(value = state.cities.first().name)
            }
            else -> CityDropdown(
                cities = state.cities,
                selected = state.selectedCity,
                onSelect = vm::onCitySelect,
                error = if (state.selectedCity == null && state.name.isNotEmpty()) state.cityError else null,
            )
        }
    }

    Spacer(Modifier.height(CpDimens.spacing4))

    FormField(label = "Адрес", required = true) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing2),
            verticalAlignment = Alignment.Top,
        ) {
            AppOutlinedField(
                value = state.address,
                onValueChange = vm::onAddressChange,
                placeholder = "Улица, дом, корпус",
                errorText = if (state.address.isNotEmpty()) state.addressError else null,
                leadingIcon = CpIcons.Navigation,
                modifier = Modifier.weight(1f),
            )
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                FilledIconButton(
                    onClick = vm::openLocationPicker,
                    enabled = !state.isResolvingAddress,
                    modifier = Modifier.size(CpDimens.buttonHeight),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f),
                    ),
                ) {
                    if (state.isResolvingAddress) {
                        CoffeePeekLoader(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            imageVector = CpIcons.Map,
                            contentDescription = "Выбрать на карте",
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
        state.locationHint?.let { hint ->
            Spacer(Modifier.height(CpDimens.spacing2))
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    Spacer(Modifier.height(CpDimens.spacing4))

    val selectedPrice = priceLevelValue(state.priceRange)
    PriceBeanSlider(
        selected = state.priceRange,
        onSelect = vm::onPriceRangeSelect,
        title = selectedPrice?.let { "Цена (капучино за $it)" } ?: "Цена",
        showHint = false,
    )
}

// ── Шаг 2: Фото ───────────────────────────────────────────────────────────────

@Composable
private fun StepPhotos(
    state: AddShopUiState,
    vm: AddShopViewModel,
) {
    StepLegend(optional = true)
    PhotoAttachmentsSection(
        photos = state.photos.map { PickedImage(it.bytes, it.fileName, it.contentType) },
        maxPhotos = MAX_SHOP_PHOTOS,
        onPhotosAdded = vm::addPhotos,
        onRemovePhoto = vm::removePhoto,
        title = "Фотографии кофейни",
        hint = "До $MAX_SHOP_PHOTOS фотографий (необязательно). Можно выбрать несколько сразу.",
    )

    Spacer(Modifier.height(CpDimens.spacing6))
    PhotoAttachmentsSection(
        photos = state.menuPhotos.map { PickedImage(it.bytes, it.fileName, it.contentType) },
        maxPhotos = MAX_MENU_PHOTOS,
        onPhotosAdded = vm::addMenuPhotos,
        onRemovePhoto = vm::removeMenuPhoto,
        title = "Фото меню",
        hint = "До $MAX_MENU_PHOTOS фото меню (необязательно). Не попадут в галерею кофейни.",
    )
}

// ── Шаг 3: Расписание ─────────────────────────────────────────────────────────

private val SCHEDULE_PRESETS = listOf("08:00" to "20:00", "09:00" to "21:00", "10:00" to "22:00")

@Composable
private fun StepSchedule(state: AddShopUiState, vm: AddShopViewModel) {
    StepLegend(optional = true)
    Text(
        text = "По умолчанию кофейня открыта каждый день.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = CpDimens.spacing4),
    )

    Column(verticalArrangement = Arrangement.spacedBy(CpDimens.spacing6)) {
        GroupSection(
            title = "Режим",
            footer = "Включите, если в будни и выходные разные часы.",
        ) {
            SwitchRow(
                label = "Разное время по дням",
                checked = state.usePerDaySchedule,
                onCheckedChange = vm::setUsePerDaySchedule,
            )
        }

        if (!state.usePerDaySchedule) {
            GroupSection(title = "Часы работы") {
                TimeStepperRow("Открытие", state.unifiedOpenTime) { vm.adjustUnifiedOpen(it) }
                RowSeparator()
                TimeStepperRow("Закрытие", state.unifiedCloseTime) { vm.adjustUnifiedClose(it) }
            }

            GroupSection(title = "Типовые часы") {
                SCHEDULE_PRESETS.forEachIndexed { index, (open, close) ->
                    if (index > 0) RowSeparator()
                    CheckmarkRow(
                        label = "$open – $close",
                        checked = state.unifiedOpenTime == open && state.unifiedCloseTime == close,
                        onToggle = { vm.applySchedulePreset(open, close) },
                    )
                }
            }

            GroupSection(
                title = "Дни работы",
                trailing = "${7 - state.closedDays.size} из 7",
                footer = "Снимите отметку с дней, когда кофейня закрыта.",
            ) {
                state.schedules.forEachIndexed { index, day ->
                    if (index > 0) RowSeparator()
                    CheckmarkRow(
                        label = day.label,
                        checked = day.dayOfWeek !in state.closedDays,
                        onToggle = { vm.toggleClosedDay(day.dayOfWeek) },
                    )
                }
            }
        } else {
            state.schedules.forEach { day ->
                GroupSection(title = day.label) {
                    SwitchRow(
                        label = "Открыто",
                        checked = !day.isClosed,
                        onCheckedChange = { open ->
                            vm.updateSchedule(day.dayOfWeek) { it.copy(isClosed = !open) }
                        },
                    )
                    if (!day.isClosed) {
                        RowSeparator()
                        TimeStepperRow("Открытие", day.openTime) { vm.adjustDayOpenTime(day.dayOfWeek, it) }
                        RowSeparator()
                        TimeStepperRow("Закрытие", day.closeTime) { vm.adjustDayCloseTime(day.dayOfWeek, it) }
                    }
                }
            }
        }
    }
}

// ponytail: ±30 min stepper; swap for an iOS-style wheel time picker if 30-min granularity is too coarse.
@Composable
private fun TimeStepperRow(label: String, value: String, onAdjust: (Int) -> Unit) {
    StepperRow(
        label = label,
        value = value,
        onDecrease = { onAdjust(-30) },
        onIncrease = { onAdjust(30) },
        decreaseDescription = "$label на 30 минут раньше",
        increaseDescription = "$label на 30 минут позже",
    )
}

@Composable
private fun StepLegend(
    requiredHint: String? = null,
    optional: Boolean = false,
) {
    when {
        requiredHint != null -> Text(
            text = requiredHint,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
        )
        optional -> Text(
            text = "Необязательно — можно пропустить",
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = CpDimens.spacing1),
        )
    }
}

// ── Шаг 4: Контакты ───────────────────────────────────────────────────────────

@Composable
private fun StepContacts(state: AddShopUiState, vm: AddShopViewModel) {
    StepLegend(optional = true)
    Text(
        text = "Заполните те поля, которые актуальны для вашего заведения.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = CpDimens.spacing4),
    )

    FormField(label = "Номер телефона", optional = true) {
        AppOutlinedField(
            value = state.phone,
            onValueChange = vm::onPhoneChange,
            placeholder = "+375",
            keyboardType = KeyboardType.Phone,
            errorText = if (state.phone.isNotEmpty()) state.phoneError else null,
            leadingIcon = CpIcons.Phone,
        )
    }
    Spacer(Modifier.height(CpDimens.spacing3))
    FormField(label = "Instagram профиль", optional = true) {
        AppOutlinedField(
            value = state.instagram,
            onValueChange = vm::onInstagramChange,
            placeholder = "@",
            errorText = if (state.instagram.isNotEmpty()) state.instagramError else null,
            leadingIcon = CpIcons.Instagram,
        )
    }
    Spacer(Modifier.height(CpDimens.spacing3))
    FormField(label = "Веб-сайт", optional = true) {
        AppOutlinedField(
            value = state.website,
            onValueChange = vm::onWebsiteChange,
            placeholder = "mycoffee.by",
            keyboardType = KeyboardType.Uri,
            errorText = if (state.website.isNotEmpty()) state.websiteError else null,
            leadingIcon = CpIcons.Globe,
        )
    }
    Spacer(Modifier.height(CpDimens.spacing3))
    FormField(label = "Email", optional = true) {
        AppOutlinedField(
            value = state.email,
            onValueChange = vm::onEmailChange,
            placeholder = "info@coffee.by",
            keyboardType = KeyboardType.Email,
            errorText = if (state.email.isNotEmpty()) state.emailError else null,
            leadingIcon = CpIcons.Email,
        )
    }
}

// ── Шаг 3: Особенности ────────────────────────────────────────────────────────

@Composable
private fun StepFeatures(state: AddShopUiState, vm: AddShopViewModel) {
    StepLegend(optional = true)
    Text(
        text = "Отметьте то, что есть в заведении.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = CpDimens.spacing4),
    )

    Column(verticalArrangement = Arrangement.spacedBy(CpDimens.spacing6)) {
        CheckmarkSection(
            title = "Методы приготовления",
            items = state.brewMethods,
            selectedIds = state.selectedBrewMethodIds,
            onToggle = vm::toggleBrewMethod,
            leading = { item ->
                Icon(
                    painter = painterResource(brewMethodIcon(item.name)),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
            },
        )
        CheckmarkSection(
            title = "Зерно",
            items = state.beans,
            selectedIds = state.selectedBeanIds,
            onToggle = vm::toggleBean,
        )
        CheckmarkSection(
            title = "Обжарщики",
            items = state.roasters,
            selectedIds = state.selectedRoasterIds,
            onToggle = vm::toggleRoaster,
            footer = "Не нашли обжарщика? Добавьте его — он появится в списке после проверки.",
            leading = { item -> RoasterAvatar(item) },
            extraRows = {
                ActionRow(
                    label = "Добавить обжарщика",
                    icon = CpIcons.Add,
                    onClick = { Navigator.navigate(Navigator.Screen.AddRoaster) },
                )
            },
        )
        CheckmarkSection(
            title = "Оборудование",
            items = state.equipment,
            selectedIds = state.selectedEquipmentIds,
            onToggle = vm::toggleEquipment,
        )
    }
}

// ── Переиспользуемые компоненты ───────────────────────────────────────────────

@Composable
private fun FormField(
    label: String,
    required: Boolean = false,
    optional: Boolean = false,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier.padding(bottom = CpDimens.spacing2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing1),
    ) {
        Text(
            text = if (required) "$label *" else label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (optional) {
            Text(
                text = "необязательно",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }
    }
    content()
}

@Composable
private fun AppOutlinedField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    maxLength: Int = Int.MAX_VALUE,
    modifier: Modifier = Modifier,
    errorText: String? = null,
    counter: String? = null,
    minLines: Int = 1,
    maxLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text,
    leadingIcon: ImageVector? = null,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    Column(modifier = modifier) {
        val singleLine = maxLines <= 1
        CompactOutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            maxLength = maxLength,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (singleLine) Modifier.height(CpDimens.buttonHeight) else Modifier),
            shape = if (singleLine) {
                RoundedCornerShape(percent = 50)
            } else {
                RoundedCornerShape(CpDimens.buttonRadius)
            },
            placeholder = {
                Text(
                    placeholder,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            textStyle = MaterialTheme.typography.bodyLarge,
            isError = errorText != null,
            singleLine = maxLines <= 1,
            contentPadding = if (singleLine) {
                CpDimens.singleLineFieldContentPadding
            } else {
                OutlinedTextFieldDefaults.contentPadding()
            },
            minLines = minLines,
            maxLines = maxLines,
            leadingIcon = leadingIcon?.let { icon ->
                {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            trailingIcon = trailingContent?.let { content ->
                { content() }
            },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = keyboardType,
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = MaterialTheme.colorScheme.outline,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                errorBorderColor     = MaterialTheme.colorScheme.error,
                focusedContainerColor   = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            ),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp, start = 4.dp, end = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            if (errorText != null) {
                Text(
                    text = errorText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            } else Spacer(Modifier.width(0.dp))
            if (counter != null) {
                Text(
                    text = counter,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SingleValueField(value: String) {
    CompactOutlinedTextField(
        value = value,
        onValueChange = {},
        readOnly = true,
        modifier = Modifier.fillMaxWidth().height(CpDimens.buttonHeight),
        textStyle = MaterialTheme.typography.bodyLarge,
        contentPadding = CpDimens.singleLineFieldContentPadding,
        shape = RoundedCornerShape(percent = 50),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.outline,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            disabledBorderColor = MaterialTheme.colorScheme.outline,
            disabledTextColor = MaterialTheme.colorScheme.onSurface,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        ),
        enabled = false,
    )
}

@Composable
private fun CityDropdown(
    cities: List<City>,
    selected: City?,
    onSelect: (City) -> Unit,
    error: String? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    val menuShape = RoundedCornerShape(CpDimens.selectRadius)
    Box {
        CompactOutlinedTextField(
            value = selected?.name ?: "",
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.fillMaxWidth().height(CpDimens.buttonHeight).clickable { expanded = true },
            placeholder = {
                Text(
                    "Выберите город",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            textStyle = MaterialTheme.typography.bodyLarge,
            trailingIcon = {
                Icon(
                    CpIcons.ChevronUpDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            isError = error != null,
            shape = RoundedCornerShape(percent = 50),
            contentPadding = CpDimens.singleLineFieldContentPadding,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = MaterialTheme.colorScheme.outline,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                errorBorderColor     = MaterialTheme.colorScheme.error,
                focusedContainerColor   = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                disabledBorderColor  = MaterialTheme.colorScheme.outline,
                disabledTextColor    = MaterialTheme.colorScheme.onSurface,
            ),
            enabled = false,
        )
        // Invisible clickable overlay
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { expanded = true }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .clip(menuShape)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, menuShape),
            shape = menuShape,
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
        ) {
            cities.forEach { city ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = city.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (city.id == selected?.id)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface,
                        )
                    },
                    onClick = {
                        onSelect(city)
                        expanded = false
                    },
                    leadingIcon = if (city.id == selected?.id) {
                        { Icon(CpIcons.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) }
                    } else null,
                )
            }
        }
    }
    if (error != null) {
        Text(
            text = error,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(top = 4.dp, start = 4.dp),
        )
    }
}
