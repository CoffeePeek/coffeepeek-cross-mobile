package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.resources.*
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Presentation only. The caller supplies platform launchers and closes the sheet before launching them. */
@Composable
fun CoffeePeekPhotoSourceSheet(onDismiss: () -> Unit, onGallery: () -> Unit, onCamera: () -> Unit) {
    val title = stringResource(Res.string.coffeepeek_photo_source_title)
    if (CoffeePeekNativePhotoSource(title, onGallery, onCamera, onDismiss)) return
    CoffeePeekSwipeDismissModalBottomSheet(onDismissRequest = onDismiss, dismissDescription = stringResource(Res.string.coffeepeek_photo_source_cancel)) {
        PhotoSourceContent(title, onGallery, onCamera, onDismiss)
    }
}

@Composable
private fun PhotoSourceContent(title: String, onGallery: () -> Unit, onCamera: () -> Unit, onDismiss: () -> Unit) {
    Column {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = CoffeePeekDimens.spacing4, vertical = CoffeePeekDimens.spacing2))
        Column(Modifier.padding(horizontal = CoffeePeekDimens.spacing4).clip(RoundedCornerShape(CoffeePeekDimens.radius2xl))
            .background(MaterialTheme.colorScheme.surfaceVariant)) {
            PhotoSourceRow(CoffeePeekIcons.Gallery, stringResource(Res.string.coffeepeek_photo_source_gallery), onGallery)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(start = 56.dp))
            PhotoSourceRow(CoffeePeekIcons.Camera, stringResource(Res.string.coffeepeek_photo_source_camera), onCamera)
        }
        TextButton(onDismiss, Modifier.fillMaxWidth().navigationBarsPadding().padding(CoffeePeekDimens.spacing2)) {
            Text(stringResource(Res.string.coffeepeek_photo_source_cancel), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun PhotoSourceRow(icon: ImageVector, title: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(56.dp).clickable(role = Role.Button, onClickLabel = title, onClick = onClick)
        .padding(horizontal = CoffeePeekDimens.spacing4), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable private fun PhotoSourcePreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    PhotoSourceContent(stringResource(Res.string.coffeepeek_photo_source_title), {}, {}, {})
}
@Preview @Composable private fun CoffeePeekPhotoSourceLightPreview() = PhotoSourcePreview(false)
@Preview @Composable private fun CoffeePeekPhotoSourceDarkPreview() = PhotoSourcePreview(true)
