package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

data class CoffeePeekActivityDay(
    val label: String, val description: String, val isToday: Boolean, val active: Boolean, val photoCount: Int,
)

data class CoffeePeekActivityWeekLabels(val open: String, val loading: String, val error: String, val retry: String)

/** Date arithmetic, private/public counts and image loading belong to the caller. */
@Composable
fun CoffeePeekActivityWeek(
    days: List<CoffeePeekActivityDay>, labels: CoffeePeekActivityWeekLabels, loading: Boolean, failed: Boolean,
    onClick: () -> Unit, onRetry: () -> Unit, modifier: Modifier = Modifier,
    image: @Composable (dayIndex: Int, modifier: Modifier) -> Unit,
) {
    require(days.size == 7)
    CoffeePeekCard(modifier.semantics { contentDescription = labels.open }, onClick = onClick,
        elevation = 0.dp, shape = RoundedCornerShape(20.dp),
        border = BorderStroke(0.7.dp, MaterialTheme.colorScheme.outlineVariant),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            days.forEachIndexed { index, day ->
                CoffeePeekActivityDayCell(day, Modifier.weight(1f)) { image(index, it) }
            }
        }
        if (loading) Text(labels.loading, Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        else if (failed) Row(verticalAlignment = Alignment.CenterVertically) {
            Text(labels.error, Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
            TextButton(onClick = onRetry) { Text(labels.retry) }
        }
    }
}

@Composable
private fun CoffeePeekActivityDayCell(day: CoffeePeekActivityDay, modifier: Modifier, image: @Composable (Modifier) -> Unit) {
    Column(modifier.semantics(mergeDescendants = true) { contentDescription = day.description },
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(day.label, style = MaterialTheme.typography.labelSmall,
            color = if (day.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
            if (day.photoCount > 0) {
                image(Modifier.fillMaxSize().clearAndSetSemantics {})
                if (day.photoCount > 1) Text("+${day.photoCount - 1}",
                    Modifier.align(Alignment.BottomEnd).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)).padding(horizontal = 3.dp),
                    style = MaterialTheme.typography.labelSmall)
            } else Box(Modifier.size(8.dp).background(if (day.active) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f), CircleShape))
        }
    }
}

@Composable
private fun CoffeePeekActivityWeekPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    CoffeePeekActivityWeek(List(7) { CoffeePeekActivityDay("${it + 1}", "Day ${it + 1}", it == 3, it in 1..4, 0) },
        CoffeePeekActivityWeekLabels("Open", "Loading", "Unavailable", "Retry"), false, false, {}, {}) { _, _ -> }
}

@Preview @Composable private fun CoffeePeekActivityWeekLightPreview() = CoffeePeekActivityWeekPreview(false)
@Preview @Composable private fun CoffeePeekActivityWeekDarkPreview() = CoffeePeekActivityWeekPreview(true)
