package com.coffeepeek.admin.di.media

import androidx.compose.runtime.Composable
import com.coffeepeek.core.designsystem.component.CoffeePeekPhotoViewerImages

/** Ordinary galleries share one platform viewer; pickers, crops and generated posters are separate flows. */
@Composable
internal expect fun PlatformPhotoViewer(images: CoffeePeekPhotoViewerImages, initialIndex: Int, onDismiss: () -> Unit)
