package com.coffeepeek.feature.homecheckin.impl.ui.compose.component

import androidx.compose.foundation.background
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.feature.homecheckin.impl.resources.Res
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_add_photos
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_camera
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_cancel
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_gallery
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekSwipeDismissModalBottomSheet
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme

@Composable
internal fun HomeCheckInPhotoSource(onDismiss: () -> Unit, onGallery: () -> Unit, onCamera: () -> Unit) {
    CoffeePeekSwipeDismissModalBottomSheet(onDismiss, stringResource(Res.string.home_check_in_cancel)) {
        HomeCheckInPhotoSourceContent(onDismiss, onGallery, onCamera)
    }
}

@Composable
private fun HomeCheckInPhotoSourceContent(onDismiss: () -> Unit, onGallery: () -> Unit, onCamera: () -> Unit) {
    Text(stringResource(Res.string.home_check_in_add_photos), style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
    Column(Modifier.padding(horizontal = 16.dp).clip(RoundedCornerShape(24.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant)) {
        SourceRow(CoffeePeekIcons.Gallery, stringResource(Res.string.home_check_in_gallery), onGallery)
        HorizontalDivider(Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outlineVariant)
        SourceRow(CoffeePeekIcons.Camera, stringResource(Res.string.home_check_in_camera), onCamera)
    }
    TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(8.dp)) {
        Text(stringResource(Res.string.home_check_in_cancel), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun SourceRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick).heightIn(min = 56.dp)
        .padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Preview @Composable private fun HomeCheckInPhotoSourcePreview() = CoffeePeekTheme {
    Surface { Column { HomeCheckInPhotoSourceContent({}, {}, {}) } }
}
