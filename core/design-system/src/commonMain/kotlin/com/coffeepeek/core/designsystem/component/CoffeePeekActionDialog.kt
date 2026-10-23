package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Full available width on phones, bounded on larger windows; the host owns safe insets. */
@Composable
fun CoffeePeekActionDialog(
    title: String,
    message: String?,
    actions: List<CoffeePeekDialogAction>,
    cancelLabel: String?,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        CoffeePeekActionPanel(title, message, actions, cancelLabel, onDismiss,
            Modifier.padding(CoffeePeekDimens.spacing4).widthIn(max = 560.dp))
    }
}

// Dialog rendering requires Interactive Preview or Run Preview in some IDE versions.
@Composable
private fun CoffeePeekActionDialogPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    CoffeePeekActionDialog("Block this author?",
        "You will stop seeing each other's posts. Existing subscriptions will be removed.",
        listOf(CoffeePeekDialogAction("Block", {}, destructive = true)), "Cancel", {})
}

@Preview @Composable private fun CoffeePeekActionDialogLightPreview() = CoffeePeekActionDialogPreview(false)
@Preview @Composable private fun CoffeePeekActionDialogDarkPreview() = CoffeePeekActionDialogPreview(true)
