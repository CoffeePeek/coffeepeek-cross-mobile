package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

/** The caller owns the decision and closes the sheet before performing the action. */
@Composable
fun CoffeePeekDestructiveConfirmationSheet(
    title: String,
    message: String,
    confirmLabel: String,
    cancelLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    CoffeePeekSwipeDismissModalBottomSheet(onDismiss, cancelLabel) {
        DestructiveConfirmationContent(title, message, confirmLabel, cancelLabel, onConfirm, onDismiss)
    }
}

@Composable
private fun DestructiveConfirmationContent(
    title: String, message: String, confirmLabel: String, cancelLabel: String,
    onConfirm: () -> Unit, onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(CoffeePeekDimens.spacing4),
        verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
        Text(title, Modifier.fillMaxWidth().semantics { heading() },
            style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(message, Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
        MaterialTheme(colorScheme = colors.copy(primary = colors.error, onPrimary = colors.onError)) {
            CoffeePeekButton(confirmLabel, onConfirm)
        }
        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(cancelLabel, color = colors.onSurface)
        }
    }
}

@Preview @Composable private fun CoffeePeekDestructiveConfirmationLightPreview() = CoffeePeekTheme(darkTheme = false) {
    Surface { DestructiveConfirmationContent("Confirm action?", "You can cancel and stay here.", "Confirm", "Cancel", {}, {}) }
}

@Preview @Composable private fun CoffeePeekDestructiveConfirmationDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    Surface { DestructiveConfirmationContent("Confirm action?", "You can cancel and stay here.", "Confirm", "Cancel", {}, {}) }
}
