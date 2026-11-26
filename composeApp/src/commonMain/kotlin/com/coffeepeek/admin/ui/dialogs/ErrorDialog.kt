package com.coffeepeek.admin.ui.dialogs

import androidx.compose.foundation.layout.heightIn

import com.coffeepeek.core.designsystem.icons.CoffeePeekIllustrations
import coffeepeek.composeapp.generated.resources.*
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import com.coffeepeek.core.designsystem.component.CoffeePeekFilledButton as Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import coffeepeek.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.painterResource

object ErrorDialog {
    @Composable
    operator fun invoke(show: Boolean, message: String,
        title: String = stringResource(Res.string.error_dialog_unexpected_error), onDismiss: () -> Unit) {
        if (!show) return
        val close = stringResource(Res.string.error_dialog_shelf_camera_tips_close)
        if (PlatformGlobalError(title, message, close, onDismiss)) return
        Dialog(onDismissRequest = onDismiss) { ErrorDialogContent(title, message, close, onDismiss) }
    }
}

@Composable
private fun ErrorDialogContent(title: String, message: String, close: String, onDismiss: () -> Unit) {
    Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            ErrorDialogMessage(title, message)
            Spacer(Modifier.height(24.dp))
            ErrorDialogCloseButton(close, onDismiss)
        }
    }
}

@Composable
private fun ErrorDialogMessage(title: String, message: String) {
    Image(painterResource(CoffeePeekIllustrations.LoadError), contentDescription = null,
        contentScale = ContentScale.Fit, modifier = Modifier.size(96.dp))
    Spacer(Modifier.height(16.dp))
    Text(title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp),
        textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface)
    Spacer(Modifier.height(8.dp))
    Text(message, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ErrorDialogCloseButton(label: String, onDismiss: () -> Unit) {
    Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().heightIn(min = CoffeePeekDimens.buttonHeight),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary), shape = RoundedCornerShape(percent = 50)) {
        Text(label, style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp),
            modifier = Modifier.padding(vertical = 4.dp))
    }
}
