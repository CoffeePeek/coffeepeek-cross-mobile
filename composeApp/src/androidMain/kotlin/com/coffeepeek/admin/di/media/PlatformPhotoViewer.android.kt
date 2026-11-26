package com.coffeepeek.admin.di.media

import androidx.compose.runtime.Composable
import com.coffeepeek.core.designsystem.component.CoffeePeekPhotoViewer
import com.coffeepeek.core.designsystem.component.CoffeePeekPhotoViewerImages

@Composable
internal actual fun PlatformPhotoViewer(images: CoffeePeekPhotoViewerImages, initialIndex: Int, onDismiss: () -> Unit) {
    CoffeePeekPhotoViewer(images, initialIndex, onDismiss)
}
