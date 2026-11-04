package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Android counterpart of UIKit's standard grouped track and inset selection. */
@Composable
internal fun CoffeePeekNativeSegments(labels: List<String>, selectedIndex: Int, onSelected: (Int) -> Unit,
    modifier: Modifier, enabled: Boolean, minHeight: Dp) {
    val dark = MaterialTheme.colorScheme.background.luminance() < .5f
    val track = Color(0xFF767680).copy(alpha = if (dark) .24f else .12f)
    Row(modifier.fillMaxWidth().alpha(if (enabled) 1f else .45f).clip(RoundedCornerShape(8.dp))
        .background(track).padding(2.dp).selectableGroup()) {
        labels.forEachIndexed { index, label ->
            CoffeePeekNativeSegment(label, index == selectedIndex, dark, enabled, { onSelected(index) },
                Modifier.weight(1f), (minHeight - 4.dp).coerceAtLeast(0.dp))
        }
    }
}

@Composable
private fun CoffeePeekNativeSegment(label: String, selected: Boolean, dark: Boolean, enabled: Boolean,
    onClick: () -> Unit, modifier: Modifier, minHeight: Dp) {
    val shape = RoundedCornerShape(6.dp)
    val thumb = if (dark) Color(0xFF636366) else Color.White
    Box(modifier.heightIn(min = minHeight).then(if (selected) Modifier.shadow(1.dp, shape) else Modifier)
        .clip(shape).background(if (selected) thumb else Color.Transparent)
        .selectable(selected = selected, enabled = enabled, role = Role.Tab, onClick = onClick).padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center) {
        Text(label, style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.SansSerif,
            fontSize = 13.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal),
            color = if (dark) Color.White else Color.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@PreviewLightDark
@Composable
private fun CoffeePeekNativeSegmentsPreview() = CoffeePeekTheme {
    var selected by remember { mutableIntStateOf(0) }
    CoffeePeekNativeSegments(listOf("Feed", "Leaders"), selected, { selected = it },
        Modifier.padding(16.dp), true, 48.dp)
}
