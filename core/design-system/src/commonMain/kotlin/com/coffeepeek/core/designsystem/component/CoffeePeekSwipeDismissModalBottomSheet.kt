package com.coffeepeek.core.designsystem.component

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * Use [maxSheetHeightFraction] to bound the handle and content within the available
 * modal content height. Do not put fractional height on [modifier]: Material's
 * anchors must see the full dialog viewport to keep the sheet bottom-aligned.
 * [contentWindowInsets] are applied/consumed by Material once, including horizontal
 * cutouts. Material owns IME avoidance; content must not add raw system-bar padding.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoffeePeekSwipeDismissModalBottomSheet(
    onDismissRequest: () -> Unit,
    dismissDescription: String,
    modifier: Modifier = Modifier,
    dismissEnabled: Boolean = true,
    maxSheetHeightFraction: Float? = null,
    contentWindowInsets: @Composable () -> WindowInsets = { WindowInsets.safeDrawing },
    content: @Composable ColumnScope.() -> Unit,
) {
    require(maxSheetHeightFraction == null || maxSheetHeightFraction > 0f && maxSheetHeightFraction <= 1f) {
        "maxSheetHeightFraction must be in (0, 1]"
    }
    val native = LocalCoffeePeekNativeSheetPresenter.current
    if (native != null && native.Present(onDismissRequest, dismissEnabled, maxSheetHeightFraction, content)) return
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
        // Material wraps a custom dragHandle in an unconditional clickable. Own the
        // handle in content so locked sheets expose neither a click nor a dismiss action.
        dragHandle = null,
        properties = ModalBottomSheetProperties(shouldDismissOnClickOutside = dismissEnabled),
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = contentWindowInsets,
    ) {
        // Fractional constraints belong below Material's anchor layout, not above it.
        Column(Modifier.fillMaxWidth().then(
            maxSheetHeightFraction?.let { Modifier.fillMaxHeight(it) } ?: Modifier,
        )) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .semantics { contentDescription = dismissDescription }
                    .then(if (dismissEnabled) {
                        Modifier.clickable(role = Role.Button, onClickLabel = dismissDescription) {
                            scope.launch {
                                if (currentDismissEnabled) {
                                    sheetState.hide()
                                    if (!sheetState.isVisible && currentDismissEnabled) currentOnDismiss()
                                }
                            }
                        }
                    } else {
                        Modifier.semantics { disabled(); role = Role.Button }
                    })
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
            content()
        }
    }
}

// Modal rendering requires Interactive Preview or Run Preview in some IDE versions.
@Composable
private fun CoffeePeekSwipeDismissModalBottomSheetPreviewContent(darkTheme: Boolean) = CoffeePeekTheme(darkTheme = darkTheme) {
    var shown by remember { mutableStateOf(true) }
    if (shown) CoffeePeekSwipeDismissModalBottomSheet({ shown = false }, "Dismiss sheet") {
        Text("Example sheet: drag the handle to dismiss", Modifier.padding(24.dp))
    }
}

@Preview @Composable private fun CoffeePeekSwipeDismissModalBottomSheetLightPreview() = CoffeePeekSwipeDismissModalBottomSheetPreviewContent(false)
@Preview @Composable private fun CoffeePeekSwipeDismissModalBottomSheetDarkPreview() = CoffeePeekSwipeDismissModalBottomSheetPreviewContent(true)

@Composable
private fun CoffeePeekSwipeDismissModalBottomSheetBoundedPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    CoffeePeekSwipeDismissModalBottomSheet({}, "Dismiss sheet", maxSheetHeightFraction = 0.76f) {
        Column(Modifier.fillMaxWidth().fillMaxHeight().verticalScroll(rememberScrollState())) {
            repeat(40) { Text("Example row $it", Modifier.padding(8.dp)) }
        }
    }
}

@Preview @Composable private fun CoffeePeekSwipeDismissModalBottomSheetBoundedLightPreview() = CoffeePeekSwipeDismissModalBottomSheetBoundedPreview(false)
@Preview @Composable private fun CoffeePeekSwipeDismissModalBottomSheetBoundedDarkPreview() = CoffeePeekSwipeDismissModalBottomSheetBoundedPreview(true)

@Preview @Composable private fun CoffeePeekSwipeDismissModalBottomSheetLockedLightPreview() = CoffeePeekTheme(darkTheme = false) {
    CoffeePeekSwipeDismissModalBottomSheet({}, "Dismiss sheet", dismissEnabled = false) {
        Text("Submission in progress", Modifier.padding(24.dp))
    }
}

@Preview @Composable private fun CoffeePeekSwipeDismissModalBottomSheetLockedDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    CoffeePeekSwipeDismissModalBottomSheet({}, "Dismiss sheet", dismissEnabled = false) {
        Text("Submission in progress", Modifier.padding(24.dp))
    }
}
