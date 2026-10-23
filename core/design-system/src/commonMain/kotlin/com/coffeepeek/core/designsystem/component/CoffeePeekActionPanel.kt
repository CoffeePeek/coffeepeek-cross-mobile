package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

data class CoffeePeekDialogAction(
    val label: String,
    val onClick: () -> Unit,
    val destructive: Boolean = false,
    val enabled: Boolean = true,
)

@Composable
fun CoffeePeekActionPanel(
    title: String,
    message: String?,
    actions: List<CoffeePeekDialogAction>,
    cancelLabel: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CoffeePeekContentCard(modifier) {
        Column(Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
            Text(title, Modifier.fillMaxWidth().semantics { heading() },
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            if (!message.isNullOrBlank()) Text(message, style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            actions.forEach { CoffeePeekDialogActionButton(it) }
            if (cancelLabel != null) CoffeePeekDialogActionButton(CoffeePeekDialogAction(cancelLabel, onDismiss))
        }
    }
}

@Composable
private fun CoffeePeekDialogActionButton(action: CoffeePeekDialogAction) {
    val colors = MaterialTheme.colorScheme
    TextButton(
        onClick = action.onClick,
        enabled = action.enabled,
        modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
        shape = RoundedCornerShape(CoffeePeekDimens.buttonRadius),
        colors = ButtonDefaults.textButtonColors(containerColor = colors.surfaceVariant,
            contentColor = if (action.destructive) colors.error else colors.onSurface),
    ) {
        Text(action.label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun CoffeePeekActionPanelPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    CoffeePeekActionPanel("Choose a photo", null,
        listOf(CoffeePeekDialogAction("Photo library", {}), CoffeePeekDialogAction("Camera", {})),
        "Cancel", {}, Modifier.padding(16.dp))
}

@Preview @Composable private fun CoffeePeekActionPanelLightPreview() = CoffeePeekActionPanelPreview(false)
@Preview @Composable private fun CoffeePeekActionPanelDarkPreview() = CoffeePeekActionPanelPreview(true)
