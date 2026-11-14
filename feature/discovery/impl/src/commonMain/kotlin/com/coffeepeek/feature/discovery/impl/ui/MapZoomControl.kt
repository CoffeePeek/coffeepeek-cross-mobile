package com.coffeepeek.feature.discovery.impl.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekIcon
import com.coffeepeek.core.designsystem.component.coffeePeekCardBorder
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.discovery.impl.resources.Res
import com.coffeepeek.feature.discovery.impl.resources.discovery_map_zoom_in
import com.coffeepeek.feature.discovery.impl.resources.discovery_map_zoom_out
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun MapZoomControl(onZoomIn: () -> Unit, onZoomOut: () -> Unit, modifier: Modifier = Modifier) {
    val border = coffeePeekCardBorder()
    Surface(
        modifier = modifier.width(CoffeePeekDimens.minimumTouchTarget),
        shape = RoundedCornerShape(CoffeePeekDimens.radiusLg),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        border = border,
    ) {
        Column {
            ZoomButton(CoffeePeekIcons.Add, stringResource(Res.string.discovery_map_zoom_in), onZoomIn)
            HorizontalDivider(thickness = border.width, color = MaterialTheme.colorScheme.outlineVariant)
            ZoomButton(CoffeePeekIcons.Minus, stringResource(Res.string.discovery_map_zoom_out), onZoomOut)
        }
    }
}

@Composable
private fun ZoomButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().heightIn(min = CoffeePeekDimens.minimumTouchTarget)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        CoffeePeekIcon(icon, description, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurface)
    }
}

@Preview @Composable private fun MapZoomControlLightPreview() = CoffeePeekTheme(darkTheme = false) {
    MapZoomControl({}, {})
}

@Preview @Composable private fun MapZoomControlDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    MapZoomControl({}, {})
}
