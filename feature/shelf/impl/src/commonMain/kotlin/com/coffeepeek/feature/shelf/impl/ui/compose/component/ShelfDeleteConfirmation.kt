package com.coffeepeek.feature.shelf.impl.ui.compose.component

import androidx.compose.runtime.Composable
import com.coffeepeek.core.designsystem.component.CoffeePeekDestructiveConfirmationSheet
import com.coffeepeek.feature.shelf.impl.resources.*
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

/** Deletion is only for a bag added by mistake; the message points to "Закончилась" instead. */
@Composable
internal fun ShelfDeleteConfirmation(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    CoffeePeekDestructiveConfirmationSheet(
        title = stringResource(Res.string.shelf_delete_title),
        message = stringResource(Res.string.shelf_delete_message),
        confirmLabel = stringResource(Res.string.shelf_bag_delete),
        cancelLabel = stringResource(Res.string.shelf_cancel),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}
