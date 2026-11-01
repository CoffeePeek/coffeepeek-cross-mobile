package com.coffeepeek.admin.ui.screen.addshop

import com.coffeepeek.admin.ui.component.inputValidationMessage

import com.coffeepeek.admin.ui.screen.contributions.contributionFailureMessage

import com.coffeepeek.admin.ui.screen.contributions.contributionFieldMessage

import coffeepeek.composeapp.generated.resources.*

import com.coffeepeek.core.designsystem.component.CoffeePeekScaffold as Scaffold

import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.material3.Surface

import com.coffeepeek.core.designsystem.component.CoffeePeekSelectField
import com.coffeepeek.core.designsystem.component.CoffeePeekSelectOption
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.select_city_placeholder
import coffeepeek.composeapp.generated.resources.select_city_label
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

import androidx.compose.runtime.getValue
import com.coffeepeek.admin.ui.component.PlatformScrollableColumn
import com.coffeepeek.admin.ui.component.SubmissionStatus
import com.coffeepeek.admin.ui.component.SubmissionStatusScreen
import com.coffeepeek.admin.ui.component.SubmissionErrorActions
import com.coffeepeek.admin.ui.icons.CpIcons
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.coffeepeek.core.designsystem.component.CoffeePeekFilledButton as Button
import androidx.compose.material3.ExperimentalMaterial3Api
import com.coffeepeek.core.designsystem.component.CoffeePeekFilledIconButton as FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
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
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.component.CompactOutlinedTextField
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
    val localizedLegacyReviewContentOwnershipKindShop = stringResource(Res.string.legacy_review_content_ownership_kind_shop)
    val localizedCatalogShopSuggestionAction = stringResource(Res.string.catalog_shop_suggestion_action)
    val localizedLegacyAddShopScreenContributionsShopFailedTitle = stringResource(Res.string.legacy_add_shop_screen_contributions_shop_failed_title)
    val localizedLegacyAddShopScreenContributionsShopSendingTitle = stringResource(Res.string.legacy_add_shop_screen_contributions_shop_sending_title)
    val localizedLegacyAddShopScreenContributionsShopSendingDescription = stringResource(Res.string.legacy_add_shop_screen_contributions_shop_sending_description)
    val localizedLegacyAddShopScreenContributionsShopStepBasic = stringResource(Res.string.legacy_add_shop_screen_contributions_shop_step_basic)
    val localizedLegacyAddShopScreenContributionsSectionContacts = stringResource(Res.string.legacy_add_shop_screen_contributions_section_contacts)
    val localizedLegacyPhotoAttachmentsSectionContributionsSectionPhotos = stringResource(Res.string.legacy_photo_attachments_section_contributions_section_photos)
    val localizedShopFiltersEquipment = stringResource(Res.string.shop_filters_equipment)
    val localizedLegacyAddShopScreenContributionsShopStepSchedule = stringResource(Res.string.legacy_add_shop_screen_contributions_shop_step_schedule)
    val localizedLegacyAddShopScreenSandboxCitiesFailure = stringResource(Res.string.legacy_add_shop_screen_sandbox_cities_failure)
    val localizedHistoryHomeRetry = stringResource(Res.string.history_home_retry)
    val localizedLegacyAddShopScreenContributionsShopSubmit = stringResource(Res.string.legacy_add_shop_screen_contributions_shop_submit)
    val localizedLegacyAddShopScreenSandboxNext = stringResource(Res.string.legacy_add_shop_screen_sandbox_next)

    val state by vm.state.collectAsState()

    if (state.isSuccess) {
        AddShopSuccessScreen(
            name = state.name.trim().ifBlank { localizedLegacyReviewContentOwnershipKindShop },
            cityName = state.selectedCity?.name,
            address = state.address.trim(),
            coverPhoto = state.photos.firstOrNull()?.bytes,
            onGoHome = vm::onSuccessDismiss,
        )
        return
    }

    if (state.isSubmitting || state.submitError != null) {
        val failed = state.submitError != null
        SubmissionStatusScreen(
            screenTitle = localizedCatalogShopSuggestionAction,
            status = if (failed) SubmissionStatus.Error else SubmissionStatus.Sending,
            title = if (failed) localizedLegacyAddShopScreenContributionsShopFailedTitle else localizedLegacyAddShopScreenContributionsShopSendingTitle,
            description = contributionFailureMessage(state.submitError)?.let { stringResource(Res.string.legacy_submission_error_kept, it) }
                ?: localizedLegacyAddShopScreenContributionsShopSendingDescription,
            onBack = if (failed) vm::clearSubmitError else Navigator::popBack,
        ) {
            if (failed) SubmissionErrorActions(vm::nextStep, vm::clearSubmitError)
        }
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

    Scaffold(
        topBar = {
            AddShopHeader(
                title = when (state.currentStep) {
                    AddShopStep.BASIC    -> localizedLegacyAddShopScreenContributionsShopStepBasic
                    AddShopStep.CONTACTS -> localizedLegacyAddShopScreenContributionsSectionContacts
                    AddShopStep.PHOTOS   -> localizedLegacyPhotoAttachmentsSectionContributionsSectionPhotos
                    AddShopStep.FEATURES -> localizedShopFiltersEquipment
                    AddShopStep.SCHEDULE -> localizedLegacyAddShopScreenContributionsShopStepSchedule
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
                    Text(contributionFailureMessage(state.catalogsError) ?: localizedLegacyAddShopScreenSandboxCitiesFailure, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(CpDimens.spacing3))
                    Button(
                        onClick = { vm.loadCatalogs() },
                        modifier = Modifier.height(CpDimens.buttonHeight),
                        shape = RoundedCornerShape(percent = 50),
                    ) { Text(localizedHistoryHomeRetry) }
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
            PlatformScrollableColumn(
                reserveBottomClearance = true,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .consumeWindowInsets(padding)
                    .padding(horizontal = CpDimens.spacing4),
            ) {
                Spacer(Modifier.height(CpDimens.spacing4))
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
                        AddShopStep.SCHEDULE -> localizedLegacyAddShopScreenContributionsShopSubmit
                        else -> localizedLegacyAddShopScreenSandboxNext
                    },
                    onClick = vm::nextStep,
                    enabled = !state.isSubmitting && when (step) {
                        AddShopStep.BASIC -> state.step1Valid
                        AddShopStep.CONTACTS -> state.contactsStepValid
                        else -> true
                    },
                )

                Spacer(Modifier.height(CpDimens.spacing8 + CpDimens.spacing4))
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
    val localizedLegacyAddShopScreenContributionsShopRequiredHint = stringResource(Res.string.legacy_add_shop_screen_contributions_shop_required_hint)
    val localizedLegacyAddShopScreenContributionsShopNameLabel = stringResource(Res.string.legacy_add_shop_screen_contributions_shop_name_label)
    val localizedLegacyAddShopScreenContributionsShopNamePlaceholder = stringResource(Res.string.legacy_add_shop_screen_contributions_shop_name_placeholder)
    val localizedCheckinDescriptionLabel = stringResource(Res.string.checkin_description_label)
    val localizedLegacyAddShopScreenContributionsShopDescriptionPlaceholder = stringResource(Res.string.legacy_add_shop_screen_contributions_shop_description_placeholder)
    val localizedSelectCityLabel = stringResource(Res.string.select_city_label)
    val localizedLegacyAddShopScreenOwnershipAddress = stringResource(Res.string.legacy_add_shop_screen_ownership_address)
    val localizedLegacyAddShopScreenContributionsShopAddressPlaceholder = stringResource(Res.string.legacy_add_shop_screen_contributions_shop_address_placeholder)
    val localizedLegacyAddShopScreenContributionsShopPickOnMap = stringResource(Res.string.legacy_add_shop_screen_contributions_shop_pick_on_map)
    val localizedShopFiltersPrice = stringResource(Res.string.shop_filters_price)

    StepLegend(requiredHint = localizedLegacyAddShopScreenContributionsShopRequiredHint)
    Spacer(Modifier.height(CpDimens.spacing3))

    FormField(label = localizedLegacyAddShopScreenContributionsShopNameLabel, required = true) {
        AppOutlinedField(
            value = state.name,
            onValueChange = vm::onNameChange,
            maxLength = 55,
            placeholder = localizedLegacyAddShopScreenContributionsShopNamePlaceholder,
            errorText = if (state.name.isNotEmpty()) contributionFieldMessage(state.nameError) else null,
            counter = "${state.name.length}/55",
        )
    }

    Spacer(Modifier.height(CpDimens.spacing4))

    FormField(label = localizedCheckinDescriptionLabel, optional = true) {
        AppOutlinedField(
            value = state.description,
            onValueChange = vm::onDescriptionChange,
            placeholder = localizedLegacyAddShopScreenContributionsShopDescriptionPlaceholder,
            minLines = 3,
            maxLines = 6,
        )
    }

    Spacer(Modifier.height(CpDimens.spacing4))

    FormField(label = localizedSelectCityLabel, required = true) {
        when {
            state.cities.size == 1 -> {
                SingleValueField(value = state.cities.first().name)
            }
            else -> CityDropdown(
                cities = state.cities,
                selected = state.selectedCity,
                onSelect = vm::onCitySelect,
                error = if (state.selectedCity == null && state.name.isNotEmpty()) contributionFieldMessage(state.cityError) else null,
            )
        }
    }

    Spacer(Modifier.height(CpDimens.spacing4))

    FormField(label = localizedLegacyAddShopScreenOwnershipAddress, required = true) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing2),
            verticalAlignment = Alignment.Top,
        ) {
            AppOutlinedField(
                value = state.address,
                onValueChange = vm::onAddressChange,
                placeholder = localizedLegacyAddShopScreenContributionsShopAddressPlaceholder,
                errorText = if (state.address.isNotEmpty()) contributionFieldMessage(state.addressError) else null,
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
                            contentDescription = localizedLegacyAddShopScreenContributionsShopPickOnMap,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
        state.locationHint?.let { hint ->
            Spacer(Modifier.height(CpDimens.spacing2))
            Text(
                text = addShopLocationMessage(hint),
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
        title = selectedPrice?.let { stringResource(Res.string.legacy_shop_price_cappuccino, it) } ?: localizedShopFiltersPrice,
        showHint = false,
    )
}

// ── Шаг 2: Фото ───────────────────────────────────────────────────────────────

@Composable
private fun StepPhotos(
    state: AddShopUiState,
    vm: AddShopViewModel,
) {
    val localizedLegacyAddShopScreenContributionsShopPhotosTitle = stringResource(Res.string.legacy_add_shop_screen_contributions_shop_photos_title)
    val localizedLegacyAddShopScreenContributionsRequestMenuPhotos = stringResource(Res.string.legacy_add_shop_screen_contributions_request_menu_photos)

    StepLegend(optional = true)
    PhotoAttachmentsSection(
        photos = state.photos.map { PickedImage(it.bytes, it.fileName, it.contentType) },
        maxPhotos = MAX_SHOP_PHOTOS,
        onPhotosAdded = vm::addPhotos,
        onRemovePhoto = vm::removePhoto,
        title = localizedLegacyAddShopScreenContributionsShopPhotosTitle,
        hint = stringResource(Res.string.legacy_shop_photos_hint, MAX_SHOP_PHOTOS),
    )

    Spacer(Modifier.height(CpDimens.spacing6))
    PhotoAttachmentsSection(
        photos = state.menuPhotos.map { PickedImage(it.bytes, it.fileName, it.contentType) },
        maxPhotos = MAX_MENU_PHOTOS,
        onPhotosAdded = vm::addMenuPhotos,
        onRemovePhoto = vm::removeMenuPhoto,
        title = localizedLegacyAddShopScreenContributionsRequestMenuPhotos,
        hint = stringResource(Res.string.legacy_shop_menu_photos_hint, MAX_MENU_PHOTOS),
    )
}

// ── Шаг 3: Расписание ─────────────────────────────────────────────────────────

private val SCHEDULE_PRESETS = listOf("08:00" to "20:00", "09:00" to "21:00", "10:00" to "22:00")

@Composable
private fun StepSchedule(state: AddShopUiState, vm: AddShopViewModel) {
    val localizedLegacyAddShopScreenContributionsShopScheduleIntro = stringResource(Res.string.legacy_add_shop_screen_contributions_shop_schedule_intro)
    val localizedLegacyAddShopScreenContributionsShopMode = stringResource(Res.string.legacy_add_shop_screen_contributions_shop_mode)
    val localizedLegacyAddShopScreenContributionsShopModeFooter = stringResource(Res.string.legacy_add_shop_screen_contributions_shop_mode_footer)
    val localizedLegacyAddShopScreenContributionsShopPerDay = stringResource(Res.string.legacy_add_shop_screen_contributions_shop_per_day)
    val localizedLegacyAddShopScreenContributionsShopHours = stringResource(Res.string.legacy_add_shop_screen_contributions_shop_hours)
    val localizedLegacyAddShopScreenContributionsShopOpening = stringResource(Res.string.legacy_add_shop_screen_contributions_shop_opening)
    val localizedLegacyAddShopScreenContributionsShopClosing = stringResource(Res.string.legacy_add_shop_screen_contributions_shop_closing)
    val localizedLegacyAddShopScreenContributionsShopPresets = stringResource(Res.string.legacy_add_shop_screen_contributions_shop_presets)
    val localizedLegacyAddShopScreenContributionsShopWorkDays = stringResource(Res.string.legacy_add_shop_screen_contributions_shop_work_days)
    val localizedLegacyAddShopScreenContributionsShopWorkDaysFooter = stringResource(Res.string.legacy_add_shop_screen_contributions_shop_work_days_footer)
    val localizedDiscoveryMapCardOpen = stringResource(Res.string.discovery_map_card_open)

    StepLegend(optional = true)
    Text(
        text = localizedLegacyAddShopScreenContributionsShopScheduleIntro,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = CpDimens.spacing4),
    )

    Column(verticalArrangement = Arrangement.spacedBy(CpDimens.spacing6)) {
        GroupSection(
            title = localizedLegacyAddShopScreenContributionsShopMode,
            footer = localizedLegacyAddShopScreenContributionsShopModeFooter,
        ) {
            SwitchRow(
                label = localizedLegacyAddShopScreenContributionsShopPerDay,
                checked = state.usePerDaySchedule,
                onCheckedChange = vm::setUsePerDaySchedule,
            )
        }

        if (!state.usePerDaySchedule) {
            GroupSection(title = localizedLegacyAddShopScreenContributionsShopHours) {
                TimeStepperRow(localizedLegacyAddShopScreenContributionsShopOpening, state.unifiedOpenTime) { vm.adjustUnifiedOpen(it) }
                RowSeparator()
                TimeStepperRow(localizedLegacyAddShopScreenContributionsShopClosing, state.unifiedCloseTime) { vm.adjustUnifiedClose(it) }
            }

            GroupSection(title = localizedLegacyAddShopScreenContributionsShopPresets) {
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
                title = localizedLegacyAddShopScreenContributionsShopWorkDays,
                trailing = stringResource(Res.string.legacy_calendar_days_count, 7 - state.closedDays.size),
                footer = localizedLegacyAddShopScreenContributionsShopWorkDaysFooter,
            ) {
                state.schedules.forEachIndexed { index, day ->
                    if (index > 0) RowSeparator()
                    CheckmarkRow(
                        label = addShopWeekdayLabel(day.dayOfWeek),
                        checked = day.dayOfWeek !in state.closedDays,
                        onToggle = { vm.toggleClosedDay(day.dayOfWeek) },
                    )
                }
            }
        } else {
            state.schedules.forEach { day ->
                GroupSection(title = addShopWeekdayLabel(day.dayOfWeek)) {
                    SwitchRow(
                        label = localizedDiscoveryMapCardOpen,
                        checked = !day.isClosed,
                        onCheckedChange = { open ->
                            vm.updateSchedule(day.dayOfWeek) { it.copy(isClosed = !open) }
                        },
                    )
                    if (!day.isClosed) {
                        RowSeparator()
                        TimeStepperRow(localizedLegacyAddShopScreenContributionsShopOpening, day.openTime) { vm.adjustDayOpenTime(day.dayOfWeek, it) }
                        RowSeparator()
                        TimeStepperRow(localizedLegacyAddShopScreenContributionsShopClosing, day.closeTime) { vm.adjustDayCloseTime(day.dayOfWeek, it) }
                    }
                }
            }
        }
    }
}

// ponytail: ±30 min stepper; swap for an iOS-style wheel time picker if 30-min granularity is too coarse.
@Composable
private fun TimeStepperRow(label: String, value: String, onAdjust: (Int) -> Unit) {
    if (com.coffeepeek.admin.ui.component.NativeTimePickerRow(label, value, onAdjust)) return
    StepperRow(
        label = label,
        value = value,
        onDecrease = { onAdjust(-30) },
        onIncrease = { onAdjust(30) },
        decreaseDescription = stringResource(Res.string.legacy_time_thirty_minutes_earlier, label),
        increaseDescription = stringResource(Res.string.legacy_time_thirty_minutes_later, label),
    )
}

@Composable
private fun StepLegend(
    requiredHint: String? = null,
    optional: Boolean = false,
) {
    val localizedLegacyAddShopScreenContributionsShopOptionalLegend = stringResource(Res.string.legacy_add_shop_screen_contributions_shop_optional_legend)

    when {
        requiredHint != null -> Text(
            text = requiredHint,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
        )
        optional -> Text(
            text = localizedLegacyAddShopScreenContributionsShopOptionalLegend,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = CpDimens.spacing1),
        )
    }
}

// ── Шаг 4: Контакты ───────────────────────────────────────────────────────────

@Composable
private fun StepContacts(state: AddShopUiState, vm: AddShopViewModel) {
    val localizedLegacyAddShopScreenContributionsShopContactsIntro = stringResource(Res.string.legacy_add_shop_screen_contributions_shop_contacts_intro)
    val localizedLegacyAddShopScreenContributionsShopPhoneLabel = stringResource(Res.string.legacy_add_shop_screen_contributions_shop_phone_label)
    val localizedLegacyAddShopScreenContributionsShopInstagramLabel = stringResource(Res.string.legacy_add_shop_screen_contributions_shop_instagram_label)
    val localizedLegacyAddShopScreenContributionsShopWebsiteLabel = stringResource(Res.string.legacy_add_shop_screen_contributions_shop_website_label)

    StepLegend(optional = true)
    Text(
        text = localizedLegacyAddShopScreenContributionsShopContactsIntro,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = CpDimens.spacing4),
    )

    FormField(label = localizedLegacyAddShopScreenContributionsShopPhoneLabel, optional = true) {
        AppOutlinedField(
            value = state.phone,
            onValueChange = vm::onPhoneChange,
            placeholder = "+375",
            keyboardType = KeyboardType.Phone,
            errorText = if (state.phone.isNotEmpty()) inputValidationMessage(state.phoneError) else null,
            leadingIcon = CpIcons.Phone,
        )
    }
    Spacer(Modifier.height(CpDimens.spacing3))
    FormField(label = localizedLegacyAddShopScreenContributionsShopInstagramLabel, optional = true) {
        AppOutlinedField(
            value = state.instagram,
            keyboardType = KeyboardType.Uri,
            onValueChange = vm::onInstagramChange,
            placeholder = "@",
            errorText = if (state.instagram.isNotEmpty()) inputValidationMessage(state.instagramError) else null,
            leadingIcon = CpIcons.Instagram,
        )
    }
    Spacer(Modifier.height(CpDimens.spacing3))
    FormField(label = localizedLegacyAddShopScreenContributionsShopWebsiteLabel, optional = true) {
        AppOutlinedField(
            value = state.website,
            onValueChange = vm::onWebsiteChange,
            placeholder = "mycoffee.by",
            keyboardType = KeyboardType.Uri,
            errorText = if (state.website.isNotEmpty()) inputValidationMessage(state.websiteError) else null,
            leadingIcon = CpIcons.Globe,
        )
    }
    Spacer(Modifier.height(CpDimens.spacing3))
    FormField(label = stringResource(Res.string.step_email), optional = true) {
        AppOutlinedField(
            value = state.email,
            onValueChange = vm::onEmailChange,
            placeholder = "info@coffee.by",
            keyboardType = KeyboardType.Email,
            errorText = if (state.email.isNotEmpty()) inputValidationMessage(state.emailError) else null,
            leadingIcon = CpIcons.Email,
        )
    }
}

// ── Шаг 3: Особенности ────────────────────────────────────────────────────────

@Composable
private fun StepFeatures(state: AddShopUiState, vm: AddShopViewModel) {
    val localizedLegacyAddShopScreenContributionsShopFeaturesIntro = stringResource(Res.string.legacy_add_shop_screen_contributions_shop_features_intro)
    val localizedLegacyAddShopScreenContributionsShopBrewMethods = stringResource(Res.string.legacy_add_shop_screen_contributions_shop_brew_methods)
    val localizedLegacyAddShopScreenShelfGrindBeans = stringResource(Res.string.legacy_add_shop_screen_shelf_grind_beans)
    val localizedFavoritesRoasters = stringResource(Res.string.favorites_roasters)
    val localizedLegacyAddShopScreenContributionsShopRoastersFooter = stringResource(Res.string.legacy_add_shop_screen_contributions_shop_roasters_footer)
    val localizedCatalogRoasterSuggestionAction = stringResource(Res.string.catalog_roaster_suggestion_action)
    val localizedShopFiltersEquipment = stringResource(Res.string.shop_filters_equipment)

    StepLegend(optional = true)
    Text(
        text = localizedLegacyAddShopScreenContributionsShopFeaturesIntro,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = CpDimens.spacing4),
    )

    Column(verticalArrangement = Arrangement.spacedBy(CpDimens.spacing6)) {
        CheckmarkSection(
            title = localizedLegacyAddShopScreenContributionsShopBrewMethods,
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
            title = localizedLegacyAddShopScreenShelfGrindBeans,
            items = state.beans,
            selectedIds = state.selectedBeanIds,
            onToggle = vm::toggleBean,
        )
        CheckmarkSection(
            title = localizedFavoritesRoasters,
            items = state.roasters,
            selectedIds = state.selectedRoasterIds,
            onToggle = vm::toggleRoaster,
            footer = localizedLegacyAddShopScreenContributionsShopRoastersFooter,
            leading = { item -> RoasterAvatar(item) },
            extraRows = {
                ActionRow(
                    label = localizedCatalogRoasterSuggestionAction,
                    icon = CpIcons.Add,
                    onClick = { Navigator.navigate(Navigator.Screen.AddRoaster) },
                )
            },
        )
        CheckmarkSection(
            title = localizedShopFiltersEquipment,
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
    val localizedLegacyAddShopScreenContributionsShopFieldOptional = stringResource(Res.string.legacy_add_shop_screen_contributions_shop_field_optional)

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
                text = localizedLegacyAddShopScreenContributionsShopFieldOptional,
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
    CoffeePeekSelectField(
        value = selected?.name ?: stringResource(Res.string.select_city_placeholder),
        options = cities.map { city ->
            CoffeePeekSelectOption("city:${city.id}", city.name, city.id == selected?.id) { onSelect(city) }
        },
        contentDescription = stringResource(Res.string.select_city_label),
        modifier = Modifier.fillMaxWidth(),
        isPlaceholder = selected == null,
        errorDescription = error,
    )
    if (error != null) {
        Text(error, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(top = 4.dp, start = 4.dp))
    }
}

@Composable
private fun CityDropdownPreview(darkTheme: Boolean) = CoffeePeekTheme(darkTheme = darkTheme) {
    Surface { Column(Modifier.padding(16.dp)) {
        val cities = listOf(City("minsk", "Минск"), City("brest", "Брест"))
        CityDropdown(cities, cities.first(), {})
    } }
}
@Preview @Composable private fun CityDropdownLightPreview() = CityDropdownPreview(false)
@Preview @Composable private fun CityDropdownDarkPreview() = CityDropdownPreview(true)
