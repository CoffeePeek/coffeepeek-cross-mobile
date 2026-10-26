package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * A pill of exclusive options. By default the options share the width equally (short labels). With [scrollable] every option takes the width of its label
 * (plus a horizontal padding) and the row scrolls sideways, so long labels or a large font never get squeezed; the pill then keeps a 4 dp inner padding.
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
    selectedContainerColor: Color = MaterialTheme.colorScheme.surface,
    selectedContentColor: Color = MaterialTheme.colorScheme.onSurface,
    minSegmentHeight: Dp = 48.dp,
) {
    require(options.isNotEmpty()) { "Segmented control requires options" }
    require(options.distinct().size == options.size) { "Options must be unique" }
    require(selected in options) { "Selection must belong to options" }
    val container = modifier
        .clip(RoundedCornerShape(percent = 50))
        .background(MaterialTheme.colorScheme.surfaceVariant)
    if (scrollable) {
        LazyRow(
            modifier = container.selectableGroup(),
            contentPadding = PaddingValues(4.dp),
        ) {
            items(options) { option ->
                Segment(label(option), option == selected, enabled, { onSelected(option) }, Modifier, selectedContainerColor, selectedContentColor, minSegmentHeight, labelPadding = 16.dp)
            }
        }
    } else {
        Row(modifier = container.fillMaxWidth().padding(2.dp).selectableGroup()) {
            options.forEach { option ->
                Segment(label(option), option == selected, enabled, { onSelected(option) }, Modifier.weight(1f), selectedContainerColor, selectedContentColor, minSegmentHeight)
            }
        }
    }
}

@Composable
private fun Segment(text: String, isSelected: Boolean, enabled: Boolean, onClick: () -> Unit, modifier: Modifier, selectedContainerColor: Color, selectedContentColor: Color, minHeight: Dp, labelPadding: Dp = 0.dp) {
    Box(
        modifier = modifier
            .heightIn(min = minHeight)
            .clip(RoundedCornerShape(percent = 50))
            .background(if (isSelected) selectedContainerColor else Color.Transparent)
            .then(
                if (isSelected) Modifier.border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(percent = 50),
                ) else Modifier,
            )
            .selectable(
                selected = isSelected,
                role = Role.Tab,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = labelPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) selectedContentColor else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CoffeePeekCapsuleSegmentedControlPreviewContent(darkTheme: Boolean, compact: Boolean = false) = CoffeePeekTheme(darkTheme = darkTheme) {
    var selected by remember { mutableStateOf("First") }
    Surface {
        CoffeePeekCapsuleSegmentedControl(listOf("First", "Second"), selected, { it }, { selected = it },
            modifier = Modifier.padding(16.dp), minSegmentHeight = if (compact) 44.dp else 48.dp)
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
@Preview @Composable private fun CoffeePeekCompactSegmentedControlLightPreview() = CoffeePeekCapsuleSegmentedControlPreviewContent(false, true)
@Preview @Composable private fun CoffeePeekCompactSegmentedControlDarkPreview() = CoffeePeekCapsuleSegmentedControlPreviewContent(true, true)
