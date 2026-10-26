package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekColors
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Receives an already formatted date and an image renderer; performs no loading or navigation. */
@Composable
fun CoffeePeekTimelineHeader(
    name: String, dateLabel: String, modifier: Modifier = Modifier,
    avatar: @Composable (Modifier) -> Unit,
    actions: @Composable () -> Unit = {},
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(CoffeePeekColors.GoldWarmSoft).clearAndSetSemantics {},
            contentAlignment = Alignment.Center) {
            Text(name.trim().firstOrNull()?.uppercase() ?: "?", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold, color = CoffeePeekColors.LightTextPrimary)
            avatar(Modifier.fillMaxSize())
        }
        Column(Modifier.weight(1f).padding(start = CoffeePeekDimens.spacing3), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (dateLabel.isNotBlank()) Text(dateLabel, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        actions()
    }
}

@Composable private fun TimelineHeaderPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    CoffeePeekTimelineHeader("Анна", "14 октября 2026", avatar = {})
}
@Preview @Composable private fun TimelineHeaderLightPreview() = TimelineHeaderPreview(false)
@Preview @Composable private fun TimelineHeaderDarkPreview() = TimelineHeaderPreview(true)
