package com.coffeepeek.core.designsystem.component

import com.coffeepeek.core.designsystem.theme.CoffeePeekColors
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import com.coffeepeek.core.designsystem.component.CoffeePeekIconButton as IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.resources.*
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun CoffeePeekPhotoAttachment(
    index: Int, count: Int, enabled: Boolean, drag: CoffeePeekPhotoDragState,
    onOpen: () -> Unit, onMove: (Int, Int) -> Unit, onRemove: () -> Unit,
    manage: Boolean = true,
    photo: @Composable (Modifier) -> Unit,
) {
    val description = stringResource(Res.string.coffeepeek_photo_description, index + 1, count)
    val actions = photoMoveActions(index, count, enabled && manage, onMove)
    val shape = RoundedCornerShape(CoffeePeekDimens.radiusMd)
    Box(Modifier.size(96.dp).then(if (manage) Modifier.coffeePeekPhotoDrag(index, enabled, drag, onMove) else Modifier)) {
        Box(Modifier.fillMaxSize().clip(shape)
            .border(if (drag.target == index) 3.dp else 1.dp,
                if (drag.target == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, shape)
            .then(if (manage) Modifier.clickable(enabled = enabled, role = Role.Button, onClick = onOpen) else Modifier)
            .semantics {
                contentDescription = description
                customActions = actions
            }) {
            photo(Modifier.fillMaxSize())
            if (manage) Text("${index + 1}", color = Color.White, style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.align(Alignment.BottomStart).padding(4.dp)
                    .background(CoffeePeekColors.PhotoBadgeOverlay, RoundedCornerShape(4.dp)).padding(horizontal = 4.dp))
        }
        CoffeePeekPhotoRemoveButton(stringResource(Res.string.coffeepeek_photo_remove, index + 1),
            onRemove, Modifier.align(Alignment.TopEnd), enabled)
    }
}

@Composable
private fun Modifier.coffeePeekPhotoDrag(
    index: Int, enabled: Boolean, drag: CoffeePeekPhotoDragState, onMove: (Int, Int) -> Unit,
): Modifier {
    val haptic = LocalHapticFeedback.current
    val move by rememberUpdatedState(onMove)
    val dragging = drag.index == index
    return onGloballyPositioned { drag.bounds[index] = it.boundsInParent() }
        .zIndex(if (dragging) 1f else 0f)
        .graphicsLayer {
            translationX = if (dragging) drag.offset.x else 0f
            translationY = if (dragging) drag.offset.y else 0f
            shadowElevation = if (dragging) 8.dp.toPx() else 0f
        }
        .pointerInput(enabled, drag, index) {
            if (enabled) detectDragGesturesAfterLongPress(
                onDragStart = {
                    drag.start(index)
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                onDragEnd = { drag.finish(move) }, onDragCancel = drag::cancel,
                onDrag = { change, delta ->
                    change.consume()
                    drag.move(delta)
                },
            )
        }
}

@Composable
private fun photoMoveActions(index: Int, count: Int, enabled: Boolean, onMove: (Int, Int) -> Unit): List<CustomAccessibilityAction> {
    val earlier = stringResource(Res.string.coffeepeek_photo_move_earlier)
    val later = stringResource(Res.string.coffeepeek_photo_move_later)
    return if (!enabled) emptyList() else buildList {
        if (index > 0) add(CustomAccessibilityAction(earlier) {
            onMove(index, index - 1)
            true
        })
        if (index < count - 1) add(CustomAccessibilityAction(later) {
            onMove(index, index + 1)
            true
        })
    }
}

@Preview @Composable private fun CoffeePeekPhotoAttachmentLightPreview() = PhotoAttachmentPreview(false)
@Preview @Composable private fun CoffeePeekPhotoAttachmentDarkPreview() = PhotoAttachmentPreview(true)

@Composable
private fun PhotoAttachmentPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    CoffeePeekPhotoAttachment(0, 3, true, CoffeePeekPhotoDragState(), {}, { _, _ -> }, {}) {
        CoffeePeekImagePlaceholder(it)
    }
}
