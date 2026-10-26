package com.coffeepeek.feature.contributions.impl.ui

import com.coffeepeek.feature.contributions.impl.ui.component.ContributionRoasterLogo
import com.coffeepeek.feature.contributions.impl.ui.component.ContributionBrewMethodIcon
import com.coffeepeek.feature.contributions.impl.ui.component.ContributionPhotos
import androidx.compose.foundation.background
import com.coffeepeek.feature.contributions.impl.resources.Res
import com.coffeepeek.feature.contributions.impl.resources.contributions_editor_already_uploaded
import com.coffeepeek.feature.contributions.impl.resources.contributions_editor_collapse
import com.coffeepeek.feature.contributions.impl.resources.contributions_editor_current_photos
import com.coffeepeek.feature.contributions.impl.resources.contributions_editor_description_placeholder
import com.coffeepeek.feature.contributions.impl.resources.contributions_editor_drinks_empty
import com.coffeepeek.feature.contributions.impl.resources.contributions_editor_expand_menu
import com.coffeepeek.feature.contributions.impl.resources.contributions_editor_menu_details
import com.coffeepeek.feature.contributions.impl.resources.contributions_editor_new_menu_photos
import com.coffeepeek.feature.contributions.impl.resources.contributions_editor_new_menu_photos_hint
import com.coffeepeek.feature.contributions.impl.resources.contributions_editor_new_photos
import com.coffeepeek.feature.contributions.impl.resources.contributions_editor_new_photos_hint
import com.coffeepeek.feature.contributions.impl.resources.contributions_editor_no_saved_photos
import com.coffeepeek.feature.contributions.impl.resources.contributions_editor_price_placeholder
import com.coffeepeek.feature.contributions.impl.resources.contributions_editor_remove
import com.coffeepeek.feature.contributions.impl.resources.contributions_editor_remove_uploaded
import com.coffeepeek.feature.contributions.impl.resources.contributions_section_brew_methods
import com.coffeepeek.feature.contributions.impl.resources.contributions_section_equipment
import com.coffeepeek.feature.contributions.impl.resources.contributions_section_roasters
import com.coffeepeek.feature.contributions.impl.resources.contributions_section_tags
import com.coffeepeek.feature.contributions.impl.resources.contributions_shop_email_label
import com.coffeepeek.feature.contributions.impl.resources.contributions_shop_error_name_too_long
import com.coffeepeek.feature.contributions.impl.resources.contributions_shop_field_optional
import com.coffeepeek.feature.contributions.impl.resources.contributions_shop_instagram_label
import com.coffeepeek.feature.contributions.impl.resources.contributions_shop_menu_photos_title
import com.coffeepeek.feature.contributions.impl.resources.contributions_shop_phone_label
import com.coffeepeek.feature.contributions.impl.resources.contributions_shop_website_label
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
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
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
            stringResource(Res.string.contributions_section_tags), state.catalogs.tags, state.tagIds, { onAction(ChangeEditorAction.Toggle(it)) },
        )
        ChangeSection.Roasters -> CheckmarkSection(
            stringResource(Res.string.contributions_section_roasters), state.catalogs.roasters, state.roasterIds, { onAction(ChangeEditorAction.Toggle(it)) },
            leading = { item -> ContributionRoasterLogo(item) },
        )
        ChangeSection.Equipment -> CheckmarkSection(
            stringResource(Res.string.contributions_section_equipment), state.catalogs.equipment, state.equipmentIds, { onAction(ChangeEditorAction.Toggle(it)) },
        )
        ChangeSection.BrewMethods -> CheckmarkSection(
            stringResource(Res.string.contributions_section_brew_methods), state.catalogs.brewMethods, state.brewMethodIds, { onAction(ChangeEditorAction.Toggle(it)) },
            leading = { item -> Box(Modifier.size(22.dp)) { ContributionBrewMethodIcon(item.name) } },
        )
        ChangeSection.Menu -> MenuEditor(state, host, onAction)
    }
}

@Composable
private fun DescriptionEditor(state: ChangeEditorState, onAction: (ChangeEditorAction) -> Unit) {
    OutlinedField(
        state.description, { onAction(ChangeEditorAction.Description(it)) }, stringResource(Res.string.contributions_editor_description_placeholder),
        errorText = stringResource(Res.string.contributions_shop_error_name_too_long, ContributionLimits.DESCRIPTION).takeIf { state.descriptionTooLong },
        counter = "${state.description.length}/${ContributionLimits.DESCRIPTION}", minLines = 6, maxLines = 12,
    )
}

@Composable
private fun ContactsEditor(state: ChangeEditorState, onAction: (ChangeEditorAction) -> Unit) {
    val issues = state.contactIssues
    Column(verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
        EditorContact(Res.string.contributions_shop_phone_label, state.phone, KeyboardType.Phone, CoffeePeekIcons.Phone, ContactIssue.Phone in issues) { onAction(ChangeEditorAction.Phone(it)) }
        EditorContact(Res.string.contributions_shop_instagram_label, state.instagram, KeyboardType.Uri, CoffeePeekIcons.Instagram, ContactIssue.Instagram in issues) { onAction(ChangeEditorAction.Instagram(it)) }
        EditorContact(Res.string.contributions_shop_website_label, state.website, KeyboardType.Uri, CoffeePeekIcons.Globe, ContactIssue.Website in issues) { onAction(ChangeEditorAction.Website(it)) }
        EditorContact(Res.string.contributions_shop_email_label, state.email, KeyboardType.Email, CoffeePeekIcons.Email, ContactIssue.Email in issues) { onAction(ChangeEditorAction.Email(it)) }
    }
}

