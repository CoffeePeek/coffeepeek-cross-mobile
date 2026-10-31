package com.coffeepeek.admin.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.MotionDurationScale
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.*
import com.coffeepeek.core.designsystem.component.CoffeePeekTextButton as TextButton
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.theme.switchPalette

@Composable
internal actual fun PlatformAlert(title: String, message: String, confirmLabel: String, onConfirm: () -> Unit, onDismiss: () -> Unit, cancelLabel: String?, destructive: Boolean, enabled: Boolean) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm, enabled = enabled) { Text(confirmLabel, color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary) } },
        dismissButton = cancelLabel?.let { { TextButton(onClick = onDismiss) { Text(it) } } })
}
@Composable
internal actual fun NativeAlert(title: String, message: String, confirmLabel: String, onConfirm: () -> Unit, onDismiss: () -> Unit, cancelLabel: String?, destructive: Boolean) = false
@Composable
internal actual fun NativeOperationStatus(message: String) = false
@Composable
internal actual fun NativeActionSheet(title: String, actions: List<PlatformAction>, message: String?, dismissBeforeAction: Boolean, onDismiss: () -> Unit) = false
@Composable
internal actual fun NativePhotoSourceSheet(title: String, onGallery: () -> Unit, onCamera: () -> Unit, onDismiss: () -> Unit) = false
@Composable
internal actual fun NativeModalSheet(onDismiss: () -> Unit, dismissEnabled: Boolean, content: @Composable ColumnScope.() -> Unit) = false
@Composable
internal actual fun PlatformSwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, enabled: Boolean, modifier: Modifier) {
    com.coffeepeek.core.designsystem.component.CoffeePeekSwitch(checked, onCheckedChange, modifier, enabled)
}
@Composable
internal actual fun NativeSegmentedControl(labels: List<String>, selectedIndex: Int, onSelected: (Int) -> Unit, modifier: Modifier) = false
internal actual fun platformHaptic(event: HapticEvent) = AndroidHapticFeedback.perform(event)
@Composable
internal actual fun platformReduceMotion(): Boolean {
    // The window Recomposer already observes the system animator duration scale.
    val scale = rememberCoroutineScope().coroutineContext[MotionDurationScale]
    return scale?.scaleFactor == 0f
}
@Composable
internal actual fun NativeDatePicker(title: String, initialMillis: Long, maximumMillis: Long?, timeOnly: Boolean, onConfirm: (Long) -> Unit, onDismiss: () -> Unit) = false

@androidx.compose.ui.tooling.preview.PreviewLightDark
@Composable
private fun PlatformSwitchPreview() = com.coffeepeek.core.designsystem.theme.CoffeePeekTheme {
    PlatformSwitch(true, {})
}
