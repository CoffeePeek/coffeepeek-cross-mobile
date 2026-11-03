package com.coffeepeek.admin.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.icons.CpIcons
import dev.chrisbanes.haze.HazeState

@Composable
actual fun PlatformGlassIconButton(
    icon: GlassControlIcon,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier,
    enabled: Boolean,
    selected: Boolean,
    isLoading: Boolean,
    hazeState: HazeState?,
    content: @Composable () -> Unit,
) {
    GlassIconButton(
        onClick = onClick,
        contentDescription = contentDescription,
        modifier = modifier,
        enabled = enabled,
        hazeState = hazeState,
        content = content,
    )
}

@Composable
actual fun PlatformFloatingBottomNavBar(
    items: List<FloatingNavItem>,
    modifier: Modifier,
    hazeState: HazeState?,
) {
    FloatingBottomNavBar(items = items, modifier = modifier, hazeState = hazeState)
}

@Composable
actual fun PlatformGlassBackButton(onClick: () -> Unit, modifier: Modifier) {
    com.coffeepeek.core.designsystem.component.CoffeePeekTonalButton(onClick, modifier) {
        Icon(CpIcons.ChevronLeft, contentDescription = null, modifier = Modifier.size(18.dp))
        Text("Назад", Modifier.padding(start = 4.dp))
    }
}

@Composable
actual fun PlatformMapControlButton(
    icon: GlassControlIcon,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier,
    selected: Boolean,
    content: @Composable () -> Unit,
) {
    com.coffeepeek.core.designsystem.component.CoffeePeekIconButton(
        onClick = onClick,
        modifier = modifier.semantics { this.contentDescription = contentDescription },
        content = content,
    )
}

@androidx.compose.ui.tooling.preview.PreviewLightDark
@Composable
private fun PlatformGlassControlsPreview() = com.coffeepeek.core.designsystem.theme.CoffeePeekTheme {
    Row {
            PlatformGlassBackButton({})
            PlatformMapControlButton(GlassControlIcon.Location, {}, "Location") { Icon(CpIcons.Location, null) }
        }
}
