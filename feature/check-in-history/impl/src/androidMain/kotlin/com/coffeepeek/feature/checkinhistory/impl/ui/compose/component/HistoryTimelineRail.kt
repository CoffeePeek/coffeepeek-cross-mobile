package com.coffeepeek.feature.checkinhistory.impl.ui.compose.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme

@Composable
internal fun HistoryTimelineRail(isHome: Boolean, firstInDay: Boolean, lastInDay: Boolean, content: @Composable () -> Unit) {
    val line = MaterialTheme.colorScheme.outlineVariant
    val dot = if (isHome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    val background = MaterialTheme.colorScheme.background
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Box(Modifier.fillMaxWidth()) {
        Canvas(Modifier.matchParentSize()) {
            val x = if (rtl) 8.dp.toPx() else size.width - 8.dp.toPx()
            val dotY = 27.dp.toPx()
            drawLine(line, Offset(x, if (firstInDay) dotY else 0f),
                Offset(x, if (lastInDay) dotY else size.height), 2.dp.toPx())
            drawCircle(background, 7.dp.toPx(), Offset(x, dotY))
            drawCircle(dot, 4.dp.toPx(), Offset(x, dotY))
        }
        Box(Modifier.fillMaxWidth().padding(end = 28.dp, bottom = if (lastInDay) 0.dp else 12.dp)) { content() }
    }
}

@PreviewLightDark @Composable private fun HistoryTimelineRailPreview() = CoffeePeekTheme {
    HistoryTimelineRail(true, true, false) { Text("Hario V60", Modifier.padding(18.dp)) }
}
