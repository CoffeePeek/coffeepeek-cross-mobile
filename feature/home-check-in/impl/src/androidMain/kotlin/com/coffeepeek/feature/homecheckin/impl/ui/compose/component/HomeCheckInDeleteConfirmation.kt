package com.coffeepeek.feature.homecheckin.impl.ui.compose.component

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.homecheckin.impl.R

@Composable
internal fun HomeCheckInDeleteConfirmation(onConfirm: () -> Unit, onCancel: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.home_check_in_delete_question)) },
        text = { Text(stringResource(R.string.home_check_in_delete_warning)) },
        confirmButton = {
            TextButton(onClick = onConfirm, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                Text(stringResource(R.string.home_check_in_delete_confirm))
            }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text(stringResource(R.string.home_check_in_cancel)) } },
    )
}

@PreviewLightDark @Composable private fun HomeCheckInDeleteConfirmationPreview() = CoffeePeekTheme {
    HomeCheckInDeleteConfirmation({}, {})
}
