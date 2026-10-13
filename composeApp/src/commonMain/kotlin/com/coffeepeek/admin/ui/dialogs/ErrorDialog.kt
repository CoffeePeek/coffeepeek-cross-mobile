package com.coffeepeek.admin.ui.dialogs

import androidx.compose.runtime.Composable

object ErrorDialog {

    @Composable
    operator fun invoke(
        show: Boolean,
        message: String,
        title: String = "Что-то пошло не так",
        onDismiss: () -> Unit
    ) {
        if (show) {
            com.coffeepeek.admin.ui.component.PlatformAlert(title, message, onConfirm = onDismiss, onDismiss = onDismiss)
        }
    }
}
