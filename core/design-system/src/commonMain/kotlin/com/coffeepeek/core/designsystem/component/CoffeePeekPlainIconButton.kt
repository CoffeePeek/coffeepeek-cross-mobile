package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Inline content action: no visible plate, with the same accessible target as glass controls. */
@Composable
fun CoffeePeekPlainIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Box(modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
        .coffeePeekGlassPress(pressed, enabled)
        .clickable(interactions, indication = null, enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center) {
        CompositionLocalProvider(LocalContentColor provides color.copy(alpha = if (enabled) 1f else 0.38f)) { content() }
    }
}

@Composable
private fun PlainIconPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    CoffeePeekPlainIconButton({}) { Icon(CoffeePeekIcons.MoreHorizontal, "Actions", Modifier.size(24.dp)) }
}

@Preview @Composable private fun PlainIconLightPreview() = PlainIconPreview(false)
@Preview @Composable private fun PlainIconDarkPreview() = PlainIconPreview(true)
