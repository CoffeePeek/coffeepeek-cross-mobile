package com.coffeepeek.feature.checkinhistory.impl.ui.compose.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.checkinhistory.impl.resources.Res
import com.coffeepeek.feature.checkinhistory.impl.resources.history_calendar_more_visits
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

/** The date node announces the exact total; these compact marks are decorative. */
@Composable
internal fun HistoryCalendarVisitMarkers(total: Long, onSelectedBackground: Boolean, modifier: Modifier = Modifier) {
    if (total <= 0) return
    val color = if (onSelectedBackground) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
    Row(modifier.clearAndSetSemantics {}, horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically) {
        repeat(total.coerceAtMost(3).toInt()) { Box(Modifier.size(3.dp).background(color, CircleShape)) }
        if (total > 3) Text(stringResource(Res.string.history_calendar_more_visits, total - 3),
            color = color, style = MaterialTheme.typography.labelSmall, fontSize = 8.sp, lineHeight = 10.sp, maxLines = 1)
    }
}

@Composable private fun HistoryCalendarVisitMarkersPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    HistoryCalendarVisitMarkers(5, onSelectedBackground = false)
}
@Preview @Composable private fun HistoryCalendarVisitMarkersLightPreview() = HistoryCalendarVisitMarkersPreview(false)
@Preview @Composable private fun HistoryCalendarVisitMarkersDarkPreview() = HistoryCalendarVisitMarkersPreview(true)

@Composable private fun HistoryCalendarVisitMarkersSelectedPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Box(Modifier.background(MaterialTheme.colorScheme.primary)) {
        HistoryCalendarVisitMarkers(5, onSelectedBackground = true)
    }
}
@Preview @Composable private fun HistoryCalendarVisitMarkersSelectedLightPreview() = HistoryCalendarVisitMarkersSelectedPreview(false)
@Preview @Composable private fun HistoryCalendarVisitMarkersSelectedDarkPreview() = HistoryCalendarVisitMarkersSelectedPreview(true)
