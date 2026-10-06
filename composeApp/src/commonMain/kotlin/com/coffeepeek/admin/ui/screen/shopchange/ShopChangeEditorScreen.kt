package com.coffeepeek.admin.ui.screen.shopchange

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.di.platformViewModel
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.component.CompactOutlinedTextField
import com.coffeepeek.admin.ui.component.AppButton
import com.coffeepeek.admin.ui.component.CoffeePeekLoader
import com.coffeepeek.admin.ui.component.CpTopBar
import com.coffeepeek.admin.ui.component.CheckmarkSection
import com.coffeepeek.admin.ui.component.PhotoAttachmentsSection
import com.coffeepeek.admin.ui.component.RoasterAvatar
import com.coffeepeek.admin.ui.component.RowSeparator
import com.coffeepeek.admin.ui.component.GroupSection
import com.coffeepeek.admin.ui.component.brewMethodIcon
import com.coffeepeek.admin.ui.component.shopTagIcon
import com.coffeepeek.admin.ui.component.platformTextInputOptions
import com.coffeepeek.admin.ui.component.rememberSyncedTextFieldValue
import com.coffeepeek.admin.ui.component.limitTextLength
import com.coffeepeek.admin.ui.icons.CpIcons
import com.coffeepeek.admin.utils.CpImage
import com.coffeepeek.admin.utils.MAX_MENU_PHOTOS
import com.coffeepeek.admin.utils.MAX_SHOP_PHOTOS
import com.coffeepeek.domain.model.CatalogItem
import com.coffeepeek.domain.model.MenuItemAvailability
import com.coffeepeek.domain.model.ShopChangeSection
import com.coffeepeek.domain.model.ShopPhoto
import org.koin.core.parameter.parametersOf
import org.jetbrains.compose.resources.painterResource

@Composable
fun ShopChangeEditorScreen(shopId: String, sectionName: String, requestId: String) {
    val section = remember(sectionName) {
        ShopChangeSection.entries.firstOrNull { it.name == sectionName } ?: ShopChangeSection.Description
    }
    val vm: ShopChangeEditorViewModel = platformViewModel(
        parameters = { parametersOf(shopId, section, requestId) },
    )
    val state by vm.state.collectAsState()

    state.error?.let { err ->
        AlertDialog(
            onDismissRequest = vm::clearError,
            title = { Text("Ошибка") },
            text = { Text(err) },
            confirmButton = {
                TextButton(onClick = vm::clearError) { Text("Понятно") }
            },
        )
    }

    Scaffold(
        topBar = { CpTopBar(section.title()) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CoffeePeekLoader()
            }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = CpDimens.spacing4),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                if (state.shopTitle.isNotBlank()) {
                    Text(
                        text = state.shopTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(CpDimens.spacing3))
                }
                when (state.section) {
                    ShopChangeSection.Description -> DescriptionEditor(state, vm)
                    ShopChangeSection.Contacts -> ContactsEditor(state, vm)
                    ShopChangeSection.Photos -> PhotosEditor(state, vm)
                    ShopChangeSection.Tags -> CheckmarkSection(
                        title = "Особенности",
                        items = state.catalogTags,
                        selectedIds = state.selectedTagIds,
                        onToggle = vm::toggleTag,
                        collapsedCount = Int.MAX_VALUE,
                        leading = { item ->
                            Icon(
                                imageVector = shopTagIcon(item.slug),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp),
                            )
                        },
                    )
                    ShopChangeSection.Roasters -> CheckmarkSection(
                        title = "Обжарщики",
                        items = state.catalogRoasters,
                        selectedIds = state.selectedRoasterIds,
                        onToggle = vm::toggleRoaster,
                        collapsedCount = Int.MAX_VALUE,
                        leading = { item -> RoasterAvatar(item) },
                    )
                    ShopChangeSection.Equipment -> CatalogEditor(state.catalogEquipment, state.selectedEquipmentIds, vm::toggleEquipment)
                    ShopChangeSection.BrewMethods -> CheckmarkSection(
                        title = "Методы заваривания",
                        items = state.catalogBrewMethods,
                        selectedIds = state.selectedBrewMethodIds,
                        onToggle = vm::toggleBrewMethod,
                        collapsedCount = Int.MAX_VALUE,
                        leading = { item ->
                            Icon(
                                painter = painterResource(brewMethodIcon(item.name)),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp),
                            )
                        },
                    )
                    ShopChangeSection.Menu -> MenuEditor(state, vm)
                }
                Spacer(Modifier.height(CpDimens.spacing4))
            }
            AppButton(
                text = if (requestId.isBlank()) "Отправить на модерацию" else "Сохранить заявку",
                onClick = vm::submit,
                enabled = state.canSubmit && !state.isSubmitting,
            )
            Spacer(Modifier.height(CpDimens.spacing3))
        }
    }
}

