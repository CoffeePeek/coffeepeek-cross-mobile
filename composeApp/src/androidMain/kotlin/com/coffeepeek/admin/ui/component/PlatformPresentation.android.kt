package com.coffeepeek.admin.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.*
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
internal actual fun NativeActionSheet(title: String, actions: List<PlatformAction>, onDismiss: () -> Unit) = false
@Composable
internal actual fun NativeModalSheet(onDismiss: () -> Unit, dismissEnabled: Boolean, content: @Composable ColumnScope.() -> Unit) = false
@Composable
internal actual fun PlatformSwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, enabled: Boolean, modifier: Modifier) {
    val palette = switchPalette(MaterialTheme.colorScheme.background.luminance() < .5f)
    Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled, modifier = modifier,
        // A non-null slot keeps the thumb full-size in both states.
        thumbContent = { Box(Modifier.size(SwitchDefaults.IconSize)) },
        colors = SwitchDefaults.colors(
            checkedThumbColor = palette.checkedThumb, checkedTrackColor = palette.checkedTrack,
            uncheckedThumbColor = palette.uncheckedThumb, uncheckedTrackColor = palette.uncheckedTrack,
            checkedBorderColor = Color.Transparent, uncheckedBorderColor = Color.Transparent,
            disabledCheckedThumbColor = palette.checkedThumb,
            disabledCheckedTrackColor = palette.checkedTrack.copy(alpha = .5f),
            disabledUncheckedThumbColor = palette.uncheckedThumb,
            disabledUncheckedTrackColor = palette.uncheckedTrack.copy(alpha = .5f),
            disabledCheckedBorderColor = Color.Transparent, disabledUncheckedBorderColor = Color.Transparent,
        ))
}
@Composable
internal actual fun NativeSegmentedControl(labels: List<String>, selectedIndex: Int, onSelected: (Int) -> Unit, modifier: Modifier) = false
internal actual fun platformHaptic(event: HapticEvent) = Unit
@Composable
internal actual fun platformReduceMotion() = false
@Composable
internal actual fun NativeDatePicker(title: String, initialMillis: Long, maximumMillis: Long?, timeOnly: Boolean, onConfirm: (Long) -> Unit, onDismiss: () -> Unit) = false
