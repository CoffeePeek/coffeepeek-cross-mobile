@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.admin.ui.component

import androidx.compose.runtime.Composable
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.photo_source_camera
import coffeepeek.composeapp.generated.resources.photo_source_gallery
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerSourceType

@Composable
internal actual fun NativePhotoSourceSheet(
    title: String, onGallery: () -> Unit, onCamera: () -> Unit, onDismiss: () -> Unit,
): Boolean = NativeActionSheet(
    title = title,
    actions = listOf(
        PlatformAction(stringResource(Res.string.photo_source_gallery), onClick = onGallery),
        PlatformAction(stringResource(Res.string.photo_source_camera), onClick = onCamera,
            enabled = UIImagePickerController.isSourceTypeAvailable(
                UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera)),
    ),
    onDismiss = onDismiss,
)
