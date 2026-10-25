package com.coffeepeek.feature.homecheckin.impl.ui.compose.component

import androidx.compose.material3.*
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.feature.homecheckin.impl.resources.Res
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_cancel
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_delete_confirm
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_delete_question
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_delete_warning
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.component.CoffeePeekNativeAlert

@Composable
internal fun HomeCheckInDeleteConfirmation(onConfirm: () -> Unit, onCancel: () -> Unit) {
    if (CoffeePeekNativeAlert(
        title = stringResource(Res.string.home_check_in_delete_question),
        message = stringResource(Res.string.home_check_in_delete_warning),
        confirmLabel = stringResource(Res.string.home_check_in_delete_confirm),
        onConfirm = onConfirm, onDismiss = onCancel,
        cancelLabel = stringResource(Res.string.home_check_in_cancel), destructive = true,
    )) return
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(Res.string.home_check_in_delete_question)) },
        text = { Text(stringResource(Res.string.home_check_in_delete_warning)) },
        confirmButton = {
            TextButton(onClick = onConfirm, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                Text(stringResource(Res.string.home_check_in_delete_confirm))
            }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text(stringResource(Res.string.home_check_in_cancel)) } },
    )
}

@Preview @Composable private fun HomeCheckInDeleteConfirmationLightPreview() = HomeCheckInDeleteConfirmationPreview(false)
@Preview @Composable private fun HomeCheckInDeleteConfirmationDarkPreview() = HomeCheckInDeleteConfirmationPreview(true)

@Composable private fun HomeCheckInDeleteConfirmationPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    HomeCheckInDeleteConfirmation({}, {})
}
