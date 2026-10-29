package com.coffeepeek.feature.homecheckin.impl.ui.compose.component

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.homecheckin.impl.resources.*

@Composable
internal fun HomeCheckInDeleteConfirmation(onConfirm: () -> Unit, onCancel: () -> Unit) {
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

@Composable private fun HomeCheckInDeleteConfirmationPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    HomeCheckInDeleteConfirmation({}, {})
}
@Preview @Composable private fun HomeCheckInDeleteConfirmationLightPreview() = HomeCheckInDeleteConfirmationPreview(false)
@Preview @Composable private fun HomeCheckInDeleteConfirmationDarkPreview() = HomeCheckInDeleteConfirmationPreview(true)
