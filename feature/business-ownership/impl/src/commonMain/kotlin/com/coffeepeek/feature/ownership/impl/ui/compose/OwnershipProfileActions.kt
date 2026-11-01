package com.coffeepeek.feature.ownership.impl.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.modifier.CoffeePeekGlassIconButton
import com.coffeepeek.core.designsystem.modifier.CoffeePeekGlassIcon
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.ownership.impl.resources.*
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun OwnershipProfileActions(owner: Boolean, suggest: () -> Unit, edit: () -> Unit,
    favorite: @Composable () -> Unit, share: @Composable () -> Unit,
    roaster: Boolean = false, claim: @Composable (dismiss: () -> Unit) -> Unit) {
    var applying by remember { mutableStateOf(false) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (owner) CoffeePeekGlassIconButton(edit, stringResource(Res.string.ownership_edit),
            nativeIcon = CoffeePeekGlassIcon.Edit) {
            Icon(CoffeePeekIcons.NoteEdit, null)
        }
        favorite()
        share()
        if (!owner) OwnershipProfileMenu(
            editLabel = if (roaster) null else stringResource(Res.string.ownership_edit),
            representativeLabel = stringResource(if (roaster) Res.string.ownership_representative_roaster else Res.string.ownership_representative),
            actionsLabel = stringResource(Res.string.ownership_actions),
            onEdit = suggest,
            onRepresentative = { applying = true },
        )
    }
    if (applying) claim { applying = false }
}
@Composable private fun OwnershipProfileActionsPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Column {
        OwnershipProfileActions(false, {}, {}, {}, {}, claim = {})
        OwnershipProfileActions(true, {}, {}, {}, {}, claim = {})
    }
}
@Preview @Composable private fun OwnershipProfileActionsLightPreview() = OwnershipProfileActionsPreview(false)
@Preview @Composable private fun OwnershipProfileActionsDarkPreview() = OwnershipProfileActionsPreview(true)
