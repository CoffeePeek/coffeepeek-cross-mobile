package com.coffeepeek.feature.contributions.impl.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekActionRow
import com.coffeepeek.core.designsystem.component.CoffeePeekCapsuleSegmentedControl
import com.coffeepeek.core.designsystem.component.CoffeePeekGroupSection
import com.coffeepeek.core.designsystem.component.CoffeePeekRowSeparator
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.feature.contributions.api.ChangeEditorHost
import com.coffeepeek.feature.contributions.domain.model.Availability
import com.coffeepeek.feature.contributions.domain.model.ChangeSection
import com.coffeepeek.feature.contributions.domain.model.ShopPhotoRef
import com.coffeepeek.feature.contributions.domain.rules.ContactIssue
import com.coffeepeek.feature.contributions.domain.rules.ContributionLimits
import com.coffeepeek.feature.contributions.impl.R
import com.coffeepeek.feature.contributions.impl.change.ChangeEditorAction
import com.coffeepeek.feature.contributions.impl.change.ChangeEditorState
import com.coffeepeek.feature.contributions.impl.change.MenuRowState

@Composable
internal fun ChangeEditorBody(state: ChangeEditorState, host: ChangeEditorHost, onAction: (ChangeEditorAction) -> Unit) {
    when (state.section) {
        ChangeSection.Description -> DescriptionEditor(state, onAction)
        ChangeSection.Contacts -> ContactsEditor(state, onAction)
        ChangeSection.Photos -> PhotosEditor(state, host, onAction)
        ChangeSection.Tags -> CheckmarkSection(
            stringResource(R.string.contributions_section_tags), state.catalogs.tags, state.tagIds, { onAction(ChangeEditorAction.Toggle(it)) },
        )
        ChangeSection.Roasters -> CheckmarkSection(
            stringResource(R.string.contributions_section_roasters), state.catalogs.roasters, state.roasterIds, { onAction(ChangeEditorAction.Toggle(it)) },
            leading = { item -> host.roasterLogo(item) },
        )
        ChangeSection.Equipment -> CheckmarkSection(
            stringResource(R.string.contributions_section_equipment), state.catalogs.equipment, state.equipmentIds, { onAction(ChangeEditorAction.Toggle(it)) },
        )
        ChangeSection.BrewMethods -> CheckmarkSection(
            stringResource(R.string.contributions_section_brew_methods), state.catalogs.brewMethods, state.brewMethodIds, { onAction(ChangeEditorAction.Toggle(it)) },
            leading = { item -> Box(Modifier.size(22.dp)) { host.brewMethodIcon(item.name) } },
        )
        ChangeSection.Menu -> MenuEditor(state, host, onAction)
    }
}

@Composable
private fun DescriptionEditor(state: ChangeEditorState, onAction: (ChangeEditorAction) -> Unit) {
    OutlinedField(
        state.description, { onAction(ChangeEditorAction.Description(it)) }, stringResource(R.string.contributions_editor_description_placeholder),
        errorText = stringResource(R.string.contributions_shop_error_name_too_long, ContributionLimits.DESCRIPTION).takeIf { state.descriptionTooLong },
        counter = "${state.description.length}/${ContributionLimits.DESCRIPTION}", minLines = 6, maxLines = 12,
    )
}

@Composable
private fun ContactsEditor(state: ChangeEditorState, onAction: (ChangeEditorAction) -> Unit) {
    val issues = state.contactIssues
    Column(verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
        EditorContact(R.string.contributions_shop_phone_label, state.phone, KeyboardType.Phone, CoffeePeekIcons.Phone, ContactIssue.Phone in issues) { onAction(ChangeEditorAction.Phone(it)) }
        EditorContact(R.string.contributions_shop_instagram_label, state.instagram, KeyboardType.Uri, CoffeePeekIcons.Instagram, ContactIssue.Instagram in issues) { onAction(ChangeEditorAction.Instagram(it)) }
        EditorContact(R.string.contributions_shop_website_label, state.website, KeyboardType.Uri, CoffeePeekIcons.Globe, ContactIssue.Website in issues) { onAction(ChangeEditorAction.Website(it)) }
        EditorContact(R.string.contributions_shop_email_label, state.email, KeyboardType.Email, CoffeePeekIcons.Email, ContactIssue.Email in issues) { onAction(ChangeEditorAction.Email(it)) }
    }
}

@Composable
private fun EditorContact(
    label: Int, value: String, keyboard: KeyboardType, icon: androidx.compose.ui.graphics.vector.ImageVector, invalid: Boolean, onChange: (String) -> Unit,
) {
    val issue = when (label) {
        R.string.contributions_shop_phone_label -> ContactIssue.Phone
        R.string.contributions_shop_instagram_label -> ContactIssue.Instagram
        R.string.contributions_shop_website_label -> ContactIssue.Website
        else -> ContactIssue.Email
    }
    FormField(stringResource(label)) {
        OutlinedField(value, onChange, stringResource(label), errorText = issue.message().takeIf { invalid && value.isNotBlank() }, keyboardType = keyboard, leadingIcon = icon)
    }
}

