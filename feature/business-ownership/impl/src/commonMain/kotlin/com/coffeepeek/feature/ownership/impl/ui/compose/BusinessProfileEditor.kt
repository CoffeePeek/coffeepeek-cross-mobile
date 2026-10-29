package com.coffeepeek.feature.ownership.impl.ui.compose

import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.coffeepeek.core.designsystem.component.*
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.ownership.api.*
import com.coffeepeek.feature.ownership.domain.model.*
import com.coffeepeek.feature.ownership.impl.resources.*
import com.coffeepeek.feature.ownership.impl.ui.compose.model.*
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun BusinessProfileEditor(state: OwnershipState, action: (OwnershipAction) -> Unit, image: OwnershipImage, picker: OwnershipPhotoPicker) {
    val draft = state.draft ?: return
    val kind = state.profile?.kind ?: return
    Text(stringResource(Res.string.ownership_editor_hint), style = MaterialTheme.typography.bodySmall)
    BusinessProfileFields(draft, kind, state.busy, action)
    BusinessGallery(state, action, image, picker)
    CoffeePeekButton(stringResource(Res.string.ownership_save), { action(OwnershipAction.AskSave) },
        enabled = state.dirty && !state.busy && !state.reloadRequired)
    CoffeePeekButton(stringResource(Res.string.ownership_reload), { action(OwnershipAction.AskReload) }, enabled = !state.busy)
}
@Composable
private fun BusinessProfileFields(draft: ProfileDraft, kind: BusinessKind, busy: Boolean, action: (OwnershipAction) -> Unit) {
    ProfileInput(ProfileField.Name, Res.string.ownership_name, draft.name, busy, action)
    ProfileInput(ProfileField.About, Res.string.ownership_about, draft.about, busy, action)
    if (kind == BusinessKind.CoffeeShop) {
        ProfileInput(ProfileField.Phone, Res.string.ownership_phone, draft.phone, busy, action)
        ProfileInput(ProfileField.Email, Res.string.ownership_email, draft.email, busy, action)
    }
    ProfileInput(ProfileField.Site, Res.string.ownership_site, draft.site, busy, action)
    ProfileInput(ProfileField.Instagram, Res.string.ownership_instagram, draft.instagram, busy, action)
    ProfileInput(ProfileField.Address, Res.string.ownership_address, draft.address, busy, action)
    ProfileInput(ProfileField.Latitude, Res.string.ownership_latitude, draft.latitude, busy, action)
    ProfileInput(ProfileField.Longitude, Res.string.ownership_longitude, draft.longitude, busy, action)
}
@Composable
private fun ProfileInput(field: ProfileField, label: org.jetbrains.compose.resources.StringResource, value: String, busy: Boolean, action: (OwnershipAction) -> Unit) {
    val title = stringResource(label)
    CoffeePeekTextField(title, value, { action(OwnershipAction.ProfileText(field, it)) }, title, enabled = !busy)
}
@Preview @Composable private fun BusinessProfileEditorLightPreview() = CoffeePeekTheme(darkTheme = false) {
    BusinessProfileFields(ProfileDraft(name = "Пример кофейни"), BusinessKind.CoffeeShop, false, {})
}
@Preview @Composable private fun BusinessProfileEditorDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    BusinessProfileFields(ProfileDraft(name = "Пример кофейни"), BusinessKind.CoffeeShop, false, {})
}
