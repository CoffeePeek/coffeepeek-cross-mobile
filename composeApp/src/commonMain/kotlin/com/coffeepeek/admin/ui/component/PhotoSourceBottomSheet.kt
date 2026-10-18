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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.icons.CpIcons

@Composable
fun PhotoSourceBottomSheet(
    onDismiss: () -> Unit,
    onGallery: () -> Unit,
    onCamera: () -> Unit,
    title: String = "Добавить фото",
) {
    if (NativeActionSheet(title, listOf(
        PlatformAction("Выбрать из галереи", onClick = onGallery),
        PlatformAction("Сделать фото", onClick = onCamera),
    ), onDismiss = onDismiss)) return
    SwipeDismissModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(
                horizontal = CpDimens.spacing4,
                vertical = CpDimens.spacing2,
            ),
        )
        Column(
            modifier = Modifier
                .padding(horizontal = CpDimens.spacing4)
                .clip(RoundedCornerShape(CpDimens.radius2xl))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            PhotoSourceRow(CpIcons.Gallery, "Выбрать из галереи", onGallery)
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(start = 56.dp),
            )
            PhotoSourceRow(CpIcons.Camera, "Сделать фото", onCamera)
        }
        TextButton(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(CpDimens.spacing2),
        ) {
            Text("Отмена", style = MaterialTheme.typography.labelLarge)
        }
    }
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
