package com.coffeepeek.admin.sandbox.compose

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.coffeepeek.R
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import kotlin.math.roundToInt

/** Movable debug handle: never permanently covers a real screen's filter/close control. */
@Composable internal fun SandboxMenuButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier) {
        val density = LocalDensity.current
        val horizontalLimit = with(density) { (maxWidth - 64.dp).coerceAtLeast(0.dp).toPx() }
        val verticalLimit = with(density) { (maxHeight / 2 - 80.dp).coerceAtLeast(0.dp).toPx() }
        var position by remember { mutableStateOf(Offset.Zero) }
        Surface(Modifier.align(Alignment.CenterEnd).padding(end = 4.dp)
            .offset { IntOffset(position.x.roundToInt(), position.y.roundToInt()) }
            .pointerInput(horizontalLimit, verticalLimit) {
                detectDragGestures { change, amount ->
                    change.consume()
                    position = Offset((position.x + amount.x).coerceIn(-horizontalLimit, 0f),
                        (position.y + amount.y).coerceIn(-verticalLimit, verticalLimit))
                }
            }, color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.small) {
            TextButton(onClick, contentPadding = PaddingValues(6.dp)) {
                Text(stringResource(R.string.sandbox_short_banner), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
@PreviewLightDark @Composable private fun SandboxMenuButtonPreview() = CoffeePeekTheme {
    Surface { SandboxMenuButton({}, Modifier.size(320.dp, 480.dp)) }
}
