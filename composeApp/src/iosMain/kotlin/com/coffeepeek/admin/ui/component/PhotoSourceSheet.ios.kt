@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.coffeepeek.admin.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.uikit.LocalUIViewController
import androidx.compose.ui.unit.dp
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.photo_source_camera
import coffeepeek.composeapp.generated.resources.photo_source_gallery
import coffeepeek.composeapp.generated.resources.presentation_cancel
import com.coffeepeek.admin.theme.toUIColor
import com.coffeepeek.admin.theme.uiKitStyle
import com.coffeepeek.core.designsystem.component.CoffeePeekActionPanel
import com.coffeepeek.core.designsystem.component.CoffeePeekDialogAction
import org.jetbrains.compose.resources.stringResource
import platform.UIKit.UIAccessibilityIsReduceMotionEnabled
import platform.UIKit.UIAlertAction
import platform.UIKit.UIAlertActionStyleCancel
import platform.UIKit.UIAlertActionStyleDefault
import platform.UIKit.UIAlertController
import platform.UIKit.UIAlertControllerStyleActionSheet
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.popoverPresentationController

@Composable
internal actual fun NativePhotoSourceSheet(
    title: String, onGallery: () -> Unit, onCamera: () -> Unit, onDismiss: () -> Unit,
): Boolean {
    val dismiss = rememberUpdatedState(onDismiss)
    val selection = remember { PlatformActionSelection(true) { dismiss.value() } }
    val cancel = stringResource(Res.string.presentation_cancel)
    val actions = listOf(
        PlatformAction(stringResource(Res.string.photo_source_gallery), onClick = onGallery),
        PlatformAction(stringResource(Res.string.photo_source_camera), onClick = onCamera,
            enabled = UIImagePickerController.isSourceTypeAvailable(
                UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera)),
    )
    if (NativeModalSheet(onDismiss, dismissEnabled = true) {
        val controller = LocalUIViewController.current
        CoffeePeekActionPanel(title, null, actions.map { action ->
            CoffeePeekDialogAction(action.title, {
                selection.select(action) { completed ->
                    controller.dismissViewControllerAnimated(!UIAccessibilityIsReduceMotionEnabled(), completed)
                }
            }, enabled = action.enabled)
        }, cancel, onDismiss, Modifier.fillMaxWidth().padding(16.dp))
    }) return true
    LegacyPhotoSourceSheet(title, actions, cancel, selection, onDismiss)
    return true
}

/** iOS 15 has no content-sized native sheet; its standard action sheet opens at the bottom. */
@Composable
private fun LegacyPhotoSourceSheet(
    title: String, actions: List<PlatformAction>, cancel: String,
    selection: PlatformActionSelection, onDismiss: () -> Unit,
) {
    val host = LocalUIViewController.current
    val latestActions = rememberUpdatedState(actions)
    val dismiss = rememberUpdatedState(onDismiss)
    val colors = MaterialTheme.colorScheme
    val alert = remember(host, title) {
        val alert = UIAlertController.alertControllerWithTitle(title, null, UIAlertControllerStyleActionSheet)
        actions.forEachIndexed { index, action ->
            alert.addAction(UIAlertAction.actionWithTitle(action.title, UIAlertActionStyleDefault) {
                selection.select(latestActions.value[index]) { completed ->
                    alert.dismissViewControllerAnimated(!UIAccessibilityIsReduceMotionEnabled(), completed)
                }
            }.apply { enabled = action.enabled })
        }
        alert.addAction(UIAlertAction.actionWithTitle(cancel, UIAlertActionStyleCancel) { dismiss.value() })
        alert.popoverPresentationController?.apply {
            sourceView = host.view
            sourceRect = host.view.bounds
            permittedArrowDirections = 0u
        }
        alert
    }
    SideEffect {
        alert.overrideUserInterfaceStyle = colors.uiKitStyle
        alert.view.tintColor = colors.primary.toUIColor()
    }
    PresentNativeController(alert, host)
}