@Composable
private fun DescriptionEditor(state: ShopChangeEditorUiState, vm: ShopChangeEditorViewModel) {
    val descriptionField = rememberSyncedTextFieldValue(state.description)
    OutlinedTextField(
        value = descriptionField.value,
        onValueChange = { updated ->
            val limited = updated.limitTextLength(1000)
            descriptionField.value = limited
            vm.onDescriptionChange(limited.text)
        },
        keyboardOptions = platformTextInputOptions(),
        modifier = Modifier.fillMaxWidth().height(180.dp),
        placeholder = { Text("Расскажите о кофейне") },
        isError = state.descriptionError != null,
        supportingText = { Text("${state.description.length}/1000") },
        shape = RoundedCornerShape(CpDimens.buttonRadius),
        colors = fieldColors(),
    )
}

@Composable
private fun ContactsEditor(state: ShopChangeEditorUiState, vm: ShopChangeEditorViewModel) {
    ContactField("Телефон", state.phone, vm::onPhoneChange, state.phoneError, KeyboardType.Phone)
    Spacer(Modifier.height(CpDimens.spacing3))
    ContactField("Instagram", state.instagram, vm::onInstagramChange, state.instagramError)
    Spacer(Modifier.height(CpDimens.spacing3))
    ContactField("Сайт", state.website, vm::onWebsiteChange, state.websiteError, KeyboardType.Uri)
    Spacer(Modifier.height(CpDimens.spacing3))
    ContactField("Email", state.email, vm::onEmailChange, state.emailError, KeyboardType.Email)
}

@Composable
private fun ContactField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    error: String?,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    Column {
        CompactOutlinedTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth().height(CpDimens.buttonHeight),
            placeholder = { Text(label) },
            isError = error != null,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            singleLine = true,
            contentPadding = CpDimens.singleLineFieldContentPadding,
            shape = RoundedCornerShape(percent = 50),
            colors = fieldColors(),
        )
        if (error != null) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = CpDimens.spacing2, top = CpDimens.spacing1),
            )
        }
    }
}

