package com.coffeepeek.admin.sandbox.compose

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.coffeepeek.R
import com.coffeepeek.admin.ui.component.PhotoViewerImages
import com.coffeepeek.admin.sandbox.backend.SandboxBackend
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme

@Composable internal fun SandboxPhotoViewer(backend: SandboxBackend, images: PhotoViewerImages, initialIndex: Int, onClose: () -> Unit) {
    var index by remember(images) { mutableIntStateOf(initialIndex.coerceIn(0, images.previews.lastIndex.coerceAtLeast(0))) }
    Dialog(onClose) {
        SandboxPhotoViewerContent(images.displayUrl(index, index, true)?.let(backend::photo)
            ?: images.highQualityUrl(index, index)?.let(backend::photo), index, images.previews.size,
            { index-- }, { index++ }, onClose)
    }
}
@Composable private fun SandboxPhotoViewerContent(bytes: ByteArray?, index: Int, count: Int,
    onPrevious: () -> Unit, onNext: () -> Unit, onClose: () -> Unit) {
    Surface { Column(Modifier.fillMaxWidth().padding(16.dp)) {
        SandboxPhoto(bytes, Modifier.fillMaxWidth().aspectRatio(1f))
        Row {
            TextButton(onPrevious, enabled = index > 0) { Text(stringResource(R.string.sandbox_previous)) }
            TextButton(onNext, enabled = index + 1 < count) { Text(stringResource(R.string.sandbox_next)) }
            TextButton(onClose) { Text(stringResource(R.string.sandbox_close)) }
        }
    } }
}
@PreviewLightDark @Composable private fun SandboxPhotoViewerPreview() = CoffeePeekTheme {
    SandboxPhotoViewerContent(null, 0, 1, {}, {}, {})
}
