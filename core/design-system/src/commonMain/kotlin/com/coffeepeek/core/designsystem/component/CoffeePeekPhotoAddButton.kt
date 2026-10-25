package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.resources.Res
import com.coffeepeek.core.designsystem.resources.coffeepeek_photo_add
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun CoffeePeekPhotoAddButton(enabled: Boolean, isLoading: Boolean, onAdd: () -> Unit) {
    val description = stringResource(Res.string.coffeepeek_photo_add)
    val shape = RoundedCornerShape(CoffeePeekDimens.radiusMd)
    Box(Modifier.size(96.dp).clip(shape).background(MaterialTheme.colorScheme.surface)
        .border(1.dp, MaterialTheme.colorScheme.outline, shape)
        .clickable(enabled = enabled, role = Role.Button, onClick = onAdd), contentAlignment = Alignment.Center) {
        if (isLoading) CoffeePeekLoader(description, size = 20.dp)
        else Icon(CoffeePeekIcons.Camera, description, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Preview @Composable private fun CoffeePeekPhotoAddButtonLightPreview() = CoffeePeekTheme(darkTheme = false) {
    CoffeePeekPhotoAddButton(true, false, {})
}
@Preview @Composable private fun CoffeePeekPhotoAddButtonDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    CoffeePeekPhotoAddButton(false, true, {})
}
