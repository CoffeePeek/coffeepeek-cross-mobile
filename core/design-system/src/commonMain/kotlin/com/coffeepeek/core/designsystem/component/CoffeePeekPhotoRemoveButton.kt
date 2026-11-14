package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekColors
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Keep the complete target inside the photo; the visible badge can remain compact. */
@Composable
fun CoffeePeekPhotoRemoveButton(
    description: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
) {
    CoffeePeekIconButton(onClick, modifier.size(CoffeePeekDimens.minimumTouchTarget), enabled, glass = false) {
        CoffeePeekIcon(CoffeePeekIcons.Close, description,
            Modifier.size(24.dp).alpha(if (enabled) 1f else 0.45f)
                .background(CoffeePeekColors.PhotoBadgeOverlay, CircleShape)
                .border(1.5.dp, Color.White, CircleShape).padding(4.dp), tint = Color.White)
    }
}

@Composable
private fun CoffeePeekPhotoRemoveButtonPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Row(Modifier.background(Color.White)) {
        CoffeePeekPhotoRemoveButton("Удалить фото", {})
        CoffeePeekPhotoRemoveButton("Удалить фото", {}, enabled = false)
    }
}

@Preview @Composable private fun CoffeePeekPhotoRemoveButtonLightPreview() = CoffeePeekPhotoRemoveButtonPreview(false)
@Preview @Composable private fun CoffeePeekPhotoRemoveButtonDarkPreview() = CoffeePeekPhotoRemoveButtonPreview(true)
