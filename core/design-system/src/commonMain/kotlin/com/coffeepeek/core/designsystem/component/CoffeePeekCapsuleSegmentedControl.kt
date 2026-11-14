package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * A pill of exclusive options. By default the options share the width equally (short labels). With [scrollable] every option takes the width of its label
 * (plus a horizontal padding) and the row scrolls sideways, so long labels or a large font never get squeezed; the pill then keeps a 4 dp inner padding.
 * Scrollable options and large text retain the Compose layout with full-height action targets.
 * [minSegmentHeight] includes the track's inner padding and defaults to the button height.
 * Compose segments can grow beyond that minimum to accommodate larger text.
 */
@Composable
fun <T> CoffeePeekCapsuleSegmentedControl(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    scrollable: Boolean = false,
    minSegmentHeight: Dp = CoffeePeekDimens.buttonHeight,
) {
    require(options.isNotEmpty()) { "Segmented control requires options" }
    require(options.distinct().size == options.size) { "Options must be unique" }
    require(selected in options) { "Selection must belong to options" }
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(modifier) {
        if (coffeePeekSegmentsNeedScroll(options.size, maxWidth, fontScale, scrollable)) {
            CoffeePeekScrollableSegments(options, selected, label, onSelected, Modifier, enabled, minSegmentHeight)
        } else {
            CoffeePeekPlatformSegments(options.map(label), options.indexOf(selected),
                { onSelected(options[it]) }, Modifier, enabled, minSegmentHeight)
        }
    }
}

internal fun coffeePeekSegmentsNeedScroll(count: Int, width: Dp, fontScale: Float, requested: Boolean): Boolean =
    requested || fontScale > 1.3f || width < CoffeePeekDimens.minimumTouchTarget * count + CoffeePeekDimens.spacing1 * 2

@Composable
private fun <T> CoffeePeekScrollableSegments(options: List<T>, selected: T, label: (T) -> String,
    onSelected: (T) -> Unit, modifier: Modifier, enabled: Boolean, minSegmentHeight: Dp) {
    val container = modifier
        .heightIn(min = minSegmentHeight)
        .clip(RoundedCornerShape(percent = 50))
        .background(MaterialTheme.colorScheme.surfaceVariant)
    LazyRow(
        modifier = container.selectableGroup(),
        contentPadding = PaddingValues(horizontal = CoffeePeekDimens.spacing1),
    ) {
        items(options) { option ->
            CoffeePeekCapsuleSegment(label(option), option == selected, enabled, { onSelected(option) },
                Modifier, labelPadding = 16.dp,
                minHeight = minSegmentHeight)
        }
    }
}

@Composable
internal fun CoffeePeekCapsuleSegment(text: String, isSelected: Boolean, enabled: Boolean, onClick: () -> Unit,
    modifier: Modifier, minHeight: Dp, labelPadding: Dp = 0.dp) {
    val shape = RoundedCornerShape(percent = 50)
    Box(modifier.sizeIn(minWidth = CoffeePeekDimens.minimumTouchTarget,
        minHeight = maxOf(minHeight, CoffeePeekDimens.minimumTouchTarget))
        .selectable(selected = isSelected, role = Role.Tab, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center) {
        Box(Modifier.matchParentSize().padding(vertical = CoffeePeekDimens.spacing1)
            .clip(shape).background(if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent)
            .then(if (isSelected) Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape) else Modifier))
        Text(text, Modifier.padding(horizontal = labelPadding, vertical = CoffeePeekDimens.spacing2),
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun CoffeePeekCapsuleSegmentedControlPreviewContent(darkTheme: Boolean, fontScale: Float = 1f) = CoffeePeekTheme(darkTheme = darkTheme) {
    var selected by remember { mutableStateOf("First") }
    CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale)) {
        Surface {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CoffeePeekCapsuleSegmentedControl(listOf("First", "Second"), selected, { it }, { selected = it }, Modifier.weight(1f))
                    CoffeePeekIconButton({}, Modifier.size(CoffeePeekDimens.buttonHeight)) {
                        CoffeePeekIcon(CoffeePeekIcons.Notifications, null)
                    }
                }
                CoffeePeekCapsuleSegmentedControl(listOf("First", "Second"), "Second", { it }, {})
                CoffeePeekCapsuleSegmentedControl(listOf("First", "Second", "Third"), "Second", { it }, {})
                CoffeePeekCapsuleSegmentedControl(listOf("First", "Second", "Third", "Fourth"), "Fourth", { it }, {}, enabled = false)
            }
        }
    }
}

@Composable
private fun CoffeePeekScrollableSegmentedControlPreviewContent(darkTheme: Boolean) = CoffeePeekTheme(darkTheme = darkTheme) {
    var selected by remember { mutableStateOf("Опубликовано · 5") }
    Surface {
        CoffeePeekCapsuleSegmentedControl(listOf("Опубликовано · 5", "На модерации · 1", "Отклонено · 1"), selected, { it }, { selected = it },
            modifier = Modifier.padding(16.dp), scrollable = true)
    }
}

@Preview @Composable private fun CoffeePeekScrollableSegmentedControlLightPreview() = CoffeePeekScrollableSegmentedControlPreviewContent(false)
@Preview @Composable private fun CoffeePeekScrollableSegmentedControlDarkPreview() = CoffeePeekScrollableSegmentedControlPreviewContent(true)
@Preview @Composable private fun CoffeePeekCapsuleSegmentedControlLightPreview() = CoffeePeekCapsuleSegmentedControlPreviewContent(false)
@Preview @Composable private fun CoffeePeekCapsuleSegmentedControlDarkPreview() = CoffeePeekCapsuleSegmentedControlPreviewContent(true)
@Preview @Composable private fun CoffeePeekLargeTextSegmentedControlLightPreview() = CoffeePeekCapsuleSegmentedControlPreviewContent(false, 2f)
@Preview @Composable private fun CoffeePeekLargeTextSegmentedControlDarkPreview() = CoffeePeekCapsuleSegmentedControlPreviewContent(true, 2f)
