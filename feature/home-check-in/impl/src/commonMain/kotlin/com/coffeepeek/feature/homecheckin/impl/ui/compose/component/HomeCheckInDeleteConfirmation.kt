package com.coffeepeek.feature.homecheckin.impl.ui.compose.component

import androidx.compose.material3.*
import com.coffeepeek.core.designsystem.component.CoffeePeekTextButton as TextButton
import androidx.compose.runtime.Composable
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.core.designsystem.component.CoffeePeekActionDialog
import com.coffeepeek.core.designsystem.component.CoffeePeekDialogAction
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.homecheckin.impl.resources.*

@Composable
internal fun HomeCheckInDeleteConfirmation(onConfirm: () -> Unit, onCancel: () -> Unit) {
    CoffeePeekActionDialog(
        title = stringResource(Res.string.home_check_in_delete_question),
        message = stringResource(Res.string.home_check_in_delete_warning),
        actions = listOf(CoffeePeekDialogAction(stringResource(Res.string.home_check_in_delete_confirm), onConfirm, destructive = true)),
        cancelLabel = stringResource(Res.string.home_check_in_cancel),
        onDismiss = onCancel,
    )
}

@Composable private fun HomeCheckInDeleteConfirmationPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    HomeCheckInDeleteConfirmation({}, {})
}
@Preview @Composable private fun HomeCheckInDeleteConfirmationLightPreview() = HomeCheckInDeleteConfirmationPreview(false)
@Preview @Composable private fun HomeCheckInDeleteConfirmationDarkPreview() = HomeCheckInDeleteConfirmationPreview(true)
