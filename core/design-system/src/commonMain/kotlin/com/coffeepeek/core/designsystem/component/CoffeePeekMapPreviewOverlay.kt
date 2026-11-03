package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.resources.Res
import com.coffeepeek.core.designsystem.resources.coffeepeek_map_expand
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Place above the native canvas so preview gestures stay in Compose. */
@Composable
fun CoffeePeekMapPreviewOverlay(onExpand: () -> Unit, modifier: Modifier = Modifier) {
    val description = stringResource(Res.string.coffeepeek_map_expand)
    Box(modifier.fillMaxSize()
        .clickable(role = Role.Button, onClickLabel = description, onClick = onExpand)
        .semantics { contentDescription = description }) {
        Box(Modifier.align(Alignment.TopEnd).padding(CoffeePeekDimens.spacing2)
            .size(CoffeePeekDimens.buttonHeight),
            contentAlignment = Alignment.Center) {
            CoffeePeekNativeControlBackground(Modifier.matchParentSize(), hazeState = null)
            Icon(CoffeePeekIcons.Expand, contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun CoffeePeekMapPreviewOverlayPreview(darkTheme: Boolean) = CoffeePeekTheme(darkTheme = darkTheme) {
    Box(Modifier.fillMaxWidth().height(180.dp).background(MaterialTheme.colorScheme.surfaceVariant)) {
        CoffeePeekMapPreviewOverlay({}, Modifier.matchParentSize())
    }
}

@Preview @Composable private fun CoffeePeekMapPreviewOverlayLightPreview() = CoffeePeekMapPreviewOverlayPreview(false)
@Preview @Composable private fun CoffeePeekMapPreviewOverlayDarkPreview() = CoffeePeekMapPreviewOverlayPreview(true)
