package com.coffeepeek.feature.ownership.impl.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.*
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.modifier.CoffeePeekGlassIconButton
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.ownership.impl.resources.*
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun OwnershipProfileActions(owner: Boolean, suggest: () -> Unit, edit: () -> Unit,
    favorite: @Composable () -> Unit, share: @Composable () -> Unit,
    roaster: Boolean = false, claim: @Composable (dismiss: () -> Unit) -> Unit) {
    var menu by remember { mutableStateOf(false) }
    var applying by remember { mutableStateOf(false) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (owner) CoffeePeekGlassIconButton(edit, stringResource(Res.string.ownership_edit)) {
            Icon(CoffeePeekIcons.NoteEdit, null)
        }
        favorite()
        share()
        if (!owner) Box {
            CoffeePeekGlassIconButton({ menu = true }, stringResource(Res.string.ownership_actions)) {
                Icon(CoffeePeekIcons.MoreHorizontal, null)
            }
            DropdownMenu(menu, { menu = false }) {
                if (!roaster) DropdownMenuItem(text = { Text(stringResource(Res.string.ownership_edit)) },
                    onClick = { menu = false; suggest() })
                DropdownMenuItem(text = { Text(stringResource(if (roaster) Res.string.ownership_representative_roaster else Res.string.ownership_representative)) },
                    onClick = { menu = false; applying = true })
            }
        }
    }
    if (applying) claim { applying = false }
}
@Preview @Composable private fun OwnershipProfileActionsLightPreview() = CoffeePeekTheme(darkTheme = false) {
    OwnershipProfileActions(false, {}, {}, {}, {}, claim = {})
}
@Preview @Composable private fun OwnershipProfileActionsDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    OwnershipProfileActions(true, {}, {}, {}, {}, claim = {})
}
