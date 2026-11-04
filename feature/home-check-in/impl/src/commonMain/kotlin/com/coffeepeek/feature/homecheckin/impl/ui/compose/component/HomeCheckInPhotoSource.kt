package com.coffeepeek.feature.homecheckin.impl.ui.compose.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import com.coffeepeek.core.designsystem.component.CoffeePeekTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekSwipeDismissModalBottomSheet
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.homecheckin.impl.resources.*

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

@Composable private fun HomeCheckInPhotoSourcePreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Surface { Column { HomeCheckInPhotoSourceContent({}, {}, {}) } }
}
@Preview @Composable private fun HomeCheckInPhotoSourceLightPreview() = HomeCheckInPhotoSourcePreview(false)
@Preview @Composable private fun HomeCheckInPhotoSourceDarkPreview() = HomeCheckInPhotoSourcePreview(true)
