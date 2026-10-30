package com.coffeepeek.feature.ownership.impl.ui.compose

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import com.coffeepeek.core.designsystem.component.CoffeePeekActionMenu
import com.coffeepeek.core.designsystem.component.CoffeePeekMenuAction
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.modifier.CoffeePeekGlassIconButton

@Composable
internal actual fun OwnershipProfileMenu(
    editLabel: String?, representativeLabel: String, actionsLabel: String,
    onEdit: () -> Unit, onRepresentative: () -> Unit,
) {
    val actions = listOfNotNull(
        editLabel?.let { CoffeePeekMenuAction(it, CoffeePeekIcons.NoteEdit, onClick = onEdit) },
        CoffeePeekMenuAction(representativeLabel, CoffeePeekIcons.User, onClick = onRepresentative),
    )
    CoffeePeekActionMenu(actions) { open ->
        CoffeePeekGlassIconButton(open, actionsLabel) {
            Icon(CoffeePeekIcons.MoreHorizontal, null)
        }
    }
}