@Composable
private fun EditorContact(
    label: StringResource, value: String, keyboard: KeyboardType, icon: androidx.compose.ui.graphics.vector.ImageVector, invalid: Boolean, onChange: (String) -> Unit,
) {
    val issue = when (label) {
        Res.string.contributions_shop_phone_label -> ContactIssue.Phone
        Res.string.contributions_shop_instagram_label -> ContactIssue.Instagram
        Res.string.contributions_shop_website_label -> ContactIssue.Website
        else -> ContactIssue.Email
    }
    FormField(stringResource(label)) {
        OutlinedField(value, onChange, stringResource(label), errorText = issue.message().takeIf { invalid && value.isNotBlank() }, keyboardType = keyboard, leadingIcon = icon)
    }
}

@Composable
private fun PhotosEditor(state: ChangeEditorState, host: ChangeEditorHost, onAction: (ChangeEditorAction) -> Unit) {
    SectionTitle(stringResource(Res.string.contributions_editor_current_photos))
    SavedPhotos(state.retainedPhotos) { onAction(ChangeEditorAction.RemoveRetainedPhoto(it)) }
    UploadedNote(state.uploadedPhotos.size) { onAction(ChangeEditorAction.RemoveUploadedPhotos) }
    val room = ContributionLimits.SHOP_PHOTOS - state.retainedPhotos.size - state.uploadedPhotos.size
    if (room > 0) {
        Spacer(Modifier.height(CoffeePeekDimens.spacing4))
        ContributionPhotos(
            state.newPhotos, room, stringResource(Res.string.contributions_editor_new_photos), stringResource(Res.string.contributions_editor_new_photos_hint),
            { onAction(ChangeEditorAction.AddPhotos(it)) }, { onAction(ChangeEditorAction.RemoveNewPhoto(it)) }, picker = host.photoPicker
        )
    }
}

@Composable
private fun MenuEditor(state: ChangeEditorState, host: ChangeEditorHost, onAction: (ChangeEditorAction) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    SectionTitle(stringResource(Res.string.contributions_shop_menu_photos_title))
    SavedPhotos(state.retainedMenuPhotos) { onAction(ChangeEditorAction.RemoveRetainedMenuPhoto(it)) }
    UploadedNote(state.uploadedMenuPhotos.size) { onAction(ChangeEditorAction.RemoveUploadedMenuPhotos) }
    val room = ContributionLimits.MENU_PHOTOS - state.retainedMenuPhotos.size - state.uploadedMenuPhotos.size
    if (room > 0) {
        Spacer(Modifier.height(CoffeePeekDimens.spacing3))
        ContributionPhotos(
            state.newMenuPhotos, room, stringResource(Res.string.contributions_editor_new_menu_photos),
            stringResource(Res.string.contributions_editor_new_menu_photos_hint, ContributionLimits.MENU_PHOTOS),
            { onAction(ChangeEditorAction.AddMenuPhotos(it)) }, { onAction(ChangeEditorAction.RemoveNewMenuPhoto(it)) }, picker = host.photoPicker
        )
    }
    Spacer(Modifier.height(CoffeePeekDimens.spacing5))
    CoffeePeekGroupSection(stringResource(Res.string.contributions_editor_menu_details), trailing = stringResource(Res.string.contributions_shop_field_optional)) {
        CoffeePeekActionRow(
            stringResource(if (expanded) Res.string.contributions_editor_collapse else Res.string.contributions_editor_expand_menu), { expanded = !expanded },
        )
        if (expanded) {
            if (state.menuRows.isEmpty()) Text(
                stringResource(Res.string.contributions_editor_drinks_empty), color = MaterialTheme.colorScheme.onSurfaceVariant,
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
        OutlinedField(row.priceText, { onAction(ChangeEditorAction.SetPrice(row.slug, it)) }, stringResource(Res.string.contributions_editor_price_placeholder), keyboardType = KeyboardType.Decimal)
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
            stringResource(Res.string.contributions_editor_already_uploaded, count), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f),
        )
        Text(
            stringResource(Res.string.contributions_editor_remove_uploaded), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable(onClick = onRemove).padding(CoffeePeekDimens.spacing1),
        )
    }
}

@Composable
private fun Row2(content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) =
    androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, content = content)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SavedPhotos(photos: List<ShopPhotoRef>, onRemove: (String) -> Unit) {
    if (photos.isEmpty()) {
        Text(stringResource(Res.string.contributions_editor_no_saved_photos), color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    val shape = RoundedCornerShape(CoffeePeekDimens.cardRadius)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2), verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
        photos.forEach { photo ->
            Box(Modifier.size(96.dp)) {
                com.coffeepeek.core.designsystem.component.CoffeePeekImage(data = photo.url, modifier = Modifier.fillMaxSize().clip(shape).border(1.dp, MaterialTheme.colorScheme.outline, shape), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                Box(
                    Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = (-6).dp).size(24.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.75f))
                        .clickable { onRemove(photo.id) },
                    contentAlignment = Alignment.Center,
                ) { Icon(CoffeePeekIcons.Close, stringResource(Res.string.contributions_editor_remove), tint = Color.White, modifier = Modifier.size(14.dp)) }
            }
        }
    }
}
