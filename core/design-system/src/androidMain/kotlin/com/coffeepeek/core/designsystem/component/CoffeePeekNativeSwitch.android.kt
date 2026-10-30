package com.coffeepeek.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@Composable
internal actual fun CoffeePeekNativeSwitch(
    checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier, enabled: Boolean,
): Boolean {
    val colors = MaterialTheme.colorScheme
    val palette = coffeePeekSwitchPalette(colors.background.luminance() < 0.5f)
    val track by animateColorAsState(if (checked) palette.checkedTrack else colors.onSurface.copy(alpha = 0.16f),
        tween(180), label = "glass-switch-track")
    val thumbOffset by animateDpAsState(if (checked) 20.dp else 0.dp, tween(180), label = "glass-switch-thumb")
    Box(modifier.sizeIn(minWidth = 51.dp, minHeight = 48.dp).alpha(if (enabled) 1f else 0.45f)
        .then(onCheckedChange?.let { Modifier.toggleable(checked, enabled = enabled, role = Role.Switch, onValueChange = it) }
            ?: Modifier), contentAlignment = Alignment.Center) {
        Box(Modifier.size(51.dp, 31.dp).background(track, CircleShape)
            .border(0.5.dp, colors.onSurface.copy(alpha = 0.05f), CircleShape).padding(2.dp)) {
            Box(Modifier.offset(x = thumbOffset).size(27.dp)
                .shadow(2.dp, CircleShape).background(Color.White, CircleShape))
        }
    }
    return true
}
