package com.coffeepeek.admin.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.ui.Modifier

/** Platform presentation only; callbacks continue to belong to the calling feature. */
internal data class PlatformAction(
    val title: String,
    val destructive: Boolean = false,
    val enabled: Boolean = true,
    val onClick: () -> Unit,
)

@Composable
internal expect fun PlatformAlert(
    title: String,
    message: String,
    confirmLabel: String = "Понятно",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    cancelLabel: String? = null,
    destructive: Boolean = false,
    enabled: Boolean = true,
)

@Composable
internal expect fun NativeActionSheet(title: String, actions: List<PlatformAction>, message: String? = null, dismissBeforeAction: Boolean = true, onDismiss: () -> Unit): Boolean

@Composable
internal expect fun NativePhotoSourceSheet(title: String, onGallery: () -> Unit, onCamera: () -> Unit, onDismiss: () -> Unit): Boolean

/** Returns false on platforms that render CoffeePeek's own confirmation/error surface. */
@Composable
internal expect fun NativeAlert(
    title: String, message: String, confirmLabel: String, onConfirm: () -> Unit,
    onDismiss: () -> Unit, cancelLabel: String? = null, destructive: Boolean = false,
): Boolean

@Composable
internal expect fun NativeOperationStatus(message: String): Boolean

/** Read only by the native iOS sheet; Android keeps its existing Material height policy. */
internal val LocalNativeSheetHeightFraction = staticCompositionLocalOf<Float?> { null }

@Composable
internal expect fun NativeModalSheet(
    onDismiss: () -> Unit,
    dismissEnabled: Boolean,
    content: @Composable ColumnScope.() -> Unit,
): Boolean

@Composable
internal expect fun PlatformSwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, enabled: Boolean = true, modifier: Modifier = Modifier)

@Composable
internal expect fun NativeSegmentedControl(labels: List<String>, selectedIndex: Int, onSelected: (Int) -> Unit, modifier: Modifier, enabled: Boolean = true): Boolean

internal enum class HapticEvent { Selection, Success, Error }
internal expect fun platformHaptic(event: HapticEvent)

@Composable
internal expect fun platformReduceMotion(): Boolean

@Composable
internal expect fun NativeDatePicker(
    title: String,
    initialMillis: Long,
    maximumMillis: Long?,
    timeOnly: Boolean,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit,
): Boolean
