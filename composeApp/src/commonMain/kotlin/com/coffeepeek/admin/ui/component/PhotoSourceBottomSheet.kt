package com.coffeepeek.admin.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.coffeepeek.core.designsystem.component.CoffeePeekTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.photo_source_camera
import coffeepeek.composeapp.generated.resources.photo_source_gallery
import coffeepeek.composeapp.generated.resources.photo_source_title
import coffeepeek.composeapp.generated.resources.presentation_cancel
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.icons.CpIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun PhotoSourceBottomSheet(
    onDismiss: () -> Unit,
    onGallery: () -> Unit,
    onCamera: () -> Unit,
    title: String = stringResource(Res.string.photo_source_title),
) {
    if (NativePhotoSourceSheet(title, onGallery, onCamera, onDismiss)) return
    SwipeDismissModalBottomSheet(onDismissRequest = onDismiss) {
        PhotoSourceContent(title, onGallery, onCamera, onDismiss)
    }
}

@Composable
private fun PhotoSourceContent(title: String, onGallery: () -> Unit, onCamera: () -> Unit, onDismiss: () -> Unit) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(
                horizontal = CpDimens.spacing4,
                vertical = CpDimens.spacing2,
            ),
        )
        PhotoSourceRows(onGallery, onCamera)
        TextButton(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(CpDimens.spacing2),
        ) {
            Text(stringResource(Res.string.presentation_cancel), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun PhotoSourceRows(onGallery: () -> Unit, onCamera: () -> Unit) {
    Column(
        modifier = Modifier
            .padding(horizontal = CpDimens.spacing4)
            .clip(RoundedCornerShape(CpDimens.radius2xl))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        PhotoSourceRow(CpIcons.Gallery, stringResource(Res.string.photo_source_gallery), onGallery)
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.padding(start = 56.dp),
        )
        PhotoSourceRow(CpIcons.Camera, stringResource(Res.string.photo_source_camera), onCamera)
    }
}

@Preview @Composable private fun PhotoSourceLightPreview() = CoffeePeekTheme(darkTheme = false) {
    PhotoSourceContent("Photo", {}, {}, {})
}

@Preview @Composable private fun PhotoSourceDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    PhotoSourceContent("Photo", {}, {}, {})
}

@Composable
private fun PhotoSourceRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(onClickLabel = title, onClick = onClick)
            .padding(horizontal = CpDimens.spacing4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing3),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
