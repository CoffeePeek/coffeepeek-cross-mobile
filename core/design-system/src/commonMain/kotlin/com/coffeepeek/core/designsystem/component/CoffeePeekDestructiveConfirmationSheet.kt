package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

/** The caller owns the decision and closes the sheet before performing the action. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoffeePeekDestructiveConfirmationSheet(
    title: String,
    message: String,
    confirmLabel: String,
    cancelLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        sheetMaxWidth = 560.dp,
        shape = RectangleShape,
        containerColor = Color.Transparent,
        tonalElevation = 0.dp,
        dragHandle = null,
        contentWindowInsets = { WindowInsets.safeDrawing },
    ) {
        DestructiveConfirmationContent(title, message, confirmLabel, cancelLabel, onConfirm, onDismiss)
    }
}

@Composable
private fun DestructiveConfirmationContent(
    title: String, message: String, confirmLabel: String, cancelLabel: String,
    onConfirm: () -> Unit, onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(CoffeePeekDimens.cardRadius)
    val border = BorderStroke(1.dp, colors.outlineVariant)
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(CoffeePeekDimens.spacing4),
        verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
        Surface(shape = shape, color = colors.surface, border = border) {
            Column {
                Column(Modifier.padding(CoffeePeekDimens.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
                    Text(title, Modifier.fillMaxWidth().semantics { heading() },
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center)
                    Text(message, Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
                }
                HorizontalDivider(color = colors.outlineVariant)
                ConfirmationTextAction(confirmLabel, onConfirm, colors.error)
            }
        }
        Surface(shape = shape, color = colors.surface, border = border) {
            ConfirmationTextAction(cancelLabel, onDismiss, colors.onSurface, FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ConfirmationTextAction(
    label: String, onClick: () -> Unit, color: Color, weight: FontWeight = FontWeight.Medium,
) {
    TextButton(onClick, Modifier.fillMaxWidth().heightIn(min = 54.dp),
        shape = RectangleShape, contentPadding = PaddingValues(CoffeePeekDimens.spacing3),
        colors = ButtonDefaults.textButtonColors(contentColor = color)) {
        Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = weight, textAlign = TextAlign.Center)
    }
}

@Preview @Composable private fun CoffeePeekDestructiveConfirmationLightPreview() = CoffeePeekTheme(darkTheme = false) {
    Surface { DestructiveConfirmationContent("Confirm action?", "You can cancel and stay here.", "Confirm", "Cancel", {}, {}) }
}

@Preview @Composable private fun CoffeePeekDestructiveConfirmationDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    Surface { DestructiveConfirmationContent("Confirm action?", "You can cancel and stay here.", "Confirm", "Cancel", {}, {}) }
}
