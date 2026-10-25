package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun CoffeePeekPhotoAttachment(
    index: Int, count: Int, enabled: Boolean, drag: CoffeePeekPhotoDragState,
    onOpen: () -> Unit, onMove: (Int, Int) -> Unit, onRemove: () -> Unit,
    photo: @Composable (Modifier) -> Unit,
) {
    val description = stringResource(Res.string.coffeepeek_photo_description, index + 1, count)
    val actions = photoMoveActions(index, count, enabled, onMove)
    val shape = RoundedCornerShape(CoffeePeekDimens.radiusMd)
    Box(Modifier.size(96.dp).coffeePeekPhotoDrag(index, enabled, drag, onMove)) {
        Box(Modifier.fillMaxSize().clip(shape)
            .border(if (drag.target == index) 3.dp else 1.dp,
                if (drag.target == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onOpen)
            .semantics {
                contentDescription = description
                customActions = actions
            }) {
            photo(Modifier.fillMaxSize())
            Text("${index + 1}", color = Color.White, style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.align(Alignment.BottomStart).padding(4.dp)
                    .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp))
        }
        PhotoRemoveButton(index, enabled, onRemove, Modifier.align(Alignment.TopEnd))
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

@Composable
private fun PhotoRemoveButton(index: Int, enabled: Boolean, onRemove: () -> Unit, modifier: Modifier) {
    IconButton(onClick = onRemove, enabled = enabled, modifier = modifier.offset(x = 16.dp, y = (-16).dp).size(48.dp)) {
        Icon(CoffeePeekIcons.Close, stringResource(Res.string.coffeepeek_photo_remove, index + 1),
            Modifier.size(24.dp).background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(50))
                .border(1.5.dp, Color.White, RoundedCornerShape(50)).padding(4.dp), tint = Color.White)
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