@Composable
private fun PhotosEditor(state: ShopChangeEditorUiState, vm: ShopChangeEditorViewModel) {
    Text("Текущие фото", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(CpDimens.spacing2))
    RetainedPhotoRow(state.retainedPhotos, onRemove = vm::removeRetainedPhoto)
    if (state.alreadyUploadedPhotos.isNotEmpty()) {
        Spacer(Modifier.height(CpDimens.spacing2))
        Text(
            "Уже загружено в заявке: ${state.alreadyUploadedPhotos.size}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    val remaining = MAX_SHOP_PHOTOS - state.retainedPhotos.size - state.alreadyUploadedPhotos.size
    if (remaining > 0) {
        Spacer(Modifier.height(CpDimens.spacing4))
        PhotoAttachmentsSection(
            photos = state.newPhotos,
            maxPhotos = remaining,
            onPhotosAdded = vm::addPhotos,
            onRemovePhoto = vm::removeNewPhoto,
            title = "Новые фото",
            hint = "Добавьте фото — они встанут после сохранённых.",
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RetainedPhotoRow(photos: List<ShopPhoto>, onRemove: (String) -> Unit) {
    if (photos.isEmpty()) {
        Text("Нет сохранённых фото", color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing2),
        verticalArrangement = Arrangement.spacedBy(CpDimens.spacing2),
    ) {
        photos.forEach { photo ->
            val shape = RoundedCornerShape(CpDimens.radiusMd)
            Box(modifier = Modifier.size(96.dp)) {
                CpImage(
                    data = photo.previewUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(shape)
                        .border(1.dp, MaterialTheme.colorScheme.outline, shape),
                    contentScale = ContentScale.Crop,
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 6.dp, y = (-6).dp)
                        .size(24.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .clickable { onRemove(photo.id) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        CpIcons.Close,
                        contentDescription = "Удалить",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CatalogEditor(
    items: List<CatalogItem>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
) {
    if (items.isEmpty()) {
        Text("Каталог пуст", color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing2)) {
        items.forEach { item ->
            val isSelected = item.id in selected
            FilterChip(
                selected = isSelected,
                onClick = { onToggle(item.id) },
                label = { Text(item.name) },
                leadingIcon = if (isSelected) {
                    { Icon(CpIcons.Check, null, modifier = Modifier.size(14.dp)) }
                } else {
                    null
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        }
    }
}

@Composable
private fun MenuEditor(state: ShopChangeEditorUiState, vm: ShopChangeEditorViewModel) {
    GroupSection(title = "Позиции меню") {
        if (state.menuRows.isEmpty()) {
            Text(
                text = "Каталог напитков пуст",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(CpDimens.spacing4),
            )
        }
        state.menuRows.forEachIndexed { index, row ->
            if (index > 0) RowSeparator()
            MenuItemEditor(row = row, vm = vm)
        }
    }
    Spacer(Modifier.height(CpDimens.spacing5))
    Text("Фото меню", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(CpDimens.spacing2))
    RetainedPhotoRow(state.retainedMenuPhotos, onRemove = vm::removeRetainedMenuPhoto)
    val remaining = MAX_MENU_PHOTOS - state.retainedMenuPhotos.size - state.alreadyUploadedMenuPhotos.size
    if (remaining > 0) {
        Spacer(Modifier.height(CpDimens.spacing3))
        PhotoAttachmentsSection(
            photos = state.newMenuPhotos,
            maxPhotos = remaining,
            onPhotosAdded = vm::addMenuPhotos,
            onRemovePhoto = vm::removeNewMenuPhoto,
            title = "Новые фото меню",
            hint = "До $MAX_MENU_PHOTOS фото меню, включая сохранённые.",
        )
    }
}

@Composable
private fun MenuItemEditor(row: ShopChangeMenuRow, vm: ShopChangeEditorViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = CpDimens.spacing4, vertical = CpDimens.spacing3),
    ) {
        Text(
            text = row.name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(CpDimens.spacing2))
        AvailabilityPicker(
            selected = row.availability,
            onSelect = { availability ->
                vm.updateMenuRow(row.slug) { it.copy(availability = availability) }
            },
        )
        Spacer(Modifier.height(CpDimens.spacing2))
        Row(horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing2)) {
            CompactOutlinedTextField(
                value = row.priceText,
                onValueChange = { value -> vm.updateMenuRow(row.slug) { it.copy(priceText = value) } },
                modifier = Modifier.weight(1f).height(CpDimens.buttonHeight),
                placeholder = { Text("Цена, BYN") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(CpDimens.radiusMd),
                colors = fieldColors(),
            )
            CompactOutlinedTextField(
                value = row.volumeText,
                inputFilter = Char::isDigit,
                onValueChange = { value ->
                    vm.updateMenuRow(row.slug) { it.copy(volumeText = value.filter(Char::isDigit)) }
                },
                modifier = Modifier.weight(1f).height(CpDimens.buttonHeight),
                placeholder = { Text("Объём, мл") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(CpDimens.radiusMd),
                colors = fieldColors(),
            )
        }
    }
}

@Composable
private fun AvailabilityPicker(
    selected: MenuItemAvailability,
    onSelect: (MenuItemAvailability) -> Unit,
) {
    val shape = RoundedCornerShape(CpDimens.radiusMd)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(2.dp),
    ) {
        MenuItemAvailability.entries.forEach { availability ->
            val isSelected = availability == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(CpDimens.radiusSm))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.surface
                        else Color.Transparent,
                    )
                    .selectable(
                        selected = isSelected,
                        role = Role.RadioButton,
                        onClick = { onSelect(availability) },
                    )
                    .padding(vertical = CpDimens.spacing2),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = when (availability) {
                        MenuItemAvailability.Unknown -> "Не указано"
                        MenuItemAvailability.Present -> "Есть"
                        MenuItemAvailability.Absent -> "Нет"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.outline,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
)
