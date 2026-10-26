package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Timeline geometry, including the gap between items. The feature chooses the marker's emphasis. */
@Composable
fun CoffeePeekTimelineRail(accent: Boolean, firstInDay: Boolean, lastInDay: Boolean, content: @Composable () -> Unit) {
    val line = MaterialTheme.colorScheme.outlineVariant
    val dot = if (accent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    val background = MaterialTheme.colorScheme.background
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Box(Modifier.fillMaxWidth()) {
        Canvas(Modifier.matchParentSize()) {
            val x = if (rtl) 8.dp.toPx() else size.width - 8.dp.toPx()
            val dotY = 27.dp.toPx()
            drawLine(line, Offset(x, if (firstInDay) dotY else 0f), Offset(x, if (lastInDay) dotY else size.height), 2.dp.toPx())
            drawCircle(background, 7.dp.toPx(), Offset(x, dotY))
            drawCircle(dot, 4.dp.toPx(), Offset(x, dotY))
        }
        Box(Modifier.fillMaxWidth().padding(end = 28.dp, bottom = if (lastInDay) 0.dp else 12.dp)) { content() }
    }
}

@Composable private fun TimelineRailPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Column {
        repeat(3) { index -> CoffeePeekTimelineRail(index != 1, index == 0, index == 2) { Text("CoffeePeek", Modifier.padding(18.dp)) } }
    }
}
@Preview @Composable private fun TimelineRailLightPreview() = TimelineRailPreview(false)
@Preview @Composable private fun TimelineRailDarkPreview() = TimelineRailPreview(true)
