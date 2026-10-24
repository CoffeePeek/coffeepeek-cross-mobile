package com.coffeepeek.admin.ui.screen.profile

import androidx.compose.runtime.Composable
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.profile_cancel
import coffeepeek.composeapp.generated.resources.profile_logout_message
import coffeepeek.composeapp.generated.resources.profile_logout_title
import coffeepeek.composeapp.generated.resources.profile_menu_logout
import com.coffeepeek.admin.ui.component.NativeAlert
import com.coffeepeek.core.designsystem.component.CoffeePeekDestructiveConfirmationSheet
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ProfileLogoutConfirmation(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val title = stringResource(Res.string.profile_logout_title)
    val message = stringResource(Res.string.profile_logout_message)
    val confirmLabel = stringResource(Res.string.profile_menu_logout)
    val cancelLabel = stringResource(Res.string.profile_cancel)
    if (!NativeAlert(title, message, confirmLabel, onConfirm, onDismiss, cancelLabel, destructive = true)) {
        CoffeePeekDestructiveConfirmationSheet(title, message, confirmLabel,
            cancelLabel, onConfirm, onDismiss)
    }
}

@Preview @Composable private fun ProfileLogoutConfirmationLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ProfileLogoutConfirmation({}, {})
}

@Preview @Composable private fun ProfileLogoutConfirmationDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ProfileLogoutConfirmation({}, {})
}
