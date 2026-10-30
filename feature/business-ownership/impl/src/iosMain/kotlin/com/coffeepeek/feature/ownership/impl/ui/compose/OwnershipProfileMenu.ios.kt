package com.coffeepeek.feature.ownership.impl.ui.compose

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coffeepeek.core.designsystem.component.CoffeePeekNativeActionMenu
import com.coffeepeek.core.designsystem.component.CoffeePeekNativeMenuAction
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens

@Composable
internal actual fun OwnershipProfileMenu(
    editLabel: String?, representativeLabel: String, actionsLabel: String,
    onEdit: () -> Unit, onRepresentative: () -> Unit,
) {
    CoffeePeekNativeActionMenu(
        actions = buildList {
            if (editLabel != null) add(CoffeePeekNativeMenuAction(
                id = "edit", label = editLabel, systemSymbolName = "square.and.pencil", onClick = onEdit,
            ))
            add(CoffeePeekNativeMenuAction(
                id = "representative", label = representativeLabel,
                systemSymbolName = "person.crop.circle", onClick = onRepresentative,
            ))
        },
        contentDescription = actionsLabel,
        modifier = Modifier.size(CoffeePeekDimens.buttonHeight),
    )
}
