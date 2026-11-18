package com.coffeepeek.admin.di.media

import androidx.compose.runtime.Composable
import com.coffeepeek.core.designsystem.component.CoffeePeekPhotoViewerImages

@Composable
internal expect fun CheckInPhotoViewer(images: CoffeePeekPhotoViewerImages, initialIndex: Int, onDismiss: () -> Unit)