@Composable
private fun PhotosEditor(state: ChangeEditorState, host: ChangeEditorHost, onAction: (ChangeEditorAction) -> Unit) {
    SectionTitle(stringResource(R.string.contributions_editor_current_photos))
    SavedPhotos(state.retainedPhotos, host) { onAction(ChangeEditorAction.RemoveRetainedPhoto(it)) }
    UploadedNote(state.uploadedPhotos.size) { onAction(ChangeEditorAction.RemoveUploadedPhotos) }
    val room = ContributionLimits.SHOP_PHOTOS - state.retainedPhotos.size - state.uploadedPhotos.size
    if (room > 0) {
        Spacer(Modifier.height(CoffeePeekDimens.spacing4))
        host.photos(
            state.newPhotos, room, stringResource(R.string.contributions_editor_new_photos), stringResource(R.string.contributions_editor_new_photos_hint),
            { onAction(ChangeEditorAction.AddPhotos(it)) }, { onAction(ChangeEditorAction.RemoveNewPhoto(it)) },
        )
    }
}

@Composable
private fun MenuEditor(state: ChangeEditorState, host: ChangeEditorHost, onAction: (ChangeEditorAction) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    SectionTitle(stringResource(R.string.contributions_shop_menu_photos_title))
    SavedPhotos(state.retainedMenuPhotos, host) { onAction(ChangeEditorAction.RemoveRetainedMenuPhoto(it)) }
    UploadedNote(state.uploadedMenuPhotos.size) { onAction(ChangeEditorAction.RemoveUploadedMenuPhotos) }
    val room = ContributionLimits.MENU_PHOTOS - state.retainedMenuPhotos.size - state.uploadedMenuPhotos.size
    if (room > 0) {
        Spacer(Modifier.height(CoffeePeekDimens.spacing3))
        host.photos(
            state.newMenuPhotos, room, stringResource(R.string.contributions_editor_new_menu_photos),
            stringResource(R.string.contributions_editor_new_menu_photos_hint, ContributionLimits.MENU_PHOTOS),
            { onAction(ChangeEditorAction.AddMenuPhotos(it)) }, { onAction(ChangeEditorAction.RemoveNewMenuPhoto(it)) },
        )
    }
    Spacer(Modifier.height(CoffeePeekDimens.spacing5))
    CoffeePeekGroupSection(stringResource(R.string.contributions_editor_menu_details), trailing = stringResource(R.string.contributions_shop_field_optional)) {
        CoffeePeekActionRow(
            stringResource(if (expanded) R.string.contributions_editor_collapse else R.string.contributions_editor_expand_menu), { expanded = !expanded },
        )
        if (expanded) {
            if (state.menuRows.isEmpty()) Text(
                stringResource(R.string.contributions_editor_drinks_empty), color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(CoffeePeekDimens.spacing4),
            )
            state.menuRows.forEach { row ->
                CoffeePeekRowSeparator()
                MenuRow(row, onAction)
            }
        }
    }
}

@Composable
private fun MenuRow(row: MenuRowState, onAction: (ChangeEditorAction) -> Unit) {
    val labels = Availability.entries.associateWith { it.title() }
    Column(Modifier.fillMaxWidth().padding(horizontal = CoffeePeekDimens.spacing4, vertical = CoffeePeekDimens.spacing3)) {
        Text(row.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(CoffeePeekDimens.spacing2))
        CoffeePeekCapsuleSegmentedControl(
            options = Availability.entries, selected = row.availability, label = { labels.getValue(it) },
            onSelected = { onAction(ChangeEditorAction.SetAvailability(row.slug, it)) },
        )
        Spacer(Modifier.height(CoffeePeekDimens.spacing2))
        OutlinedField(row.priceText, { onAction(ChangeEditorAction.SetPrice(row.slug, it)) }, stringResource(R.string.contributions_editor_price_placeholder), keyboardType = KeyboardType.Decimal)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(CoffeePeekDimens.spacing2))
}

@Composable
private fun UploadedNote(count: Int, onRemove: () -> Unit) {
    if (count == 0) return
    Spacer(Modifier.height(CoffeePeekDimens.spacing2))
    Row2 {
        Text(
            stringResource(R.string.contributions_editor_already_uploaded, count), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f),
        )
        Text(
            stringResource(R.string.contributions_editor_remove_uploaded), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable(onClick = onRemove).padding(CoffeePeekDimens.spacing1),
        )
    }
}

@Composable
private fun Row2(content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) =
    androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, content = content)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SavedPhotos(photos: List<ShopPhotoRef>, host: ChangeEditorHost, onRemove: (String) -> Unit) {
    if (photos.isEmpty()) {
        Text(stringResource(R.string.contributions_editor_no_saved_photos), color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    val shape = RoundedCornerShape(CoffeePeekDimens.cardRadius)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2), verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
        photos.forEach { photo ->
            Box(Modifier.size(96.dp)) {
                host.remoteImage(photo.url, Modifier.fillMaxSize().clip(shape).border(1.dp, MaterialTheme.colorScheme.outline, shape))
                Box(
                    Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = (-6).dp).size(24.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.75f))
                        .clickable { onRemove(photo.id) },
                    contentAlignment = Alignment.Center,
                ) { Icon(CoffeePeekIcons.Close, stringResource(R.string.contributions_editor_remove), tint = Color.White, modifier = Modifier.size(14.dp)) }
            }
        }
    }
}
