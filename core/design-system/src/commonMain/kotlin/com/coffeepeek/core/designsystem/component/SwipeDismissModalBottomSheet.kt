package com.coffeepeek.core.designsystem.component

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeDismissModalBottomSheet(
    onDismissRequest: () -> Unit,
    dismissDescription: String,
    modifier: Modifier = Modifier,
    dismissEnabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scope = rememberCoroutineScope()
    val currentDismissEnabled by rememberUpdatedState(dismissEnabled)
    val currentOnDismiss by rememberUpdatedState(onDismissRequest)
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden || currentDismissEnabled },
    )
    val density = LocalDensity.current
    val dismissDistance = with(density) { 72.dp.toPx() }
    val dismissVelocity = with(density) { 900.dp.toPx() }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(dismissEnabled) { if (!dismissEnabled) dragOffset = 0f }
    val dragState = rememberDraggableState { delta ->
        dragOffset = (dragOffset + delta).coerceAtLeast(0f)
    }

    ModalBottomSheet(
        onDismissRequest = { if (currentDismissEnabled) currentOnDismiss() },
        modifier = modifier.graphicsLayer { translationY = if (dismissEnabled) dragOffset else 0f },
        sheetState = sheetState,
        sheetGesturesEnabled = false,
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .semantics {
                        contentDescription = dismissDescription
                        if (dismissEnabled) onClick(label = dismissDescription) {
                            scope.launch {
                                sheetState.hide()
                                if (!sheetState.isVisible && currentDismissEnabled) currentOnDismiss()
                            }
                            true
                        }
                    }
                    .draggable(
                        state = dragState,
                        orientation = Orientation.Vertical,
                        enabled = dismissEnabled,
                        onDragStopped = { velocity ->
                            if (currentDismissEnabled && (dragOffset >= dismissDistance || velocity >= dismissVelocity)) {
                                sheetState.hide()
                                if (!sheetState.isVisible && currentDismissEnabled) currentOnDismiss()
                            } else {
                                animate(
                                    initialValue = dragOffset,
                                    targetValue = 0f,
                                    animationSpec = tween(durationMillis = 160),
                                ) { value, _ -> dragOffset = value }
                            }
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 36.dp, height = 4.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)),
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        content = content,
    )
}

// Modal rendering requires Interactive Preview or Run Preview in some IDE versions.
@Composable
private fun SwipeDismissModalBottomSheetPreviewContent(darkTheme: Boolean) = CoffeePeekTheme(darkTheme = darkTheme) {
    var shown by remember { mutableStateOf(true) }
    if (shown) SwipeDismissModalBottomSheet({ shown = false }, "Dismiss sheet") {
        Text("Example sheet: drag the handle to dismiss", Modifier.padding(24.dp))
    }
}

@Preview @Composable private fun SwipeDismissModalBottomSheetLightPreview() = SwipeDismissModalBottomSheetPreviewContent(false)
@Preview @Composable private fun SwipeDismissModalBottomSheetDarkPreview() = SwipeDismissModalBottomSheetPreviewContent(true)

@Preview @Composable private fun SwipeDismissModalBottomSheetLockedLightPreview() = CoffeePeekTheme(darkTheme = false) {
    SwipeDismissModalBottomSheet({}, "Dismiss sheet", dismissEnabled = false) {
        Text("Submission in progress", Modifier.padding(24.dp))
    }
}

@Preview @Composable private fun SwipeDismissModalBottomSheetLockedDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    SwipeDismissModalBottomSheet({}, "Dismiss sheet", dismissEnabled = false) {
        Text("Submission in progress", Modifier.padding(24.dp))
    }
}
